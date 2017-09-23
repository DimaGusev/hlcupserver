package com.dgusev.hl.server.threads;

import net.openhft.affinity.AffinityLock;

import java.util.concurrent.ThreadFactory;

/**
 * Created by dgusev on 12.09.2017.
 */
public class WorkerThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        return new WorkerThread(r);
    }
}
