package com.dgusev.hl.server.threads;

import net.openhft.affinity.AffinityLock;

import java.util.concurrent.ThreadFactory;

/**
 * Created by dgusev on 12.09.2017.
 */
public class WorkerThreadFactory implements ThreadFactory {

    private boolean affinity;


    public WorkerThreadFactory(boolean affinity) {
        this.affinity = affinity;
    }

    @Override
    public Thread newThread(Runnable r) {
        if (affinity) {
            return new WorkerThread(() -> {
                try (AffinityLock affinityLock = AffinityLock.acquireCore()) {
                    r.run();
                }
            });
        } else {
            return new WorkerThread(r);
        }
    }
}
