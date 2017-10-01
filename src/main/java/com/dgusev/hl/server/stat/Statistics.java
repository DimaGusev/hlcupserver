package com.dgusev.hl.server.stat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by dgusev on 29.09.2017.
 */
public class Statistics {

    public static  AtomicInteger tasksCount = new AtomicInteger(0);
    public static AtomicInteger eventsCount = new AtomicInteger(0);
    public static AtomicInteger inEventCount = new AtomicInteger(0);
    public static AtomicInteger rdHubCount = new AtomicInteger(0);
    public static AtomicInteger wuCount = new AtomicInteger(0);
    public static AtomicInteger nothingReadCount = new AtomicInteger(0);
    public static AtomicInteger closeCount = new AtomicInteger(0);
    public static AtomicInteger registerServerCount = new AtomicInteger(0);
    public static AtomicInteger registerClientCount = new AtomicInteger(0);
    public static AtomicInteger addChannelCount = new AtomicInteger(0);
    public static AtomicInteger removeChannelCount = new AtomicInteger(0);
    public static AtomicInteger removeChannelCount1 = new AtomicInteger(0);
    public static AtomicInteger handlerCallCount = new AtomicInteger(0);
    public static AtomicInteger fragmentationCount = new AtomicInteger(0);
    public static AtomicLong totalTime = new AtomicLong(0);
    public static AtomicLong totalHandlerTime = new AtomicLong(0);
    public static long maxTime;
    public static long maxClearTime;
    public static Map<ActionType, Summary> requestStatistics = new HashMap<>();
    public static Map<ActionType, Summary> requestClearStatistics = new HashMap<>();
    public static AtomicLong processTime = new AtomicLong(0);
    public static AtomicLong processReadTime = new AtomicLong(0);
    public static AtomicLong readTime = new AtomicLong(0);
    public static AtomicLong epollInReadyClientTime = new AtomicLong(0);
    public static AtomicLong epollInReadyServerTime = new AtomicLong(0);
    public static AtomicLong closeTime = new AtomicLong(0);
    public static AtomicLong registrationTime = new AtomicLong(0);
    public static AtomicLong ioWriteTime = new AtomicLong(0);
    public static AtomicLong readInHandlerTime = new AtomicLong(0);
    public static long ioWriteMaxTime;
    public static AtomicLong preHandleTime = new AtomicLong(0);
    public static AtomicInteger wakeUpCount = new AtomicInteger(0);
    public static int maxQueueSize;
    public static AtomicLong prepareResponseTime = new AtomicLong(0);
    public static AtomicInteger queueMore5 = new AtomicInteger(0);
    public static AtomicInteger queueMore10 = new AtomicInteger(0);
    public static AtomicInteger queueMore15 = new AtomicInteger(0);
    public static AtomicInteger queueMore20 = new AtomicInteger(0);
    public static int maxWriteCount;

    public static Queue<Integer> WRITES = new ConcurrentLinkedQueue<>();


    static {
        for (ActionType actionType: ActionType.values()) {
            requestStatistics.put(actionType, new Summary());
        }
        for (ActionType actionType: ActionType.values()) {
            requestClearStatistics.put(actionType, new Summary());
        }

    }

    public static void log(ActionType actionType, long time) {
        requestStatistics.get(actionType).log(time);
    }

    public static void logClear(ActionType actionType, long time) {
        requestClearStatistics.get(actionType).log(time);
    }


    public static void clear() {
        tasksCount = new AtomicInteger(0);
        eventsCount = new AtomicInteger(0);
        inEventCount = new AtomicInteger(0);
        rdHubCount = new AtomicInteger(0);
        wuCount = new AtomicInteger(0);
        nothingReadCount = new AtomicInteger(0);
        closeCount = new AtomicInteger(0);
        registerServerCount = new AtomicInteger(0);
        registerClientCount = new AtomicInteger(0);
        addChannelCount = new AtomicInteger(0);
        removeChannelCount = new AtomicInteger(0);
        removeChannelCount1 = new AtomicInteger(0);
        fragmentationCount = new AtomicInteger(0);
        handlerCallCount = new AtomicInteger(0);
        totalTime = new AtomicLong(0);
        totalHandlerTime = new AtomicLong(0);
        maxTime = 0;
        for (ActionType actionType: ActionType.values()) {
            requestStatistics.put(actionType, new Summary());
        }
        processTime = new AtomicLong(0);
        processReadTime = new AtomicLong(0);
        readTime = new AtomicLong(0);
        epollInReadyClientTime = new AtomicLong(0);
        closeTime = new AtomicLong(0);
        registrationTime = new AtomicLong(0);
        epollInReadyServerTime = new AtomicLong(0);
        ioWriteTime = new AtomicLong(0);
        ioWriteMaxTime = 0;
        maxClearTime = 0;
        for (ActionType actionType: ActionType.values()) {
            requestClearStatistics.put(actionType, new Summary());
        }
        readInHandlerTime = new AtomicLong(0);
        preHandleTime = new AtomicLong(0);
        wakeUpCount = new AtomicInteger(0);
        prepareResponseTime = new AtomicLong(0);
        queueMore5 = new AtomicInteger(0);
        queueMore10 = new AtomicInteger(0);
        queueMore15 = new AtomicInteger(0);
        queueMore20 = new AtomicInteger(0);
        maxWriteCount = 0;
    }

    public static void updateMaxTime(long newTime) {
        if (newTime > maxTime) {
            maxTime = newTime;
        }
    }

    public static void updateMaxClearTime(long newTime) {
        if (newTime > maxClearTime) {
            maxClearTime = newTime;
        }
    }

    public static void updateIoMaxTime(long newTime) {
        WRITES.add((int)newTime);
        if (newTime > ioWriteMaxTime) {
            ioWriteMaxTime = newTime;
        }
    }

    public static void updateMaxQueueSize(int newTime) {
        if (newTime > maxQueueSize) {
            maxQueueSize = newTime;
        }
    }

    public static void updateMaxWriteCount(int newTime) {
        if (newTime > maxWriteCount) {
            maxWriteCount = newTime;
        }
    }

    public static String  getStat() {
        System.out.println(WRITES);
        try {
            Files.write(Paths.get("times.txt"), WRITES.toString().getBytes(), StandardOpenOption.CREATE_NEW  , StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Date().getTime()/1000 + "Statistics{" +
                "tasksCount=" + tasksCount +
                ", eventsCount=" + eventsCount +
                ", inEventCount=" + inEventCount +
                ", rdHubCount=" + rdHubCount +
                ", wuCount=" + wuCount +
                ", nothingReadCount=" + nothingReadCount +
                ", closeCount=" + closeCount +
                ", registerServerCount=" + registerServerCount +
                ", registerClientCount=" + registerClientCount +
                ", addChannelCount=" + addChannelCount +
                ", removeChannelCount=" + removeChannelCount +
                ", removeChannelCount1=" + removeChannelCount1 +
                ", fragmentationCount=" + fragmentationCount +
                ", handlerCallCount=" + handlerCallCount +
                ", totalTime=" + totalTime +
                ", maxTime=" + maxTime +
                ", totalHandlerTime=" + totalHandlerTime +
                ", requestStatistics=" + requestStatistics +
                ", processTime=" + processTime +
                ", processReadTime=" + processReadTime +
                ", readTime=" + readTime +
                ", epollInReadyClientTime=" + epollInReadyClientTime +
                ", epollInReadyServerTime=" + epollInReadyServerTime +
                ", closeTime=" + closeTime +
                ", registrationTime=" + registrationTime +
                ", ioWriteTime=" + ioWriteTime +
                ", ioWriteMaxTime=" + ioWriteMaxTime +
                ", maxClearTime=" + maxClearTime +
                ", requestClearStatistics=" + requestClearStatistics +
                ", readInHandlerTime=" + readInHandlerTime +
                ", preHandleTime=" + preHandleTime +
                ", wakeUpCount=" + wakeUpCount +
                ", maxQueueSize=" + maxQueueSize +
                ", prepareResponseTime=" + prepareResponseTime +
                ", queueMore5=" + queueMore5 +
                ", queueMore10=" + queueMore10 +
                ", queueMore15=" + queueMore15 +
                ", queueMore20=" + queueMore20 +
                ", maxWriteCount=" + maxWriteCount +
                '}';
    }
}
