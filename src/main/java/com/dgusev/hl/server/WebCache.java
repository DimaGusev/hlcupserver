package com.dgusev.hl.server;

import com.dgusev.hl.server.model.Location;
import com.dgusev.hl.server.model.User;
import com.dgusev.hl.server.model.Visit;
import com.dgusev.hl.server.parsers.JsonFormatters;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * Created by dgusev on 19.08.2017.
 */
public class WebCache {

    private static final ByteBuffer[] users = new ByteBuffer[1500000];
    private static final ByteBuffer[] locations = new ByteBuffer[1500000];
    //private static final byte[][] visits = new byte[15000000][];

    public static final byte[] RESPONSE_200_TEMPLATE = "HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=UTF-8\r\nDate: Sat, 19 Aug 2017 11:30:12 GMT\r\nContent-Length:    \r\n\r\n".getBytes();



    public static void cacheUser(User user) {
        byte[] response = JsonFormatters.format(user);
        ByteBuffer buffer = ByteBuffer.allocate(response.length);
        buffer.put(response);
        buffer.flip();
        users[user.id] = buffer;
    }

    public static void cacheLocation(Location location) {
        byte[] response = JsonFormatters.format(location);
        ByteBuffer buffer = ByteBuffer.allocate(response.length);
        buffer.put(response);
        buffer.flip();
        locations[location.id] =  buffer;
    }

    //public static void cacheVisit(Visit visit) {
    //    byte[] response = JsonFormatters.format(visit);
    //    visits[visit.id] = response;
    //}

    public static int encodeUser(Integer id, ByteBuf encodeBuffer) {
        encodeBuffer.writeBytes(RESPONSE_200_TEMPLATE);
        ByteBuffer userArray = users[id];
        if (userArray.limit() > 99) {
            int d100 = userArray.limit() / 100;
            encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length - 7);
            encodeBuffer.writeByte((byte)(48  + d100));
        }
        encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length - 6);
        int d10 = (userArray.limit()/10) % 10;
        encodeBuffer.writeByte((byte)(48  + d10));
        int d0 = userArray.limit() % 10;
        encodeBuffer.writeByte((byte)(48  + d0));
        encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length);
        encodeBuffer.writeBytes(userArray.array());
        return RESPONSE_200_TEMPLATE.length + userArray.limit();
    }

    public static int encodeLocation(Integer id, ByteBuf encodeBuffer) {
        encodeBuffer.writeBytes(RESPONSE_200_TEMPLATE);
        ByteBuffer locationArray = locations[id];
        if (locationArray.limit() > 99) {
            int d100 = locationArray.limit() / 100;
            encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length - 7);
            encodeBuffer.writeByte((byte)(48  + d100));
        }
        encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length - 6);
        int d10 = (locationArray.limit()/10) % 10;
        encodeBuffer.writeByte((byte)(48  + d10));
        int d0 = locationArray.limit() % 10;
        encodeBuffer.writeByte((byte)(48  + d0));
        encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length);
        encodeBuffer.writeBytes(locationArray.array());
        return RESPONSE_200_TEMPLATE.length + locationArray.limit();
    }

    public static int encodeVisit(Visit visit, ByteBuf encodeBuffer, byte[] buf) {
        encodeBuffer.writeBytes(RESPONSE_200_TEMPLATE);
        int size = JsonFormatters.format(visit, encodeBuffer, buf);
        if (size > 99) {
            int d100 = size / 100;
            encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length - 7);
            encodeBuffer.writeByte((byte)(48  + d100));
        }
        encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length - 6);
        int d10 = (size/10) % 10;
        encodeBuffer.writeByte((byte)(48  + d10));
        int d0 = size % 10;
        encodeBuffer.writeByte((byte)(48  + d0));
        encodeBuffer.writerIndex(RESPONSE_200_TEMPLATE.length + size);
        return RESPONSE_200_TEMPLATE.length + size;
    }

}
