package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.util.Map;

public class Epoll0ChannelConfig  extends DefaultChannelConfig {
    final AbstractEpoll0Channel channel;

    Epoll0ChannelConfig(AbstractEpoll0Channel channel) {
        super(channel);
        this.channel = channel;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(super.getOptions(), EpollChannelOption.EPOLL_MODE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == EpollChannelOption.EPOLL_MODE) {
            return (T) getEpollMode();
        }
        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);
        if (option == EpollChannelOption.EPOLL_MODE) {
            setEpollMode((EpollMode) value);
        } else {
            return super.setOption(option, value);
        }
        return true;
    }

    @Override
    public Epoll0ChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        super.setConnectTimeoutMillis(connectTimeoutMillis);
        return this;
    }

    @Override
    @Deprecated
    public Epoll0ChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
        super.setMaxMessagesPerRead(maxMessagesPerRead);
        return this;
    }

    @Override
    public Epoll0ChannelConfig setWriteSpinCount(int writeSpinCount) {
        super.setWriteSpinCount(writeSpinCount);
        return this;
    }

    @Override
    public Epoll0ChannelConfig setAllocator(ByteBufAllocator allocator) {
        super.setAllocator(allocator);
        return this;
    }

    @Override
    public Epoll0ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
        if (!(allocator.newHandle() instanceof RecvByteBufAllocator.ExtendedHandle)) {
            throw new IllegalArgumentException("allocator.newHandle() must return an object of type: " +
                    RecvByteBufAllocator.ExtendedHandle.class);
        }
        super.setRecvByteBufAllocator(allocator);
        return this;
    }

    @Override
    public Epoll0ChannelConfig setAutoRead(boolean autoRead) {
        super.setAutoRead(autoRead);
        return this;
    }

    @Override
    @Deprecated
    public Epoll0ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
        return this;
    }

    @Override
    @Deprecated
    public Epoll0ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
        return this;
    }

    @Override
    public Epoll0ChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
        super.setWriteBufferWaterMark(writeBufferWaterMark);
        return this;
    }

    @Override
    public Epoll0ChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
        super.setMessageSizeEstimator(estimator);
        return this;
    }

    /**
     * Return the {@link EpollMode} used. Default is
     * {@link EpollMode#EDGE_TRIGGERED}. If you want to use {@link #isAutoRead()} {@code false} or
     * {@link #getMaxMessagesPerRead()} and have an accurate behaviour you should use
     * {@link EpollMode#LEVEL_TRIGGERED}.
     */
    public EpollMode getEpollMode() {
        return channel.isFlagSet(Native.EPOLLET)
                ? EpollMode.EDGE_TRIGGERED : EpollMode.LEVEL_TRIGGERED;
    }

    /**
     * Set the {@link EpollMode} used. Default is
     * {@link EpollMode#EDGE_TRIGGERED}. If you want to use {@link #isAutoRead()} {@code false} or
     * {@link #getMaxMessagesPerRead()} and have an accurate behaviour you should use
     * {@link EpollMode#LEVEL_TRIGGERED}.
     *
     * <strong>Be aware this config setting can only be adjusted before the channel was registered.</strong>
     */
    public Epoll0ChannelConfig setEpollMode(EpollMode mode) {
        if (mode == null) {
            throw new NullPointerException("mode");
        }
        try {
            switch (mode) {
                case EDGE_TRIGGERED:
                    checkChannelNotRegistered();
                    channel.setFlag(Native.EPOLLET);
                    break;
                case LEVEL_TRIGGERED:
                    checkChannelNotRegistered();
                    channel.clearFlag(Native.EPOLLET);
                    break;
                default:
                    throw new Error();
            }
        } catch (IOException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    private void checkChannelNotRegistered() {
        if (channel.isRegistered()) {
            throw new IllegalStateException("EpollMode can only be changed before channel is registered");
        }
    }

    @Override
    protected final void autoReadCleared() {
        channel.clearEpollIn();
    }
}
