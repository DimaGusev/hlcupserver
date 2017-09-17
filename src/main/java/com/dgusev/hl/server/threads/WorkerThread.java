package com.dgusev.hl.server.threads;

import com.dgusev.hl.server.model.VisitResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dgusev on 12.09.2017.
 */
public class WorkerThread extends Thread {

    public final char[] BUFFER = new char[10000];
    public final byte[] ARRAY_INPUT_CONTAINER = new byte[10000];
    public final byte[] ARRAY_OUTPUT_CONTAINER = new byte[10000];
    public final List<VisitResponse> VISIT_RESPONSE = new ArrayList<>(600);
    public final AttributeKey<ByteBuf> ATTRIBUTE_KEY = AttributeKey.valueOf("fragment");

    public WorkerThread(Runnable runnable) {
        super(runnable);
        for (int i = 0;i< 600;i++) {
            VISIT_RESPONSE.add(new VisitResponse());
        }
    }
}
