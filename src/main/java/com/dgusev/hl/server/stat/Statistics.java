package com.dgusev.hl.server.stat;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dgusev on 29.09.2017.
 */
public class Statistics {

    public static  AtomicInteger tasksCount = new AtomicInteger(0);
    public static AtomicInteger eventsCount = new AtomicInteger(0);
    public static AtomicInteger inEventCount = new AtomicInteger(0);
    public static AtomicInteger rdHubCount = new AtomicInteger(0);
    public static AtomicInteger wuCount = new AtomicInteger(0);
    public static AtomicInteger tCount = new AtomicInteger(0);
    public static AtomicInteger brakeEpollCount = new AtomicInteger(0);
    public static AtomicInteger spliceQueueCount = new AtomicInteger(0);
    public static AtomicInteger nothingReadCount = new AtomicInteger(0);
    public static AtomicInteger brakeEpoll1Count = new AtomicInteger(0);
    public static AtomicInteger continueReadingCount = new AtomicInteger(0);
    public static AtomicInteger closeCount = new AtomicInteger(0);


    public static void clear() {
        tasksCount = new AtomicInteger(0);
        eventsCount = new AtomicInteger(0);
        inEventCount = new AtomicInteger(0);
        rdHubCount = new AtomicInteger(0);
        wuCount = new AtomicInteger(0);
        tCount = new AtomicInteger(0);
        brakeEpollCount = new AtomicInteger(0);
        spliceQueueCount = new AtomicInteger(0);
        nothingReadCount = new AtomicInteger(0);
        brakeEpoll1Count = new AtomicInteger(0);
        continueReadingCount = new AtomicInteger(0);
        closeCount = new AtomicInteger(0);
    }

    public static String  getStat() {
        return "Statistics{" +
                "tasksCount=" + tasksCount +
                ", eventsCount=" + eventsCount +
                ", inEventCount=" + inEventCount +
                ", rdHubCount=" + rdHubCount +
                ", wuCount=" + wuCount +
                ", tCount=" + tCount +
                ", brakeEpollCount=" + brakeEpollCount +
                ", spliceQueueCount=" + spliceQueueCount +
                ", nothingReadCount=" + nothingReadCount +
                ", brakeEpoll1Count=" + brakeEpoll1Count +
                ", continueReadingCount=" + continueReadingCount +
                ", closeCount=" + closeCount +
                '}';
    }
}
