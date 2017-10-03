package com.dgusev.hl.server.threads;

import com.dgusev.hl.server.model.VisitResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dgusev on 12.09.2017.
 */
public class WorkerThread extends Thread {

    public final char[] BUFFER = new char[10000];
    public final byte[] ARRAY_INPUT_CONTAINER = new byte[10000];
    public final byte[] ARRAY_OUTPUT_CONTAINER = new byte[10000];
    public final VisitResponse[] VISIT_RESPONSE = new VisitResponse[600];
    public final AttributeKey<ByteBuf> ATTRIBUTE_KEY = AttributeKey.valueOf("fragment");
    public final ByteBuf ENCODE_BUFFER = Unpooled.directBuffer(10000);
    public final ByteBuf READ_BUFFER = Unpooled.directBuffer(5000);
    public final byte[] LOCATION_AVG_RESPONSE = "HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=utf-8\r\nContent-Length: 15\r\n\r\n{\"avg\":       }".getBytes();



    public WorkerThread(Runnable runnable) {
        super(runnable);
        for (int i = 0;i< 600;i++) {
            VISIT_RESPONSE[i] = new VisitResponse();
        }
    }
}
