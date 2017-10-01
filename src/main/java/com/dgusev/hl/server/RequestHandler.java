package com.dgusev.hl.server;

import com.dgusev.hl.server.codecs.StringUTFCodec;
import com.dgusev.hl.server.exceptions.BadRequest;
import com.dgusev.hl.server.exceptions.EntityNotFound;
import com.dgusev.hl.server.model.Location;
import com.dgusev.hl.server.model.User;
import com.dgusev.hl.server.model.Visit;
import com.dgusev.hl.server.model.VisitResponse;
import com.dgusev.hl.server.parsers.JsonFormatters;
import com.dgusev.hl.server.parsers.LocationParser;
import com.dgusev.hl.server.parsers.UserParser;
import com.dgusev.hl.server.parsers.VisitParser;
import com.dgusev.hl.server.service.TravelService;
import com.dgusev.hl.server.stat.ActionType;
import com.dgusev.hl.server.stat.Statistics;
import com.dgusev.hl.server.threads.WorkerThread;
import io.netty.buffer.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.epoll.AbstractEpoll0Channel;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;

/**
 * Created by dgusev on 11.09.2017.
 */
@Component
@ChannelHandler.Sharable
public class RequestHandler extends ChannelInboundHandlerAdapter {

    public static final ByteBuf RESPONSE_404 = Unpooled.directBuffer().writeBytes(Unpooled.copiedBuffer("HTTP/1.1 404 \r\nConnection: keep-alive\r\nContent-Type: application/json;charset=UTF-8\r\nDate: Sat, 19 Aug 2017 11:30:12 GMT\r\nContent-Length: 0\r\n\r\n", CharsetUtil.UTF_8));
    public static final ByteBuf RESPONSE_400 = Unpooled.directBuffer().writeBytes(Unpooled.copiedBuffer("HTTP/1.1 400 \r\nConnection: keep-alive\r\nContent-Type: application/json;charset=utf-8\r\nDate: Sat, 19 Aug 2017 11:30:12 GMT\r\nContent-Length: 2\r\n\r\n{}", CharsetUtil.UTF_8));
    public static final ByteBuf RESPONSE_200 = Unpooled.directBuffer().writeBytes(Unpooled.copiedBuffer("HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=UTF-8\r\nDate: Sat, 19 Aug 2017 11:30:12 GMT\r\nContent-Length: 2\r\n\r\n{}", CharsetUtil.UTF_8));

    public static final ByteBuf EMPTY_VISITS = Unpooled.directBuffer().writeBytes(Unpooled.copiedBuffer("HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=utf-8\r\nContent-Length: 14\r\n\r\n{\"visits\": []}", CharsetUtil.UTF_8));
    public static final ByteBuf ZERO_MARK = Unpooled.directBuffer().writeBytes(Unpooled.copiedBuffer("HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=utf-8\r\nContent-Length: 11\r\n\r\n{\"avg\":0.0}", CharsetUtil.UTF_8));

    public static final byte[] VISIT_HEADER = ("HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=utf-8\r\nContent-Length:     ").getBytes();
    public static final byte[] DOUBLE_NL = "\r\n\r\n".getBytes();

    @Autowired
    private TravelService travelService;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        long t1 = System.nanoTime();
        ActionType actionType = null;
        long writeTime = 0;
        long preHandleTime = 0;
        try {
            Statistics.handlerCallCount.incrementAndGet();
            WorkerThread workerThread = (WorkerThread) Thread.currentThread();
            char[] BUFFER = workerThread.BUFFER;
            byte[] ARRAY_INPUT_CONTAINER = workerThread.ARRAY_INPUT_CONTAINER;
            byte[] ARRAY_OUTPUT_CONTAINER = workerThread.ARRAY_OUTPUT_CONTAINER;
            ByteBuf ENCODE_BUFFER = workerThread.ENCODE_BUFFER;
            ENCODE_BUFFER.retain();
            AttributeKey<ByteBuf> attributeKey = workerThread.ATTRIBUTE_KEY;
            List<VisitResponse> VISIT_RESPONSE = workerThread.VISIT_RESPONSE;
            ByteBuf fragmentedMessage = ctx.channel().attr(attributeKey).get();
            if (fragmentedMessage != null) {
                fragmentedMessage.writeBytes((ByteBuf) msg);
                msg = fragmentedMessage;
            }
            ByteBuf buf = (ByteBuf) msg;
            int count = StringUTFCodec.decode(buf, ARRAY_INPUT_CONTAINER, BUFFER);
            if ((BUFFER[0] == 'P' && ((BUFFER[count - 1] != '\n' || BUFFER[count - 2] != '\r' || BUFFER[count - 3] != '}') && (!new String(BUFFER, 0, count).contains("Content-Length: 0")))) || (BUFFER[0] == 'G' && (BUFFER[count - 1] != '\n' || BUFFER[count - 2] != '\r' || BUFFER[count - 3] != '\n' || BUFFER[count - 4] != '\r'))) {
                ByteBuf byteBuf = ctx.channel().attr(attributeKey).get();
                if (byteBuf == null) {
                    byteBuf = ctx.alloc().directBuffer(1000);
                    ctx.channel().attr(attributeKey).set(byteBuf);
                }
                buf.readerIndex(0);
                byteBuf.writeBytes(buf);
                //release(buf);
                Statistics.fragmentationCount.incrementAndGet();
                return;
            } else if (fragmentedMessage != null) {
                ctx.channel().attr(attributeKey).set(null);
                release(fragmentedMessage);
            }
            long t6 = System.nanoTime();
            Statistics.preHandleTime.addAndGet(t6-t1);
            preHandleTime = t6-t1;
            int pointer = 0;
            if (BUFFER[0] == 'G') {
                pointer += 4;
                int start = pointer;
                while (BUFFER[pointer] != ' ') {
                    pointer++;
                }
                String query = new String(BUFFER, 4, pointer - 4);
                if (query.startsWith("/users/")) {
                    if (!query.contains("/visits")) {
                        actionType = ActionType.USER_GET;
                        String idString = new String(BUFFER, start + 7, pointer - start - 7);
                        try {
                            Integer id = Integer.valueOf(idString);
                            if (id >= 1500000) {
                                throw new EntityNotFound();
                            }
                            travelService.validateUser(id);
                            ENCODE_BUFFER.clear();
                            WebCache.encodeUser(id, ENCODE_BUFFER);
                            writeTime = write(ctx, ENCODE_BUFFER);
                        } catch (EntityNotFound | NumberFormatException ex) {
                            writeTime = write(ctx,RESPONSE_404.retain().duplicate());
                        }
                    } else {
                        actionType = ActionType.USER_VISITS;
                        try {
                            Integer id = null;
                            try {
                                id = Integer.valueOf(query.substring(7, query.indexOf('/', 7)));
                            } catch (NumberFormatException ex) {
                                throw new EntityNotFound();
                            }
                            int startQ = query.indexOf('?');
                            Long fromDate = null;
                            Long toDate = null;
                            String country = null;
                            Integer toDistance = null;
                            if (startQ != -1) {
                                String[] parameters = query.substring(startQ + 1).split("&");
                                for (int i = 0; i < parameters.length; i++) {
                                    String[] parameter = parameters[i].split("=");
                                    if (parameter[0].equals("fromDate")) {
                                        fromDate = Long.valueOf(parameter[1]);
                                    } else if (parameter[0].equals("toDate")) {
                                        toDate = Long.valueOf(parameter[1]);
                                    } else if (parameter[0].equals("country")) {
                                        country = decode(parameter[1]);
                                    } else if (parameter[0].equals("toDistance")) {
                                        toDistance = Integer.valueOf(parameter[1]);
                                    }
                                }
                            }
                            if (toDistance != null && toDistance < 0) {
                                throw new BadRequest();
                            }
                            travelService.validateUser(id);
                            if (toDate != null && fromDate != null && toDate < fromDate) {
                                writeTime = write(ctx,EMPTY_VISITS.retain().duplicate());
                            } else {
                                int offset = travelService.searchUserVisits(id, fromDate, toDate, country, toDistance, VISIT_RESPONSE);
                                if (offset == 0) {
                                    writeTime = write(ctx,EMPTY_VISITS.retain().duplicate());
                                } else {
                                    long v1 = System.nanoTime();
                                    ENCODE_BUFFER.clear();
                                    ENCODE_BUFFER.writeBytes(VISIT_HEADER);
                                    ENCODE_BUFFER.writeBytes(DOUBLE_NL);
                                    int responseSize = JsonFormatters.formatVisitsList(VISIT_RESPONSE, offset, ENCODE_BUFFER, ARRAY_OUTPUT_CONTAINER);
                                    byte[] bCount = Integer.valueOf(responseSize).toString().getBytes();
                                    ENCODE_BUFFER.writeBytes(DOUBLE_NL);
                                    int position = ENCODE_BUFFER.writerIndex();
                                    ENCODE_BUFFER.writerIndex(VISIT_HEADER.length - 4);
                                    ENCODE_BUFFER.writeBytes(bCount);
                                    ENCODE_BUFFER.writerIndex(position);
                                    long v2 = System.nanoTime();
                                    Statistics.prepareResponseTime.addAndGet(v2-v1);
                                    writeTime = write(ctx,ENCODE_BUFFER);
                                }
                            }
                        } catch (EntityNotFound ex) {
                            writeTime = write(ctx,RESPONSE_404.retain().duplicate());
                            ;

                        } catch (NumberFormatException | BadRequest br) {
                            writeTime = write(ctx,RESPONSE_400.retain().duplicate());
                        }
                    }
                } else if (query.startsWith("/locations/")) {
                    if (!query.contains("/avg")) {
                        actionType = ActionType.LOCATION_GET;
                        String idString = new String(BUFFER, start + 11, pointer - start - 11);
                        try {
                            Integer id = Integer.valueOf(idString);
                            if (id >= 1500000) {
                                throw new EntityNotFound();
                            }
                            travelService.validateLocation(id);
                            ENCODE_BUFFER.clear();
                            WebCache.encodeLocation(id, ENCODE_BUFFER);

                            writeTime = write(ctx,ENCODE_BUFFER);
                        } catch (EntityNotFound | NumberFormatException ex) {
                            writeTime = write(ctx,RESPONSE_404.retain().duplicate());
                            ;

                        }
                    } else {
                        actionType = ActionType.LOCATION_AVG;
                        try {
                            Integer id = null;
                            try {
                                id = Integer.valueOf(query.substring(11, query.indexOf('/', 11)));
                            } catch (NumberFormatException ex) {
                                throw new EntityNotFound();
                            }
                            int startQ = query.indexOf('?');
                            Long fromDate = null;
                            Long toDate = null;
                            Integer fromAge = null;
                            Integer toAge = null;
                            String gender = null;
                            if (startQ != -1) {
                                String[] parameters = query.substring(startQ + 1).split("&");
                                for (int i = 0; i < parameters.length; i++) {
                                    String[] parameter = parameters[i].split("=");
                                    if (parameter[0].equals("fromDate")) {
                                        fromDate = Long.valueOf(parameter[1]);
                                    } else if (parameter[0].equals("toDate")) {
                                        toDate = Long.valueOf(parameter[1]);
                                    } else if (parameter[0].equals("fromAge")) {
                                        fromAge = Integer.valueOf(parameter[1]);
                                    } else if (parameter[0].equals("toAge")) {
                                        toAge = Integer.valueOf(parameter[1]);
                                    } else if (parameter[0].equals("gender")) {
                                        gender = parameter[1];
                                        if (!gender.equals("f") && !gender.equals("m")) {
                                            throw new BadRequest();
                                        }
                                    }
                                }
                            }
                            travelService.validateLocation(id);
                            if (fromAge != null && toAge != null && fromAge > toAge) {
                                writeTime = write(ctx,ZERO_MARK.retain().duplicate());
                            } else if (toDate != null && fromDate != null && toDate < fromDate) {
                                writeTime = write(ctx,ZERO_MARK.retain().duplicate());
                            } else {

                                Double mark = travelService.calculateLocationMark(id,
                                        fromDate,
                                        toDate,
                                        fromAge,
                                        toAge,
                                        gender
                                );

                                if (mark == 0.0) {
                                    writeTime = write(ctx,ZERO_MARK.retain().duplicate());
                                } else {
                                    StringBuilder stringBuilder = new StringBuilder("{\"avg\":}");
                                    byte[] binaryResponse = stringBuilder.insert(7, format(mark, 5)).toString().getBytes();
                                    byte[] header = ("HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=utf-8\r\nContent-Length: " + binaryResponse.length + "\r\n\r\n").getBytes();
                                    byte[] result = new byte[binaryResponse.length + header.length];
                                    long avg1 = System.nanoTime();
                                    System.arraycopy(header, 0, result, 0, header.length);
                                    System.arraycopy(binaryResponse, 0, result, header.length, binaryResponse.length);
                                    ENCODE_BUFFER.clear();
                                    ENCODE_BUFFER.writeBytes(result);
                                    long avg2 = System.nanoTime();
                                    Statistics.prepareResponseTime.addAndGet(avg2-avg1);
                                    writeTime = write(ctx,ENCODE_BUFFER);
                                }
                            }
                        } catch (EntityNotFound ex) {
                            writeTime = write(ctx,RESPONSE_404.retain().duplicate());

                        } catch (NumberFormatException | BadRequest br) {
                            writeTime = write(ctx,RESPONSE_400.retain().duplicate());
                        }
                    }
                } else if (query.startsWith("/visits/")) {
                    actionType = ActionType.VISIT_GET;
                    String idString = new String(BUFFER, start + 8, pointer - start - 8);
                    try {
                        Integer id = Integer.valueOf(idString);
                        if (id >= 11000000) {
                            throw new EntityNotFound();
                        }
                        ENCODE_BUFFER.clear();
                        WebCache.encodeVisit(travelService.getVisit(id), ENCODE_BUFFER, ARRAY_OUTPUT_CONTAINER);
                        writeTime = write(ctx,ENCODE_BUFFER);
                    } catch (EntityNotFound | NumberFormatException ex) {
                        writeTime = write(ctx,RESPONSE_404.retain().duplicate());

                    }
                }
            } else if (BUFFER[0] == 'P') {
                pointer += 5;
                int start = pointer;
                while (BUFFER[pointer] != ' ') {
                    pointer++;
                }
                String query = new String(BUFFER, 5, pointer - 5);
                if (query.startsWith("/users")) {
                    actionType = ActionType.USER_POST;
                    if (query.startsWith("/users/new")) {
                        while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                            pointer++;
                        }
                        try {
                            User user = UserParser.parseUser(BUFFER, pointer + 1, count - pointer - 1);
                            travelService.createUser(user);
                            writeTime = write(ctx,RESPONSE_200.retain().duplicate());
                        } catch (BadRequest br) {
                            writeTime = write(ctx,RESPONSE_400.retain().duplicate());
                        }
                    } else {
                        String idString = new String(BUFFER, start + 7, pointer - start - 7);
                        if (idString.contains("?")) {
                            idString = idString.substring(0, idString.indexOf('?'));
                        }
                        try {
                            Integer id = Integer.valueOf(idString);
                            travelService.validateUser(id);
                            while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                                pointer++;
                            }
                            User user = UserParser.parseUser(BUFFER, pointer + 1, count - pointer - 1);
                            user.id = id;
                            travelService.updateUser(user);
                            writeTime = write(ctx,RESPONSE_200.retain().duplicate());
                        } catch (EntityNotFound | NumberFormatException ex) {
                            writeTime = write(ctx,RESPONSE_404.retain().duplicate());
                        } catch (BadRequest br) {
                            writeTime = write(ctx,RESPONSE_400.retain().duplicate());
                        }
                    }
                } else if (query.startsWith("/locations")) {
                    actionType = ActionType.LOCATION_POST;
                    if (query.startsWith("/locations/new")) {
                        while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                            pointer++;
                        }
                        try {
                            Location location = LocationParser.parseLocation(BUFFER, pointer + 1, count - pointer - 1);
                            travelService.createLocation(location);
                            writeTime = write(ctx,RESPONSE_200.retain().duplicate());
                        } catch (BadRequest br) {
                            writeTime = write(ctx,RESPONSE_400.retain().duplicate());
                        }
                    } else {
                        String idString = new String(BUFFER, start + 11, pointer - start - 11);
                        if (idString.contains("?")) {
                            idString = idString.substring(0, idString.indexOf('?'));
                        }
                        try {
                            Integer id = Integer.valueOf(idString);
                            travelService.validateLocation(id);
                            while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                                pointer++;
                            }
                            Location location = LocationParser.parseLocation(BUFFER, pointer + 1, count - pointer - 1);
                            location.id = id;
                            travelService.updateLocation(location);
                            writeTime = write(ctx,RESPONSE_200.retain().duplicate());
                        } catch (EntityNotFound | NumberFormatException ex) {
                            writeTime = write(ctx,RESPONSE_404.retain().duplicate());
                        } catch (BadRequest br) {
                            writeTime = write(ctx,RESPONSE_400.retain().duplicate());
                        }
                    }
                } else if (query.startsWith("/visits")) {
                    actionType = ActionType.VISIT_POST;
                    if (query.startsWith("/visits/new")) {
                        while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                            pointer++;
                        }
                        try {
                            Visit visit = VisitParser.parseVisit(BUFFER, pointer + 1, count - pointer - 1);
                            travelService.createVisit(visit);
                            writeTime = write(ctx,RESPONSE_200.retain().duplicate());
                        } catch (BadRequest br) {
                            writeTime = write(ctx,RESPONSE_400.retain().duplicate());
                        }
                    } else {
                        String idString = new String(BUFFER, start + 8, pointer - start - 8);
                        if (idString.contains("?")) {
                            idString = idString.substring(0, idString.indexOf('?'));
                        }
                        try {
                            Integer id = Integer.valueOf(idString);
                            travelService.validateVisit(id);
                            while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                                pointer++;
                            }
                            Visit visit = VisitParser.parseVisit(BUFFER, pointer + 1, count - pointer - 1);
                            visit.id = id;
                            travelService.updateVisit(visit);
                            writeTime = write(ctx,RESPONSE_200.retain().duplicate());
                        } catch (EntityNotFound | NumberFormatException ex) {
                            writeTime = write(ctx,RESPONSE_404.retain().duplicate());
                        } catch (BadRequest br) {
                            writeTime = write(ctx,RESPONSE_400.retain().duplicate());
                        }
                    }
                }
            } else {
                System.out.println(new Date());
                System.out.println(buf);
                System.out.println(BUFFER);
                ctx.close();
            }
            //release((ByteBuf) msg);
            //release(buf);
        } catch (Throwable e) {
            e.printStackTrace();
            ctx.close();
        } finally {
            long t2 = System.nanoTime();
            Statistics.totalHandlerTime.addAndGet(t2-t1);
            if (actionType != null) {
                Statistics.log(actionType, t2-t1);
            }
            long clearTime = t2-t1-writeTime -preHandleTime;
            Statistics.updateMaxClearTime(clearTime);
            if (actionType != null) {
                Statistics.logClear(actionType, clearTime);
            }
        }
    }

    private long write(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        long t1 = System.nanoTime();
        int size = ((AbstractEpoll0Channel)ctx.channel()).doWriteBytes(byteBuf,16);
        long t2 = System.nanoTime();
        Statistics.updateMaxWriteCount(size);
        Statistics.ioWriteTime.addAndGet(t2-t1);
        Statistics.updateIoMaxTime(t2-t1);
        return t2-t1;
    }

    private String decode(String parameter) {
        try {
            return URLDecoder.decode(parameter, "UTF-8");
        } catch (UnsupportedEncodingException e) {

        }
        return null;
    }

    private static final int POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000};

    public void release(ByteBuf byteBuf) {
        while (byteBuf.refCnt() != 0) {
            byteBuf.release();
        }
    }

    public static String format(double val, int precision) {
        StringBuilder sb = new StringBuilder();
        if (val < 0) {
            sb.append('-');
            val = -val;
        }
        int exp = POW10[precision];
        long lval = (long)(val * exp + 0.5);
        sb.append(lval / exp).append('.');
        long fval = lval % exp;
        for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
            sb.append('0');
        }
        sb.append(fval);
        return sb.toString();
    }
}
