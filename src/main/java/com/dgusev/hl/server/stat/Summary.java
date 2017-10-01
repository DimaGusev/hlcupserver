package com.dgusev.hl.server.stat;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by dgusev on 30.09.2017.
 */
public class Summary {
    public volatile long maxTime;
    public volatile AtomicLong totalTime = new AtomicLong(0);
    public volatile int count;

    public void log(long time) {
        totalTime.addAndGet(time);
        if (time > maxTime) {
            maxTime =time;
        }
        count++;
    }

    @Override
    public String toString() {
        return "Summary{" +
                "maxTime=" + maxTime +
                ", totalTime=" + totalTime +
                ", count=" + count +
                '}';
    }
}
