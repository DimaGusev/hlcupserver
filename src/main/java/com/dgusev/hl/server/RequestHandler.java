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

    public static final byte[] VISIT_HEADER = ("HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=utf-8\r\nContent-Length:     \r\n\r\n").getBytes();
    public static final byte[] DOUBLE_NL = "\r\n\r\n".getBytes();

    public static final char[] CONTENT_0 = "Content-Length: 0".toCharArray();


    //URLS
    public static final char[] USERS_URL = "/users/".toCharArray();
    public static final char[] USER_VISITS_URL = "/visits".toCharArray();
    public static final char[] LOCATIONS_URL = "/locations/".toCharArray();
    public static final char[] AVG_URL = "/avg".toCharArray();
    public static final char[] VISITS_URL = "/visits/".toCharArray();
    public static final char[] USERS_NEW_URL = "/users/new".toCharArray();
    public static final char[] LOCATIONS_NEW_URL = "/locations/new".toCharArray();
    public static final char[] VISITS_NEW_URL = "/visits/new".toCharArray();

    @Autowired
    private TravelService travelService;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            WorkerThread workerThread = (WorkerThread) Thread.currentThread();
            char[] BUFFER = workerThread.BUFFER;
            byte[] ARRAY_INPUT_CONTAINER = workerThread.ARRAY_INPUT_CONTAINER;
            byte[] ARRAY_OUTPUT_CONTAINER = workerThread.ARRAY_OUTPUT_CONTAINER;
            ByteBuf ENCODE_BUFFER = workerThread.ENCODE_BUFFER;
            AttributeKey<ByteBuf> attributeKey = workerThread.ATTRIBUTE_KEY;
            VisitResponse[] VISIT_RESPONSE = workerThread.VISIT_RESPONSE;
            ByteBuf fragmentedMessage = ctx.channel().attr(attributeKey).get();
            if (fragmentedMessage != null) {
                fragmentedMessage.writeBytes((ByteBuf) msg);
                msg = fragmentedMessage;
            }
            ByteBuf buf = (ByteBuf) msg;
            int count = StringUTFCodec.decode(buf, ARRAY_INPUT_CONTAINER, BUFFER);
            if ((BUFFER[0] == 'P' && ((BUFFER[count - 1] != '\n' || BUFFER[count - 2] != '\r' || BUFFER[count - 3] != '}') && (!contains(BUFFER, CONTENT_0)))) || (BUFFER[0] == 'G' && (BUFFER[count - 1] != '\n' || BUFFER[count - 2] != '\r' || BUFFER[count - 3] != '\n' || BUFFER[count - 4] != '\r'))) {
                ByteBuf byteBuf = ctx.channel().attr(attributeKey).get();
                if (byteBuf == null) {
                    byteBuf = ctx.alloc().directBuffer(1000);
                    ctx.channel().attr(attributeKey).set(byteBuf);
                }
                buf.readerIndex(0);
                byteBuf.writeBytes(buf);
                return;
            } else if (fragmentedMessage != null) {
                ctx.channel().attr(attributeKey).set(null);
                release(fragmentedMessage);
            }
            int pointer = 0;
            if (BUFFER[0] == 'G') {
                pointer += 4;
                int start = pointer;
                while (BUFFER[pointer] != ' ') {
                    pointer++;
                }
                if (startsWith(BUFFER, 4, USERS_URL)) {
                    if (!contains(BUFFER, 4, pointer, USER_VISITS_URL)) {
                        int id = parseInt(BUFFER, start + 7, pointer - start - 7);
                        try {
                            if (id >= 1500000 || id < 0) {
                                throw new EntityNotFound();
                            }
                            travelService.validateUser(id);
                            ENCODE_BUFFER.clear();
                            WebCache.encodeUser(id, ENCODE_BUFFER);
                            write(ctx, ENCODE_BUFFER);
                        } catch (EntityNotFound | NumberFormatException ex) {
                            write(ctx, RESPONSE_404);
                        }
                    } else {
                        try {
                            String query = new String(BUFFER, 4, pointer - 4);
                            int qIndex = indexOf(BUFFER, 11, pointer - 11, '/');
                            int dCount = pointer - 11;
                            if (qIndex != -1) {
                                dCount = qIndex;
                            }
                            int id = parseInt(BUFFER, 11, dCount);
                            if (id < 0) {
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
                                write(ctx, EMPTY_VISITS);
                            } else {
                                int offset = travelService.searchUserVisits(id, fromDate, toDate, country, toDistance, VISIT_RESPONSE);
                                if (offset == 0) {
                                    write(ctx, EMPTY_VISITS);
                                } else {
                                    ENCODE_BUFFER.clear();
                                    System.arraycopy(VISIT_HEADER, 0, ARRAY_OUTPUT_CONTAINER, 0, VISIT_HEADER.length);
                                    int responseSize = JsonFormatters.formatVisitsList(VISIT_RESPONSE, offset, ARRAY_OUTPUT_CONTAINER, VISIT_HEADER.length);
                                    System.arraycopy(DOUBLE_NL, 0, ARRAY_OUTPUT_CONTAINER, VISIT_HEADER.length + responseSize, DOUBLE_NL.length);
                                    format(responseSize, ARRAY_OUTPUT_CONTAINER, VISIT_HEADER.length - 8);
                                    ENCODE_BUFFER.writeBytes(ARRAY_OUTPUT_CONTAINER, 0, VISIT_HEADER.length + responseSize + DOUBLE_NL.length);
                                    write(ctx, ENCODE_BUFFER);
                                }
                            }
                        } catch (EntityNotFound ex) {
                            write(ctx, RESPONSE_404);

                        } catch (NumberFormatException | BadRequest br) {
                            write(ctx, RESPONSE_400);
                        }
                    }
                } else if (startsWith(BUFFER, 4, LOCATIONS_URL)) {
                    if (!contains(BUFFER, 4, pointer, AVG_URL)) {
                        try {
                            int id = parseInt(BUFFER, start + 11, pointer - start - 11);
                            if (id >= 1500000 || id < 0) {
                                throw new EntityNotFound();
                            }
                            travelService.validateLocation(id);
                            ENCODE_BUFFER.clear();
                            WebCache.encodeLocation(id, ENCODE_BUFFER);

                            write(ctx, ENCODE_BUFFER);
                        } catch (EntityNotFound | NumberFormatException ex) {
                            write(ctx, RESPONSE_404);
                        }
                    } else {
                        try {
                            String query = new String(BUFFER, 4, pointer - 4);
                            int qIndex = indexOf(BUFFER, 15, pointer - 15, '/');
                            int dCount = pointer - 15;
                            if (qIndex != -1) {
                                dCount = qIndex;
                            }
                            int id = parseInt(BUFFER, 15, dCount);
                            if (id < 0) {
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
                                write(ctx, ZERO_MARK);
                            } else if (toDate != null && fromDate != null && toDate < fromDate) {
                                write(ctx, ZERO_MARK);
                            } else {

                                Double mark = travelService.calculateLocationMark(id,
                                        fromDate,
                                        toDate,
                                        fromAge,
                                        toAge,
                                        gender
                                );

                                if (mark == 0.0) {
                                    write(ctx, ZERO_MARK);
                                } else {
                                    byte[] LOCATION_AVG_RESPONSE = workerThread.LOCATION_AVG_RESPONSE;
                                    format(mark, 5, LOCATION_AVG_RESPONSE, 116);
                                    ENCODE_BUFFER.clear();
                                    ENCODE_BUFFER.writeBytes(LOCATION_AVG_RESPONSE);
                                    write(ctx, ENCODE_BUFFER);
                                }
                            }
                        } catch (EntityNotFound ex) {
                            write(ctx, RESPONSE_404);
                        } catch (NumberFormatException | BadRequest br) {
                            write(ctx, RESPONSE_400);
                        }
                    }
                } else if (startsWith(BUFFER, 4, VISITS_URL)) {
                    try {
                        int id = parseInt(BUFFER, start + 8, pointer - start - 8);
                        if (id >= 11000000 || id < 0) {
                            throw new EntityNotFound();
                        }
                        ENCODE_BUFFER.clear();
                        WebCache.encodeVisit(travelService.getVisit(id), ENCODE_BUFFER, ARRAY_OUTPUT_CONTAINER);
                        write(ctx, ENCODE_BUFFER);
                    } catch (EntityNotFound | NumberFormatException ex) {
                        write(ctx, RESPONSE_404);
                    }
                }
            } else if (BUFFER[0] == 'P') {
                pointer += 5;
                int start = pointer;
                while (BUFFER[pointer] != ' ') {
                    pointer++;
                }
                if (startsWith(BUFFER, 5, USERS_URL)) {
                    if (startsWith(BUFFER, 5, USERS_NEW_URL)) {
                        while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                            pointer++;
                        }
                        try {
                            User user = UserParser.parseUser(BUFFER, pointer + 1, count - pointer - 1);
                            travelService.createUser(user);
                            write(ctx, RESPONSE_200);
                        } catch (BadRequest br) {
                            write(ctx, RESPONSE_400);
                        }
                    } else {
                        int qIndex = indexOf(BUFFER, start + 7, pointer - start - 7, '?');
                        int dCount = pointer - start - 7;
                        if (qIndex != -1) {
                            dCount = qIndex;
                        }
                        try {
                            int id = parseInt(BUFFER, start +7, dCount);
                            travelService.validateUser(id);
                            while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                                pointer++;
                            }
                            User user = UserParser.parseUser(BUFFER, pointer + 1, count - pointer - 1);
                            user.id = id;
                            travelService.updateUser(user);
                            write(ctx, RESPONSE_200);
                        } catch (EntityNotFound | NumberFormatException ex) {
                            write(ctx, RESPONSE_404);
                        } catch (BadRequest br) {
                            write(ctx, RESPONSE_400);
                        }
                    }
                } else if (startsWith(BUFFER, 5, LOCATIONS_URL)) {
                    if (startsWith(BUFFER, 5, LOCATIONS_NEW_URL)) {
                        while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                            pointer++;
                        }
                        try {
                            Location location = LocationParser.parseLocation(BUFFER, pointer + 1, count - pointer - 1);
                            travelService.createLocation(location);
                            write(ctx, RESPONSE_200);
                        } catch (BadRequest br) {
                            write(ctx, RESPONSE_400);
                        }
                    } else {
                        int qIndex = indexOf(BUFFER, start + 11, pointer - start - 11, '?');
                        int dCount = pointer - start - 11;
                        if (qIndex != -1) {
                            dCount = qIndex;
                        }
                        try {
                            int id = parseInt(BUFFER, start + 11, dCount);
                            travelService.validateLocation(id);
                            while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                                pointer++;
                            }
                            Location location = LocationParser.parseLocation(BUFFER, pointer + 1, count - pointer - 1);
                            location.id = id;
                            travelService.updateLocation(location);
                            write(ctx, RESPONSE_200);
                        } catch (EntityNotFound | NumberFormatException ex) {
                            write(ctx, RESPONSE_404);
                        } catch (BadRequest br) {
                            write(ctx, RESPONSE_400);
                        }
                    }
                } else if (startsWith(BUFFER, 5, VISITS_URL)) {
                    if (startsWith(BUFFER, 5, VISITS_NEW_URL)) {
                        while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                            pointer++;
                        }
                        try {
                            Visit visit = VisitParser.parseVisit(BUFFER, pointer + 1, count - pointer - 1);
                            travelService.createVisit(visit);
                            write(ctx, RESPONSE_200);
                        } catch (BadRequest br) {
                            write(ctx, RESPONSE_400);
                        }
                    } else {
                        int qIndex = indexOf(BUFFER, start + 8, pointer - start - 8, '?');
                        int dCount = pointer - start - 8;
                        if (qIndex != -1) {
                            dCount = qIndex;
                        }
                        try {
                            int id = parseInt(BUFFER, start + 8, dCount);
                            travelService.validateVisit(id);
                            while (!(BUFFER[pointer] == '\n' && BUFFER[pointer - 1] == '\r' && BUFFER[pointer - 2] == '\n' && BUFFER[pointer - 3] == '\r')) {
                                pointer++;
                            }
                            Visit visit = VisitParser.parseVisit(BUFFER, pointer + 1, count - pointer - 1);
                            visit.id = id;
                            travelService.updateVisit(visit);
                            write(ctx, RESPONSE_200);
                        } catch (EntityNotFound | NumberFormatException ex) {
                            write(ctx, RESPONSE_404);
                        } catch (BadRequest br) {
                            write(ctx, RESPONSE_400);
                        }
                    }
                }
            } else {
                System.out.println(new Date());
                System.out.println(buf);
                System.out.println(BUFFER);
                ctx.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            ctx.close();
        }
    }

    private void write(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        ((AbstractEpoll0Channel) ctx.channel()).doWriteBytes(byteBuf);
    }

    private String decode(String parameter) {
        try {
            return URLDecoder.decode(parameter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    private static final int POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    private void release(ByteBuf byteBuf) {
        while (byteBuf.refCnt() != 0) {
            byteBuf.release();
        }
    }

    private void format(double val, int precision, byte[] buf, int offset) {
        int exp = POW10[precision];
        long lval = (long) (val * exp + 0.5);
        int position = offset;
        buf[position++] = (byte)(48 + (lval / exp));
        buf[position++] = 46;
        long fval = lval % exp;
        for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
            buf[position++] = 48;
        }
        boolean printZero = false;
        for (int i = 5; i>=0; i--) {
            int digit = (int)(fval/POW10[i]);
            if (digit == 0 && !printZero) {
                continue;
            }
            buf[position++] = (byte)(48 + digit);
            printZero=true;
            fval -= (fval/POW10[i]) * POW10[i];
        }
        int currentPosition = position;
        for (int j = 0; j < 7 -(currentPosition - offset); j++) {
            buf[position++] = 48;
        }
    }

    private void format(int val, byte[] buf, int offset) {
        int position = offset;
        boolean printZero = false;
        for (int i = 5; i>=0; i--) {
            int digit = (int)(val/POW10[i]);
            if (digit == 0 && !printZero) {
                continue;
            }
            buf[position++] = (byte)(48 + digit);
            printZero=true;
            val -= (val/POW10[i]) * POW10[i];
        }
    }



    private  boolean contains(char[] buf, char[] part) {
        int position = 0;
        int maxLength = buf.length;
        char first = part[0];
        int partSize = part.length;
        while (position < maxLength) {
            if (buf[position] == first) {
                if (position + partSize >= maxLength) {
                    return false;
                } else {
                    int partOffset = 0;
                    while ((partOffset < partSize) && buf[position++] == part[partOffset++]);
                    if (partOffset == partSize) {
                        return true;
                    }
                }
            } else {
                position++;
            }
        }
        return false;
    }

    private  boolean contains(char[] buf, int from, int to, char[] part) {
        int position = from;
        int maxLength = to;
        char first = part[0];
        int partSize = part.length;
        while (position < maxLength) {
            if (buf[position] == first) {
                if (position + partSize > maxLength) {
                    return false;
                } else {
                    int partOffset = 0;
                    while ((partOffset < partSize) && buf[position++] == part[partOffset++]);
                    if (partOffset == partSize) {
                        return true;
                    }
                }
            } else {
                position++;
            }
        }
        return false;
    }

    private boolean startsWith(char[] buf, int position, char[] part) {
        for (int i = 0; i< part.length; i++) {
            if (buf[position + i] != part[i]) {
                return false;
            }
        }
        return true;
    }

    private int parseInt(char[] buf, int from, int count) {
        int id = 0;
        for (int i = 0; i< count; i++) {
            int digit = buf[from + count - i - 1] - 48;
            if (digit < 0 || digit > 9 ) {
                return -1;
            }
            id+=digit*POW10[i];
        }
        return id;
    }

    private int indexOf(char[] buf, int from, int count, char ch) {
        for (int i = 0; i< count;i++) {
            if (buf[from + i] == ch) {
                return i;
            }
        }
        return -1;
    }
}
