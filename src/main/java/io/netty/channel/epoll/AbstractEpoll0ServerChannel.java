package io.netty.channel.epoll;

import com.dgusev.hl.server.stat.Statistics;
import io.netty.channel.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

abstract class AbstractEpoll0ServerChannel  extends AbstractEpoll0Channel implements ServerChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);

    protected AbstractEpoll0ServerChannel(int fd) {
        this(new LinuxSocket(fd), false);
    }

    AbstractEpoll0ServerChannel(LinuxSocket fd) {
        this(fd, isSoErrorZero(fd));
    }

    AbstractEpoll0ServerChannel(LinuxSocket fd, boolean active) {
        super(null, fd, Native.EPOLLIN, active);
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof EpollEventLoop;
    }

    @Override
    protected InetSocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected AbstractEpollUnsafe newUnsafe() {
        return new AbstractEpoll0ServerChannel.EpollServerSocketUnsafe();
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object filterOutboundMessage(Object msg) throws Exception {
        throw new UnsupportedOperationException();
    }

    abstract Channel newChildChannel(int fd, byte[] remote, int offset, int len) throws Exception;

    final class EpollServerSocketUnsafe extends AbstractEpollUnsafe {
        // Will hold the remote address after accept(...) was successful.
        // We need 24 bytes for the address as maximum + 1 byte for storing the length.
        // So use 26 bytes as it's a power of two.
        private final byte[] acceptedAddress = new byte[26];

        @Override
        public void connect(SocketAddress socketAddress, SocketAddress socketAddress2, ChannelPromise channelPromise) {
            // Connect not supported by ServerChannel implementations
            channelPromise.setFailure(new UnsupportedOperationException());
        }

        @Override
        void epollInReady() {
            assert eventLoop().inEventLoop();
            final ChannelConfig config = config();
            if (shouldBreakEpollInReady(config)) {
                clearEpollIn0();
                return;
            }
            final EpollRecvByteAllocatorHandle allocHandle = recvBufAllocHandle();
            allocHandle.edgeTriggered(isFlagSet(Native.EPOLLET));

            final ChannelPipeline pipeline = pipeline();
            allocHandle.reset(config);
            allocHandle.attemptedBytesRead(1);
            epollInBefore();

            Throwable exception = null;
            try {
                try {
                    do {
                        // lastBytesRead represents the fd. We use lastBytesRead because it must be set so that the
                        // EpollRecvByteAllocatorHandle knows if it should try to read again or not when autoRead is
                        // enabled.
                        allocHandle.lastBytesRead(socket.accept(acceptedAddress));
                        if (allocHandle.lastBytesRead() == -1) {
                            // this means everything was handled for now
                            break;
                        }
                        allocHandle.incMessagesRead(1);

                        readPending = false;
                        pipeline.fireChannelRead(newChildChannel(allocHandle.lastBytesRead(), acceptedAddress, 1,
                                acceptedAddress[0]));
                    } while (allocHandle.continueReading());
                } catch (Throwable t) {
                    exception = t;
                }
                allocHandle.readComplete();
                pipeline.fireChannelReadComplete();

                if (exception != null) {
                    pipeline.fireExceptionCaught(exception);
                }
            } finally {
                epollInFinally(config);
            }
        }
    }

    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }
}