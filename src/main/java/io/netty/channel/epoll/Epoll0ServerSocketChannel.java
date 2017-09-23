package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static io.netty.channel.epoll.LinuxSocket.newSocketStream;
import static io.netty.channel.unix.NativeInetAddress.address;

public class Epoll0ServerSocketChannel  extends AbstractEpoll0ServerChannel implements ServerSocketChannel {

    private final Epoll0ServerSocketChannelConfig config;
    private volatile Collection<InetAddress> tcpMd5SigAddresses = Collections.emptyList();

    public Epoll0ServerSocketChannel() {
        super(newSocketStream(), false);
        config = new Epoll0ServerSocketChannelConfig(this);
    }

    public Epoll0ServerSocketChannel(int fd) {
        // Must call this constructor to ensure this object's local address is configured correctly.
        // The local address can only be obtained from a Socket object.
        this(new LinuxSocket(fd));
    }

    Epoll0ServerSocketChannel(LinuxSocket fd) {
        super(fd);
        config = new Epoll0ServerSocketChannelConfig(this);
    }

    Epoll0ServerSocketChannel(LinuxSocket fd, boolean active) {
        super(fd, active);
        config = new Epoll0ServerSocketChannelConfig(this);
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof Epoll0EventLoop;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        super.doBind(localAddress);
        if (Native.IS_SUPPORTING_TCP_FASTOPEN && config.getTcpFastopen() > 0) {
            socket.setTcpFastOpen(config.getTcpFastopen());
        }
        socket.listen(config.getBacklog());
        active = true;
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
    public Epoll0ServerSocketChannelConfig config() {
        return config;
    }

    @Override
    protected Channel newChildChannel(int fd, byte[] address, int offset, int len) throws Exception {
        return new Epoll0SocketChannel(this, new LinuxSocket(fd), address(address, offset, len));
    }

    Collection<InetAddress> tcpMd5SigAddresses() {
        return tcpMd5SigAddresses;
    }

    void setTcpMd5Sig(Map<InetAddress, byte[]> keys) throws IOException {
        tcpMd5SigAddresses = TcpMd5Util2.newTcpMd5Sigs(this, tcpMd5SigAddresses, keys);
    }
}
