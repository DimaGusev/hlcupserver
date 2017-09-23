package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;

import static io.netty.channel.epoll.LinuxSocket.newSocketStream;

public class Epoll0SocketChannel  extends AbstractEpoll0StreamChannel implements SocketChannel {

    private final Epoll0SocketChannelConfig config;

    private volatile Collection<InetAddress> tcpMd5SigAddresses = Collections.emptyList();

    public Epoll0SocketChannel() {
        super(newSocketStream(), false);
        config = new Epoll0SocketChannelConfig(this);
    }

    public Epoll0SocketChannel(int fd) {
        super(fd);
        config = new Epoll0SocketChannelConfig(this);
    }

    Epoll0SocketChannel(LinuxSocket fd, boolean active) {
        super(fd, active);
        config = new Epoll0SocketChannelConfig(this);
    }

    Epoll0SocketChannel(Channel parent, LinuxSocket fd, InetSocketAddress remoteAddress) {
        super(parent, fd, remoteAddress);
        config = new Epoll0SocketChannelConfig(this);

        if (parent instanceof EpollServerSocketChannel) {
            tcpMd5SigAddresses = ((EpollServerSocketChannel) parent).tcpMd5SigAddresses();
        }
    }

    /**
     * Returns the {@code TCP_INFO} for the current socket. See <a href="http://linux.die.net/man/7/tcp">man 7 tcp</a>.
     */
    public EpollTcpInfo tcpInfo() {
        return tcpInfo(new EpollTcpInfo());
    }

    /**
     * Updates and returns the {@code TCP_INFO} for the current socket.
     * See <a href="http://linux.die.net/man/7/tcp">man 7 tcp</a>.
     */
    public EpollTcpInfo tcpInfo(EpollTcpInfo info) {
        try {
            socket.getTcpInfo(info);
            return info;
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) super.remoteAddress();
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public Epoll0SocketChannelConfig config() {
        return config;
    }

    @Override
    public ServerSocketChannel parent() {
        return (ServerSocketChannel) super.parent();
    }

    @Override
    protected AbstractEpollUnsafe newUnsafe() {
        return new Epoll0SocketChannel.EpollSocketChannelUnsafe();
    }

    private final class EpollSocketChannelUnsafe extends EpollStreamUnsafe {
        @Override
        protected Executor prepareToClose() {
            try {
                // Check isOpen() first as otherwise it will throw a RuntimeException
                // when call getSoLinger() as the fd is not valid anymore.
                if (isOpen() && config().getSoLinger() > 0) {
                    // We need to cancel this key of the channel so we may not end up in a eventloop spin
                    // because we try to read or write until the actual close happens which may be later due
                    // SO_LINGER handling.
                    // See https://github.com/netty/netty/issues/4449
                    ((Epoll0EventLoop) eventLoop()).remove(Epoll0SocketChannel.this);
                    return GlobalEventExecutor.INSTANCE;
                }
            } catch (Throwable ignore) {
                // Ignore the error as the underlying channel may be closed in the meantime and so
                // getSoLinger() may produce an exception. In this case we just return null.
                // See https://github.com/netty/netty/issues/4449
            }
            return null;
        }
    }

    void setTcpMd5Sig(Map<InetAddress, byte[]> keys) throws IOException {
        tcpMd5SigAddresses = TcpMd5Util2.newTcpMd5Sigs(this, tcpMd5SigAddresses, keys);
    }

}
