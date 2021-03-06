package io.netty.channel.epoll;

import com.dgusev.hl.server.RequestHandler;
import com.dgusev.hl.server.stat.Statistics;
import com.dgusev.hl.server.threads.WorkerThread;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.IovArray;
import io.netty.channel.unix.SocketWritableByteChannel;
import io.netty.channel.unix.UnixChannelUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.Executor;

import static io.netty.channel.unix.FileDescriptor.pipe;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

abstract class AbstractEpoll0StreamChannel  extends AbstractEpoll0Channel implements DuplexChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ", " +
                    StringUtil.simpleClassName(DefaultFileRegion.class) + ')';
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractEpoll0StreamChannel.class);
    private static final ClosedChannelException CLEAR_SPLICE_QUEUE_CLOSED_CHANNEL_EXCEPTION =
            ThrowableUtil.unknownStackTrace(new ClosedChannelException(),
                    AbstractEpoll0StreamChannel.class, "clearSpliceQueue()");
    private static final ClosedChannelException SPLICE_TO_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(
            new ClosedChannelException(),
            AbstractEpoll0StreamChannel.class, "spliceTo(...)");
    private static final ClosedChannelException FAIL_SPLICE_IF_CLOSED_CLOSED_CHANNEL_EXCEPTION =
            ThrowableUtil.unknownStackTrace(new ClosedChannelException(),
                    AbstractEpoll0StreamChannel.class, "failSpliceIfClosed(...)");

    private Queue<AbstractEpoll0StreamChannel.SpliceInTask> spliceQueue;

    // Lazy init these if we need to splice(...)
    private FileDescriptor pipeIn;
    private FileDescriptor pipeOut;

    private WritableByteChannel byteChannel;

    protected AbstractEpoll0StreamChannel(Channel parent, int fd) {
        this(parent, new LinuxSocket(fd));
    }

    protected AbstractEpoll0StreamChannel(int fd) {
        this(new LinuxSocket(fd));
    }

    AbstractEpoll0StreamChannel(LinuxSocket fd) {
        this(fd, isSoErrorZero(fd));
    }

    AbstractEpoll0StreamChannel(Channel parent, LinuxSocket fd) {
        super(parent, fd, Native.EPOLLIN, true);
        // Add EPOLLRDHUP so we are notified once the remote peer close the connection.
        flags |= Native.EPOLLRDHUP;
    }

    AbstractEpoll0StreamChannel(Channel parent, LinuxSocket fd, SocketAddress remote) {
        super(parent, fd, Native.EPOLLIN, remote);
        // Add EPOLLRDHUP so we are notified once the remote peer close the connection.
        flags |= Native.EPOLLRDHUP;
    }

    protected AbstractEpoll0StreamChannel(LinuxSocket fd, boolean active) {
        super(null, fd, Native.EPOLLIN, active);
        // Add EPOLLRDHUP so we are notified once the remote peer close the connection.
        flags |= Native.EPOLLRDHUP;
    }

    @Override
    protected AbstractEpollUnsafe newUnsafe() {
        return new AbstractEpoll0StreamChannel.EpollStreamUnsafe();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    /**
     * Splice from this {@link AbstractEpoll0StreamChannel} to another {@link AbstractEpoll0StreamChannel}.
     * The {@code len} is the number of bytes to splice. If using {@link Integer#MAX_VALUE} it will
     * splice until the {@link ChannelFuture} was canceled or it was failed.
     *
     * Please note:
     * <ul>
     *   <li>both channels need to be registered to the same {@link EventLoop}, otherwise an
     *   {@link IllegalArgumentException} is thrown. </li>
     *   <li>{@link EpollChannelConfig#getEpollMode()} must be {@link EpollMode#LEVEL_TRIGGERED} for this and the
     *   target {@link AbstractEpoll0StreamChannel}</li>
     * </ul>
     *
     */
    public final ChannelFuture spliceTo(final AbstractEpoll0StreamChannel ch, final int len) {
        return spliceTo(ch, len, newPromise());
    }

    /**
     * Splice from this {@link AbstractEpoll0StreamChannel} to another {@link AbstractEpoll0StreamChannel}.
     * The {@code len} is the number of bytes to splice. If using {@link Integer#MAX_VALUE} it will
     * splice until the {@link ChannelFuture} was canceled or it was failed.
     *
     * Please note:
     * <ul>
     *   <li>both channels need to be registered to the same {@link EventLoop}, otherwise an
     *   {@link IllegalArgumentException} is thrown. </li>
     *   <li>{@link EpollChannelConfig#getEpollMode()} must be {@link EpollMode#LEVEL_TRIGGERED} for this and the
     *   target {@link AbstractEpoll0StreamChannel}</li>
     * </ul>
     *
     */
    public final ChannelFuture spliceTo(final AbstractEpoll0StreamChannel ch, final int len,
                                        final ChannelPromise promise) {
        if (ch.eventLoop() != eventLoop()) {
            throw new IllegalArgumentException("EventLoops are not the same.");
        }
        if (len < 0) {
            throw new IllegalArgumentException("len: " + len + " (expected: >= 0)");
        }
        if (ch.config().getEpollMode() != EpollMode.LEVEL_TRIGGERED
                || config().getEpollMode() != EpollMode.LEVEL_TRIGGERED) {
            throw new IllegalStateException("spliceTo() supported only when using " + EpollMode.LEVEL_TRIGGERED);
        }
        checkNotNull(promise, "promise");
        if (!isOpen()) {
            promise.tryFailure(SPLICE_TO_CLOSED_CHANNEL_EXCEPTION);
        } else {
            addToSpliceQueue(new AbstractEpoll0StreamChannel.SpliceInChannelTask(ch, len, promise));
            failSpliceIfClosed(promise);
        }
        return promise;
    }

    /**
     * Splice from this {@link AbstractEpoll0StreamChannel} to another {@link FileDescriptor}.
     * The {@code offset} is the offset for the {@link FileDescriptor} and {@code len} is the
     * number of bytes to splice. If using {@link Integer#MAX_VALUE} it will splice until the
     * {@link ChannelFuture} was canceled or it was failed.
     *
     * Please note:
     * <ul>
     *   <li>{@link EpollChannelConfig#getEpollMode()} must be {@link EpollMode#LEVEL_TRIGGERED} for this
     *   {@link AbstractEpoll0StreamChannel}</li>
     *   <li>the {@link FileDescriptor} will not be closed after the {@link ChannelFuture} is notified</li>
     *   <li>this channel must be registered to an event loop or {@link IllegalStateException} will be thrown.</li>
     * </ul>
     */
    public final ChannelFuture spliceTo(final FileDescriptor ch, final int offset, final int len) {
        return spliceTo(ch, offset, len, newPromise());
    }

    /**
     * Splice from this {@link AbstractEpoll0StreamChannel} to another {@link FileDescriptor}.
     * The {@code offset} is the offset for the {@link FileDescriptor} and {@code len} is the
     * number of bytes to splice. If using {@link Integer#MAX_VALUE} it will splice until the
     * {@link ChannelFuture} was canceled or it was failed.
     *
     * Please note:
     * <ul>
     *   <li>{@link EpollChannelConfig#getEpollMode()} must be {@link EpollMode#LEVEL_TRIGGERED} for this
     *   {@link AbstractEpoll0StreamChannel}</li>
     *   <li>the {@link FileDescriptor} will not be closed after the {@link ChannelPromise} is notified</li>
     *   <li>this channel must be registered to an event loop or {@link IllegalStateException} will be thrown.</li>
     * </ul>
     */
    public final ChannelFuture spliceTo(final FileDescriptor ch, final int offset, final int len,
                                        final ChannelPromise promise) {
        if (len < 0) {
            throw new IllegalArgumentException("len: " + len + " (expected: >= 0)");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0 but was " + offset);
        }
        if (config().getEpollMode() != EpollMode.LEVEL_TRIGGERED) {
            throw new IllegalStateException("spliceTo() supported only when using " + EpollMode.LEVEL_TRIGGERED);
        }
        checkNotNull(promise, "promise");
        if (!isOpen()) {
            promise.tryFailure(SPLICE_TO_CLOSED_CHANNEL_EXCEPTION);
        } else {
            addToSpliceQueue(new AbstractEpoll0StreamChannel.SpliceFdTask(ch, offset, len, promise));
            failSpliceIfClosed(promise);
        }
        return promise;
    }

    private void failSpliceIfClosed(ChannelPromise promise) {
        if (!isOpen()) {
            // Seems like the Channel was closed in the meantime try to fail the promise to prevent any
            // cases where a future may not be notified otherwise.
            if (promise.tryFailure(FAIL_SPLICE_IF_CLOSED_CLOSED_CHANNEL_EXCEPTION)) {
                eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        // Call this via the EventLoop as it is a MPSC queue.
                        clearSpliceQueue();
                    }
                });
            }
        }
    }

    /**
     * Write bytes form the given {@link ByteBuf} to the underlying {@link java.nio.channels.Channel}.
     * @param buf           the {@link ByteBuf} from which the bytes should be written
     */
    private boolean writeBytes(ChannelOutboundBuffer in, ByteBuf buf, int writeSpinCount) throws Exception {
        int readableBytes = buf.readableBytes();
        if (readableBytes == 0) {
            in.remove();
            return true;
        }

        if (buf.hasMemoryAddress() || buf.nioBufferCount() == 1) {
            int writtenBytes = doWriteBytes(buf);
            in.removeBytes(writtenBytes);
            return writtenBytes == readableBytes;
        } else {
            ByteBuffer[] nioBuffers = buf.nioBuffers();
            return writeBytesMultiple(in, nioBuffers, nioBuffers.length, readableBytes, writeSpinCount);
        }
    }

    private boolean writeBytesMultiple(
            ChannelOutboundBuffer in, IovArray array, int writeSpinCount) throws IOException {

        long expectedWrittenBytes = array.size();
        final long initialExpectedWrittenBytes = expectedWrittenBytes;

        int cnt = array.count();

        assert expectedWrittenBytes != 0;
        assert cnt != 0;

        boolean done = false;
        int offset = 0;
        int end = offset + cnt;
        for (int i = writeSpinCount; i > 0; --i) {
            long localWrittenBytes = socket.writevAddresses(array.memoryAddress(offset), cnt);
            if (localWrittenBytes == 0) {
                break;
            }
            expectedWrittenBytes -= localWrittenBytes;

            if (expectedWrittenBytes == 0) {
                // Written everything, just break out here (fast-path)
                done = true;
                break;
            }

            do {
                long bytes = array.processWritten(offset, localWrittenBytes);
                if (bytes == -1) {
                    // incomplete write
                    break;
                } else {
                    offset++;
                    cnt--;
                    localWrittenBytes -= bytes;
                }
            } while (offset < end && localWrittenBytes > 0);
        }
        in.removeBytes(initialExpectedWrittenBytes - expectedWrittenBytes);
        return done;
    }

    private boolean writeBytesMultiple(
            ChannelOutboundBuffer in, ByteBuffer[] nioBuffers,
            int nioBufferCnt, long expectedWrittenBytes, int writeSpinCount) throws IOException {

        assert expectedWrittenBytes != 0;
        final long initialExpectedWrittenBytes = expectedWrittenBytes;

        boolean done = false;
        int offset = 0;
        int end = offset + nioBufferCnt;
        for (int i = writeSpinCount; i > 0; --i) {
            long localWrittenBytes = socket.writev(nioBuffers, offset, nioBufferCnt);
            if (localWrittenBytes == 0) {
                break;
            }
            expectedWrittenBytes -= localWrittenBytes;

            if (expectedWrittenBytes == 0) {
                // Written everything, just break out here (fast-path)
                done = true;
                break;
            }
            do {
                ByteBuffer buffer = nioBuffers[offset];
                int pos = buffer.position();
                int bytes = buffer.limit() - pos;
                if (bytes > localWrittenBytes) {
                    buffer.position(pos + (int) localWrittenBytes);
                    // incomplete write
                    break;
                } else {
                    offset++;
                    nioBufferCnt--;
                    localWrittenBytes -= bytes;
                }
            } while (offset < end && localWrittenBytes > 0);
        }

        in.removeBytes(initialExpectedWrittenBytes - expectedWrittenBytes);
        return done;
    }

    /**
     * Write a {@link DefaultFileRegion}
     *
     * @param region        the {@link DefaultFileRegion} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    private boolean writeDefaultFileRegion(
            ChannelOutboundBuffer in, DefaultFileRegion region, int writeSpinCount) throws Exception {
        final long regionCount = region.count();
        if (region.transferred() >= regionCount) {
            in.remove();
            return true;
        }

        final long baseOffset = region.position();
        boolean done = false;
        long flushedAmount = 0;

        for (int i = writeSpinCount; i > 0; --i) {
            final long offset = region.transferred();
            final long localFlushedAmount =
                    Native.sendfile(socket.intValue(), region, baseOffset, offset, regionCount - offset);
            if (localFlushedAmount == 0) {
                break;
            }

            flushedAmount += localFlushedAmount;
            if (region.transferred() >= regionCount) {
                done = true;
                break;
            }
        }

        if (flushedAmount > 0) {
            in.progress(flushedAmount);
        }

        if (done) {
            in.remove();
        }
        return done;
    }

    private boolean writeFileRegion(
            ChannelOutboundBuffer in, FileRegion region, final int writeSpinCount) throws Exception {
        if (region.transferred() >= region.count()) {
            in.remove();
            return true;
        }

        boolean done = false;
        long flushedAmount = 0;

        if (byteChannel == null) {
            byteChannel = new AbstractEpoll0StreamChannel.EpollSocketWritableByteChannel();
        }
        for (int i = writeSpinCount; i > 0; --i) {
            final long localFlushedAmount = region.transferTo(byteChannel, region.transferred());
            if (localFlushedAmount == 0) {
                break;
            }

            flushedAmount += localFlushedAmount;
            if (region.transferred() >= region.count()) {
                done = true;
                break;
            }
        }

        if (flushedAmount > 0) {
            in.progress(flushedAmount);
        }

        if (done) {
            in.remove();
        }
        return done;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        int writeSpinCount = config().getWriteSpinCount();
        for (;;) {
            final int msgCount = in.size();

            if (msgCount == 0) {
                // Wrote all messages.
                clearFlag(Native.EPOLLOUT);
                // Return here so we not set the EPOLLOUT flag.
                return;
            }

            // Do gathering write if the outbounf buffer entries start with more than one ByteBuf.
            if (msgCount > 1 && in.current() instanceof ByteBuf) {
                if (!doWriteMultiple(in, writeSpinCount)) {
                    // Break the loop and so set EPOLLOUT flag.
                    break;
                }

                // We do not break the loop here even if the outbound buffer was flushed completely,
                // because a user might have triggered another write and flush when we notify his or her
                // listeners.
            } else { // msgCount == 1
                if (!doWriteSingle(in, writeSpinCount)) {
                    // Break the loop and so set EPOLLOUT flag.
                    break;
                }
            }
        }
        // Underlying descriptor can not accept all data currently, so set the EPOLLOUT flag to be woken up
        // when it can accept more data.
        setFlag(Native.EPOLLOUT);
    }

    protected boolean doWriteSingle(ChannelOutboundBuffer in, int writeSpinCount) throws Exception {
        // The outbound buffer contains only one message or it contains a file region.
        Object msg = in.current();
        if (msg instanceof ByteBuf) {
            if (!writeBytes(in, (ByteBuf) msg, writeSpinCount)) {
                // was not able to write everything so break here we will get notified later again once
                // the network stack can handle more writes.
                return false;
            }
        } else if (msg instanceof DefaultFileRegion) {
            if (!writeDefaultFileRegion(in, (DefaultFileRegion) msg, writeSpinCount)) {
                // was not able to write everything so break here we will get notified later again once
                // the network stack can handle more writes.
                return false;
            }
        } else if (msg instanceof FileRegion) {
            if (!writeFileRegion(in, (FileRegion) msg, writeSpinCount)) {
                // was not able to write everything so break here we will get notified later again once
                // the network stack can handle more writes.
                return false;
            }
        } else if (msg instanceof AbstractEpoll0StreamChannel.SpliceOutTask) {
            if (!((AbstractEpoll0StreamChannel.SpliceOutTask) msg).spliceOut()) {
                return false;
            }
            in.remove();
        } else {
            // Should never reach here.
            throw new Error();
        }

        return true;
    }

    private boolean doWriteMultiple(ChannelOutboundBuffer in, int writeSpinCount) throws Exception {
        if (PlatformDependent.hasUnsafe()) {
            // this means we can cast to IovArray and write the IovArray directly.
            IovArray array = ((EpollEventLoop) eventLoop()).cleanArray();
            in.forEachFlushedMessage(array);

            int cnt = array.count();
            if (cnt >= 1) {
                // TODO: Handle the case where cnt == 1 specially.
                if (!writeBytesMultiple(in, array, writeSpinCount)) {
                    // was not able to write everything so break here we will get notified later again once
                    // the network stack can handle more writes.
                    return false;
                }
            } else { // cnt == 0, which means the outbound buffer contained empty buffers only.
                in.removeBytes(0);
            }
        } else {
            ByteBuffer[] buffers = in.nioBuffers();
            int cnt = in.nioBufferCount();
            if (cnt >= 1) {
                // TODO: Handle the case where cnt == 1 specially.
                if (!writeBytesMultiple(in, buffers, cnt, in.nioBufferSize(), writeSpinCount)) {
                    // was not able to write everything so break here we will get notified later again once
                    // the network stack can handle more writes.
                    return false;
                }
            } else { // cnt == 0, which means the outbound buffer contained empty buffers only.
                in.removeBytes(0);
            }
        }

        return true;
    }

    @Override
    protected Object filterOutboundMessage(Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            return UnixChannelUtil.isBufferCopyNeededForWrite(buf)? newDirectBuffer(buf): buf;
        }

        if (msg instanceof FileRegion || msg instanceof AbstractEpoll0StreamChannel.SpliceOutTask) {
            return msg;
        }

        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    private void shutdownOutput0(final ChannelPromise promise) {
        try {
            socket.shutdown(false, true);
            ((AbstractUnsafe) unsafe()).shutdownOutput();
            promise.setSuccess();
        } catch (Throwable cause) {
            promise.setFailure(cause);
        }
    }

    private void shutdownInput0(final ChannelPromise promise) {
        try {
            socket.shutdown(true, false);
            promise.setSuccess();
        } catch (Throwable cause) {
            promise.setFailure(cause);
        }
    }

    private void shutdown0(final ChannelPromise promise) {
        try {
            socket.shutdown(true, true);
            ((AbstractUnsafe) unsafe()).shutdownOutput();
            promise.setSuccess();
        } catch (Throwable cause) {
            promise.setFailure(cause);
        }
    }

    @Override
    public boolean isOutputShutdown() {
        return socket.isOutputShutdown();
    }

    @Override
    public boolean isInputShutdown() {
        return socket.isInputShutdown();
    }

    @Override
    public boolean isShutdown() {
        return socket.isShutdown();
    }

    @Override
    public ChannelFuture shutdownOutput() {
        return shutdownOutput(newPromise());
    }

    @Override
    public ChannelFuture shutdownOutput(final ChannelPromise promise) {
        Executor closeExecutor = ((AbstractEpoll0StreamChannel.EpollStreamUnsafe) unsafe()).prepareToClose();
        if (closeExecutor != null) {
            closeExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    shutdownOutput0(promise);
                }
            });
        } else {
            EventLoop loop = eventLoop();
            if (loop.inEventLoop()) {
                shutdownOutput0(promise);
            } else {
                loop.execute(new Runnable() {
                    @Override
                    public void run() {
                        shutdownOutput0(promise);
                    }
                });
            }
        }
        return promise;
    }

    @Override
    public ChannelFuture shutdownInput() {
        return shutdownInput(newPromise());
    }

    @Override
    public ChannelFuture shutdownInput(final ChannelPromise promise) {
        Executor closeExecutor = ((AbstractEpoll0StreamChannel.EpollStreamUnsafe) unsafe()).prepareToClose();
        if (closeExecutor != null) {
            closeExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    shutdownInput0(promise);
                }
            });
        } else {
            EventLoop loop = eventLoop();
            if (loop.inEventLoop()) {
                shutdownInput0(promise);
            } else {
                loop.execute(new Runnable() {
                    @Override
                    public void run() {
                        shutdownInput0(promise);
                    }
                });
            }
        }
        return promise;
    }

    @Override
    public ChannelFuture shutdown() {
        return shutdown(newPromise());
    }

    @Override
    public ChannelFuture shutdown(final ChannelPromise promise) {
        Executor closeExecutor = ((AbstractEpoll0StreamChannel.EpollStreamUnsafe) unsafe()).prepareToClose();
        if (closeExecutor != null) {
            closeExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    shutdown0(promise);
                }
            });
        } else {
            EventLoop loop = eventLoop();
            if (loop.inEventLoop()) {
                shutdown0(promise);
            } else {
                loop.execute(new Runnable() {
                    @Override
                    public void run() {
                        shutdown0(promise);
                    }
                });
            }
        }
        return promise;
    }

    @Override
    protected void doClose() throws Exception {
        try {
            // Calling super.doClose() first so spliceTo(...) will fail on next call.
            super.doClose();
        } finally {
            safeClosePipe(pipeIn);
            safeClosePipe(pipeOut);
            clearSpliceQueue();
        }
    }

    private void clearSpliceQueue() {
        if (spliceQueue == null) {
            return;
        }
        for (;;) {
            AbstractEpoll0StreamChannel.SpliceInTask task = spliceQueue.poll();
            if (task == null) {
                break;
            }
            task.promise.tryFailure(CLEAR_SPLICE_QUEUE_CLOSED_CHANNEL_EXCEPTION);
        }
    }

    private static void safeClosePipe(FileDescriptor fd) {
        if (fd != null) {
            try {
                fd.close();
            } catch (IOException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Error while closing a pipe", e);
                }
            }
        }
    }

    class EpollStreamUnsafe extends AbstractEpollUnsafe {
        // Overridden here just to be able to access this method from AbstractEpoll0StreamChannel
        @Override
        protected Executor prepareToClose() {
            return super.prepareToClose();
        }

        private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close,
                                         EpollRecvByteAllocatorHandle allocHandle) {
            if (byteBuf != null) {
                if (byteBuf.isReadable()) {
                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                } else {
                    byteBuf.release();
                }
            }
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();
            pipeline.fireExceptionCaught(cause);
            if (close || cause instanceof IOException) {
                shutdownInput(false);
            }
        }

        @Override
        EpollRecvByteAllocatorHandle newEpollHandle(RecvByteBufAllocator.ExtendedHandle handle) {
            return new EpollRecvByteAllocatorStreamingHandle(handle);
        }

        @Override
        void epollInReady() {
            try {
                final ChannelPipeline pipeline = pipeline();
                boolean close = false;
                ByteBuf byteBuf = ((WorkerThread) Thread.currentThread()).READ_BUFFER;
                byteBuf.clear();
                int readBytes = doReadBytes(byteBuf);
                if (readBytes <= 0) {
                    close = readBytes < 0;
                } else {
                    ChannelHandlerContext context = pipeline.firstContext();
                    ((ChannelInboundHandler) (pipeline.first())).channelRead(context, byteBuf);
                }
                if (close) {
                    shutdownInput(false);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        }
    }

    private void addToSpliceQueue(final AbstractEpoll0StreamChannel.SpliceInTask task) {
        EventLoop eventLoop = eventLoop();
        if (eventLoop.inEventLoop()) {
            addToSpliceQueue0(task);
        } else {
            eventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    addToSpliceQueue0(task);
                }
            });
        }
    }

    private void addToSpliceQueue0(AbstractEpoll0StreamChannel.SpliceInTask task) {
        if (spliceQueue == null) {
            spliceQueue = PlatformDependent.newMpscQueue();
        }
        spliceQueue.add(task);
    }

    protected abstract class SpliceInTask {
        final ChannelPromise promise;
        int len;

        protected SpliceInTask(int len, ChannelPromise promise) {
            this.promise = promise;
            this.len = len;
        }

        abstract boolean spliceIn(RecvByteBufAllocator.Handle handle);

        protected final int spliceIn(FileDescriptor pipeOut, RecvByteBufAllocator.Handle handle) throws IOException {
            // calculate the maximum amount of data we are allowed to splice
            int length = Math.min(handle.guess(), len);
            int splicedIn = 0;
            for (;;) {
                // Splicing until there is nothing left to splice.
                int localSplicedIn = Native.splice(socket.intValue(), -1, pipeOut.intValue(), -1, length);
                if (localSplicedIn == 0) {
                    break;
                }
                splicedIn += localSplicedIn;
                length -= localSplicedIn;
            }

            return splicedIn;
        }
    }

    // Let it directly implement channelFutureListener as well to reduce object creation.
    private final class SpliceInChannelTask extends AbstractEpoll0StreamChannel.SpliceInTask implements ChannelFutureListener {
        private final AbstractEpoll0StreamChannel ch;

        SpliceInChannelTask(AbstractEpoll0StreamChannel ch, int len, ChannelPromise promise) {
            super(len, promise);
            this.ch = ch;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                promise.setFailure(future.cause());
            }
        }

        @Override
        public boolean spliceIn(RecvByteBufAllocator.Handle handle) {
            assert ch.eventLoop().inEventLoop();
            if (len == 0) {
                promise.setSuccess();
                return true;
            }
            try {
                // We create the pipe on the target channel as this will allow us to just handle pending writes
                // later in a correct fashion without get into any ordering issues when spliceTo(...) is called
                // on multiple Channels pointing to one target Channel.
                FileDescriptor pipeOut = ch.pipeOut;
                if (pipeOut == null) {
                    // Create a new pipe as non was created before.
                    FileDescriptor[] pipe = pipe();
                    ch.pipeIn = pipe[0];
                    pipeOut = ch.pipeOut = pipe[1];
                }

                int splicedIn = spliceIn(pipeOut, handle);
                if (splicedIn > 0) {
                    // Integer.MAX_VALUE is a special value which will result in splice forever.
                    if (len != Integer.MAX_VALUE) {
                        len -= splicedIn;
                    }

                    // Depending on if we are done with splicing inbound data we set the right promise for the
                    // outbound splicing.
                    final ChannelPromise splicePromise;
                    if (len == 0) {
                        splicePromise = promise;
                    } else {
                        splicePromise = ch.newPromise().addListener(this);
                    }

                    boolean autoRead = config().isAutoRead();

                    // Just call unsafe().write(...) and flush() as we not want to traverse the whole pipeline for this
                    // case.
                    ch.unsafe().write(new AbstractEpoll0StreamChannel.SpliceOutTask(ch, splicedIn, autoRead), splicePromise);
                    ch.unsafe().flush();
                    if (autoRead && !splicePromise.isDone()) {
                        // Write was not done which means the target channel was not writable. In this case we need to
                        // disable reading until we are done with splicing to the target channel because:
                        //
                        // - The user may want to to trigger another splice operation once the splicing was complete.
                        config().setAutoRead(false);
                    }
                }

                return len == 0;
            } catch (Throwable cause) {
                promise.setFailure(cause);
                return true;
            }
        }
    }

    private final class SpliceOutTask {
        private final AbstractEpoll0StreamChannel ch;
        private final boolean autoRead;
        private int len;

        SpliceOutTask(AbstractEpoll0StreamChannel ch, int len, boolean autoRead) {
            this.ch = ch;
            this.len = len;
            this.autoRead = autoRead;
        }

        public boolean spliceOut() throws Exception {
            assert ch.eventLoop().inEventLoop();
            try {
                int splicedOut = Native.splice(ch.pipeIn.intValue(), -1, ch.socket.intValue(), -1, len);
                len -= splicedOut;
                if (len == 0) {
                    if (autoRead) {
                        // AutoRead was used and we spliced everything so start reading again
                        config().setAutoRead(true);
                    }
                    return true;
                }
                return false;
            } catch (IOException e) {
                if (autoRead) {
                    // AutoRead was used and we spliced everything so start reading again
                    config().setAutoRead(true);
                }
                throw e;
            }
        }
    }

    private final class SpliceFdTask extends AbstractEpoll0StreamChannel.SpliceInTask {
        private final FileDescriptor fd;
        private final ChannelPromise promise;
        private final int offset;

        SpliceFdTask(FileDescriptor fd, int offset, int len, ChannelPromise promise) {
            super(len, promise);
            this.fd = fd;
            this.promise = promise;
            this.offset = offset;
        }

        @Override
        public boolean spliceIn(RecvByteBufAllocator.Handle handle) {
            assert eventLoop().inEventLoop();
            if (len == 0) {
                promise.setSuccess();
                return true;
            }

            try {
                FileDescriptor[] pipe = pipe();
                FileDescriptor pipeIn = pipe[0];
                FileDescriptor pipeOut = pipe[1];
                try {
                    int splicedIn = spliceIn(pipeOut, handle);
                    if (splicedIn > 0) {
                        // Integer.MAX_VALUE is a special value which will result in splice forever.
                        if (len != Integer.MAX_VALUE) {
                            len -= splicedIn;
                        }
                        do {
                            int splicedOut = Native.splice(pipeIn.intValue(), -1, fd.intValue(), offset, splicedIn);
                            splicedIn -= splicedOut;
                        } while (splicedIn > 0);
                        if (len == 0) {
                            promise.setSuccess();
                            return true;
                        }
                    }
                    return false;
                } finally {
                    safeClosePipe(pipeIn);
                    safeClosePipe(pipeOut);
                }
            } catch (Throwable cause) {
                promise.setFailure(cause);
                return true;
            }
        }
    }

    private final class EpollSocketWritableByteChannel extends SocketWritableByteChannel {
        EpollSocketWritableByteChannel() {
            super(socket);
        }

        @Override
        protected ByteBufAllocator alloc() {
            return AbstractEpoll0StreamChannel.this.alloc();
        }
    }

}