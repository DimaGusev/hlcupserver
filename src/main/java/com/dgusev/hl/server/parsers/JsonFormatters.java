package com.dgusev.hl.server.parsers;


import com.dgusev.hl.server.codecs.StringUTFCodec;
import com.dgusev.hl.server.model.Location;
import com.dgusev.hl.server.model.User;
import com.dgusev.hl.server.model.Visit;
import com.dgusev.hl.server.model.VisitResponse;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by dgusev on 18.08.2017.
 */
public class JsonFormatters {

    private static final byte[] VISITS = "{\"visits\":[".getBytes();
    private static final byte[] MARK = "{\"mark\":".getBytes();
    private static final byte[] MARK_COMMA = ",{\"mark\":".getBytes();
    private static final byte[] PLACE = ",\"place\":\"".getBytes();
    private static final byte[] VISITED_AT = "\",\"visited_at\":".getBytes();
    private static final byte[] CLOSE_OBJECT = "}".getBytes();
    private static final byte[] CLOSE_FULL = "]}".getBytes();


    private static final byte[] VISIT_GET1 = "{\"id\":".getBytes();
    private static final byte[] VISIT_GET2 = ",\"user\":".getBytes();
    private static final byte[] VISIT_GET3 = ",\"location\":".getBytes();
    private static final byte[] VISIT_GET4 = ",\"visited_at\":".getBytes();
    private static final byte[] VISIT_GET5 = ",\"mark\":".getBytes();
    private static final byte[] VISIT_GET6 = "}".getBytes();



    private static Field fieldS;
    private static Field fieldSB;

    static {
        try {
            fieldS = String.class.getDeclaredField("value");
            fieldS.setAccessible(true);
            fieldSB = Class.forName("java.lang.AbstractStringBuilder").getDeclaredField("value");
            fieldSB.setAccessible(true);
        } catch (NoSuchFieldException e) {

        } catch (ClassNotFoundException e) {
        }
    }


    public static byte[] formatUser(User user) {
        StringBuilder stringBuilder = new StringBuilder(200);
        stringBuilder.append("{\"id\":").append(user.id);
        stringBuilder.append(",\"birth_date\":").append(user.birthDate);
        stringBuilder.append(",\"gender\":\"").append(user.gender);
        stringBuilder.append("\",\"email\":\"").append(user.email);
        stringBuilder.append("\",\"first_name\":\"").append(user.firstName);
        stringBuilder.append("\",\"last_name\":\"").append(user.lastName);
        stringBuilder.append("\"}");
        return stringBuilder.toString().getBytes();
    }

    public static byte[] formatLocation(Location location) {
        StringBuilder stringBuilder = new StringBuilder(130);
        stringBuilder.append("{\"id\":").append(location.id);
        stringBuilder.append(",\"country\":\"").append(location.country);
        stringBuilder.append("\",\"city\":\"").append(location.city);
        stringBuilder.append("\",\"place\":\"").append(location.place);
        stringBuilder.append("\",\"distance\":").append(location.distance);
        stringBuilder.append("}");
        return stringBuilder.toString().getBytes();
    }

    public static byte[] format(Visit visit) {
        StringBuilder stringBuilder = new StringBuilder(130);
        stringBuilder.append("{\"id\":").append(visit.id);
        stringBuilder.append(",\"user\":").append(visit.user);
        stringBuilder.append(",\"location\":").append(visit.location);
        stringBuilder.append(",\"visited_at\":").append(visit.visitedAt);
        stringBuilder.append(",\"mark\":").append(visit.mark);
        return stringBuilder.append("}").toString().getBytes();
    }

    public static int formatVisit(Visit visit, ByteBuf buf, byte[] encodeBuffer) {
        int position = 0;
        System.arraycopy(VISIT_GET1, 0,encodeBuffer, position,VISIT_GET1.length);
        position+=VISIT_GET1.length;
        position+=encodeLong(visit.id, encodeBuffer, position);
        System.arraycopy(VISIT_GET2, 0,encodeBuffer, position,VISIT_GET2.length);
        position+=VISIT_GET2.length;
        position+=encodeLong(visit.user, encodeBuffer, position);
        System.arraycopy(VISIT_GET3, 0,encodeBuffer, position,VISIT_GET3.length);
        position+=VISIT_GET3.length;
        position+=encodeLong(visit.location, encodeBuffer, position);
        System.arraycopy(VISIT_GET4, 0,encodeBuffer, position,VISIT_GET4.length);
        position+=VISIT_GET4.length;
        position+=encodeLong(visit.visitedAt, encodeBuffer, position);
        System.arraycopy(VISIT_GET5, 0,encodeBuffer, position,VISIT_GET5.length);
        position+=VISIT_GET5.length;
        position+=encodeLong(visit.mark, encodeBuffer,  position);
        System.arraycopy(VISIT_GET6, 0,encodeBuffer, position,VISIT_GET6.length);
        position+=VISIT_GET6.length;
        buf.writeBytes(encodeBuffer, 0, position);
        return position;
    }


    private static char[] getValue(StringBuilder s) {
        try {
            return ((char[]) fieldSB.get(s));
        } catch (IllegalAccessException e) {

        }
        return null;
    }
    private static char[] getValue(String s) {
        try {
            return ((char[]) fieldS.get(s));
        } catch (IllegalAccessException e) {

        }
        return null;
    }
    public static int formatVisitsList(VisitResponse[] responseList, int size, ByteBuf encodeBuffer, byte[] buf) {
        int position = 0;
        System.arraycopy(VISITS, 0, buf, 0, VISITS.length);
        position+=VISITS.length;
        for (int i = 0; i< size;i++) {
            VisitResponse visitResponse = responseList[i];
            if (i == 0) {
                System.arraycopy(MARK, 0, buf, position, MARK.length);
                position+=MARK.length;
                buf[position] = (byte)(48 + visitResponse.mark);
                position++;
            } else {
                System.arraycopy(MARK_COMMA, 0, buf, position, MARK_COMMA.length);
                position+=MARK_COMMA.length;
                buf[position] = (byte)(48 + visitResponse.mark);
                position++;
            }
            System.arraycopy(PLACE, 0, buf, position, PLACE.length);
            position+=PLACE.length;
            String place = visitResponse.place;
            position+=StringUTFCodec.encode(getValue(place), place.length(), buf, position);
            System.arraycopy(VISITED_AT, 0, buf, position, VISITED_AT.length);
            position+=VISITED_AT.length;
            position+=encodeLong(visitResponse.visitedAt, buf, position);
            System.arraycopy(CLOSE_OBJECT, 0, buf, position, CLOSE_OBJECT.length);
            position+=CLOSE_OBJECT.length;
        }
        System.arraycopy(CLOSE_FULL, 0, buf, position, CLOSE_FULL.length);
        position+=CLOSE_FULL.length;
        encodeBuffer.writeBytes(buf, 0, position);
        return position;
    }

    private static final int POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    public static int encodeLong(long value, ByteBuffer byteBuffer) {
        int position = 0;
        if (value < 0) {
            byteBuffer.put((byte)45);
            position++;
            value = -value;
        }
        boolean printZero = false;
        for (int i = 9; i>=0; i--) {
            int digit = (int)(value/POW10[i]);
            if (digit == 0 && !printZero) {
                continue;
            }
            byteBuffer.put((byte)(48 + digit));
            position++;
            printZero=true;
            value -= (value/POW10[i]) * POW10[i];
        }
        return position;
    }

    public static int encodeLong(long value, ByteBuf byteBuffer) {
        int position = 0;
        if (value < 0) {
            byteBuffer.writeByte((byte)45);
            position++;
            value = -value;
        }
        boolean printZero = false;
        for (int i = 9; i>=0; i--) {
            int digit = (int)(value/POW10[i]);
            if (digit == 0 && !printZero) {
                continue;
            }
            byteBuffer.writeByte((byte)(48 + digit));
            position++;
            printZero=true;
            value -= (value/POW10[i]) * POW10[i];
        }
        return position;
    }

    public static int encodeLong(long value, byte[] byteBuffer, int index) {
        int position = 0;
        if (value < 0) {
            byteBuffer[index++] = (byte)45;
            position++;
            value = -value;
        }
        boolean printZero = false;
        for (int i = 9; i>=0; i--) {
            int digit = (int)(value/POW10[i]);
            if (digit == 0 && !printZero) {
                continue;
            }
            byteBuffer[index++] = (byte)(48 + digit);
            position++;
            printZero=true;
            value -= (value/POW10[i]) * POW10[i];
        }
        return position;
    }
}
