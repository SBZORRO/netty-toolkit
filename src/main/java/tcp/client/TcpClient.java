package tcp.client;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sbzorro.LogUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TcpClient implements Closeable {
  public static final Map<String, TcpClient> map = new ConcurrentHashMap<>();

  public ChannelFuture connect(String host, int port) {
    remoteAddress(host, port);
    return connect();
  }

  public ChannelFuture connect() {

    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(eventLoopGroup());
    bootstrap.channel(channelClass());
    bootstrap.remoteAddress(remoteAddress);
    bootstrap.handler(init());
    future = bootstrap.connect().addListeners(listeners);
    return future;
  }

  public ChannelFuture writeAndFlush(Object message) {
    return channel().writeAndFlush(message);
  }

  public EventLoop executer() {
    return future().channel().eventLoop().next();
  }

  public Channel channel() {
    return future().channel();
  }

  private ChannelFuture future;

  public ChannelFuture future() {
    return future;
  }

  private ChannelFutureListener[] listeners;

  public void listeners(ChannelFutureListener... listeners) {
    this.listeners = listeners;
  }

  public ChannelFutureListener[] listeners() {
    return listeners;
  }

  public void addListener(ChannelFutureListener listener) {
    future().addListener(listener);
  }

  private SocketAddress remoteAddress;

  public void remoteAddress(String host, int port) {
    remoteAddress = InetSocketAddress.createUnresolved(host, port);
  }

  private Class<? extends Channel> channelClass;

  public Class<? extends Channel> channelClass() {
    if (channelClass == null) {
      return NioSocketChannel.class;
    }
    return channelClass;
  }

  private EventLoopGroup eventLoopGroup;

  public EventLoopGroup eventLoopGroup() {
    if (this.eventLoopGroup == null) {
      this.eventLoopGroup = new NioEventLoopGroup(1);
    }
    return eventLoopGroup;
  }

  public EventLoopGroup eventLoopGroup(int i) {
    if (this.eventLoopGroup == null) {
      this.eventLoopGroup = new NioEventLoopGroup(i);
    }
    return eventLoopGroup;
  }

  public void eventLoopGroup(EventLoopGroup eventLoopGroup) {
    this.eventLoopGroup = eventLoopGroup;
  }

  public Class<? extends Channel> getChannelClass() {
    return channelClass;
  }

  public void setChannelClass(Class<? extends Channel> channelClass) {
    this.channelClass = channelClass;
  }

  private ChannelHandler init;

  public ChannelHandler init() {
    assert init == null;
    return init;
  }

  public void init(ChannelHandler init) {
    this.init = init;
  }

  @Override
  public void close() {
//    try {
////      future().channel().closeFuture().sync();
//    } catch (InterruptedException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } finally {
    eventLoopGroup().shutdownGracefully();
//    bossGroup.shutdownGracefully();
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, "solong");
//    }
  }
}
