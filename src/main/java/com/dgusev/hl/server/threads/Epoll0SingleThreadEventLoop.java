package com.dgusev.hl.server.threads;

import io.netty.channel.*;
import io.netty.channel.epoll.AbstractEpoll0Channel;
import io.netty.channel.epoll.Epoll0EventLoop;
import io.netty.channel.epoll.Epoll0ServerSocketChannel;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.UnstableApi;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Created by dgusev on 24.09.2017.
 */
public abstract class Epoll0SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {

    protected static final int DEFAULT_MAX_PENDING_TASKS = Math.max(16,
            SystemPropertyUtil.getInt("io.netty.eventLoop.maxPendingTasks", Integer.MAX_VALUE));

    private final Queue<Runnable> tailTasks;

    private boolean started;

    protected Epoll0SingleThreadEventLoop( EventLoopGroup parent, Executor executor,
                                    boolean addTaskWakesUp, int maxPendingTasks,
                                    RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, addTaskWakesUp, maxPendingTasks, rejectedExecutionHandler);
        tailTasks = newTaskQueue(maxPendingTasks);
    }

    @Override
    public EventLoopGroup parent() {
        return (EventLoopGroup) super.parent();
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        if (channel instanceof Epoll0ServerSocketChannel) {
            return register(new DefaultChannelPromise(channel, this));
        } else {
            try {
                registerEventLoop(channel, this);
                setRegister(channel);
                executePipelineInit(channel.pipeline());
                /*((Epoll0EventLoop)this).add((AbstractEpoll0Channel) channel);
                if (!started) {
                    started = true;
                    this.execute(() -> {
                    });
                }*/
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new DefaultChannelPromise(channel, this);
        }
    }

    private static Field pendingHandlerCallbackHeadField;
    private static Field isRegisteredField;
    private static Field eventLoopField;
    private static Method callHandlerAddedForAllHandlers;
    private static Method register0;
    static {
        try {
            pendingHandlerCallbackHeadField = DefaultChannelPipeline.class.getDeclaredField("pendingHandlerCallbackHead");
            pendingHandlerCallbackHeadField.setAccessible(true);
            isRegisteredField = AbstractChannel.class.getDeclaredField("registered");
            isRegisteredField.setAccessible(true);
            eventLoopField = AbstractChannel.class.getDeclaredField("eventLoop");
            eventLoopField.setAccessible(true);
            callHandlerAddedForAllHandlers = DefaultChannelPipeline.class.getDeclaredMethod("callHandlerAddedForAllHandlers");
            callHandlerAddedForAllHandlers.setAccessible(true);
            eventLoopField.setAccessible(true);
            register0 = Class.forName("io.netty.channel.AbstractChannel$AbstractUnsafe").getDeclaredMethod("register0", ChannelPromise.class);
            register0.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setRegister(Channel channel) throws IllegalAccessException {
        isRegisteredField.set(channel, true);
    }

    private void registerEventLoop(Channel channel, EventLoop eventLoop) throws IllegalAccessException {
        eventLoopField.set(channel, eventLoop);
    }

    private void executePipelineInit(ChannelPipeline pipeline) throws IllegalAccessException {
        ((Runnable)pendingHandlerCallbackHeadField.get(pipeline)).run();
    }

    @Override
    public ChannelFuture register(final ChannelPromise promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        promise.channel().unsafe().register(this, promise);
        return promise;
    }

    @Deprecated
    @Override
    public ChannelFuture register(final Channel channel, final ChannelPromise promise) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (promise == null) {
            throw new NullPointerException("promise");
        }

        channel.unsafe().register(this, promise);
        return promise;
    }

    /**
     * Adds a task to be run once at the end of next (or current) {@code eventloop} iteration.
     *
     * @param task to be added.
     */
    @UnstableApi
    public final void executeAfterEventLoopIteration(Runnable task) {
        ObjectUtil.checkNotNull(task, "task");
        if (isShutdown()) {
            reject();
        }
        if (!tailTasks.offer(task)) {
            reject(task);
        }

        if (wakesUpForTask(task)) {
            wakeup(inEventLoop());
        }
    }

    /**
     * Removes a task that was added previously via {@link #executeAfterEventLoopIteration(Runnable)}.
     *
     * @param task to be removed.
     *
     * @return {@code true} if the task was removed as a result of this call.
     */
    @UnstableApi
    final boolean removeAfterEventLoopIterationTask(Runnable task) {
        return tailTasks.remove(ObjectUtil.checkNotNull(task, "task"));
    }

    @Override
    protected boolean wakesUpForTask(Runnable task) {
        return !(task instanceof Epoll0SingleThreadEventLoop.NonWakeupRunnable);
    }

    @Override
    protected void afterRunningAllTasks() {
        runAllTasksFrom(tailTasks);
    }

    @Override
    protected boolean hasTasks() {
        return super.hasTasks();
    }

    @Override
    public int pendingTasks() {
        return super.pendingTasks() + tailTasks.size();
    }

    /**
     * Marker interface for {@link Runnable} that will not trigger an {@link #wakeup(boolean)} in all cases.
     */
    interface NonWakeupRunnable extends Runnable { }

    @Override
    public boolean inEventLoop() {
            return super.inEventLoop();
    }
}
