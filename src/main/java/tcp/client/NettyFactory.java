package tcp.client;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;
import com.sbzorro.PropUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;

public abstract class NettyFactory implements Closeable {
  public static final Map<String, NettyFactory> CLIENTS = new ConcurrentHashMap<>();

  public static final ReentrantReadWriteLock CLIENTS_LOCK = new ReentrantReadWriteLock();
  public static final WriteLock CLIENTS_WRITE_LOCK = CLIENTS_LOCK.writeLock();

  public static final Map<String, String> LAST_CMD = new ConcurrentHashMap<>();
  public static final Map<String, String> LAST_RESP = new ConcurrentHashMap<>();

  public static final ScheduledExecutorService EXE = Executors
      .newSingleThreadScheduledExecutor();

  private AtomicInteger session = new AtomicInteger(0);

  public abstract ChannelFuture bootstrap();

  public abstract Class<? extends Channel> channelClass();

  public static NettyFactory
      bootstrap(
          NettyFactory client, ChannelInitializer<?> init,
          ChannelFutureListener... listeners)
          throws InterruptedException {
    client = client.cacheIn();
    if (client.isOk()) {
      return client;
    }
    synchronized (client) {
      if (client.isOk()) {
        return client;
      }
      client.init(init);
      client.listeners(listeners);
      client.bootstrap().sync();
    }
    return client;
  }

  public void sendHexOrStr(String msg)
      throws InterruptedException {
    String host = this.host();
    NettyFactory.LAST_CMD.put(host, msg);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, host + " <<< " + msg);

    if (HexByteUtil.isHex(msg)) {
      this.writeAndFlush(
          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg))).sync();
    } else {
      this.writeAndFlush(Unpooled.copiedBuffer(msg.getBytes())).sync();
    }
  }

  public void sendHexOrStrUdp(String msg)
      throws InterruptedException {
    NettyFactory.LAST_CMD.put(this.host(), msg);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, this.host() + " <<< " + msg);

    if (HexByteUtil.isHex(msg)) {
      this.writeAndFlush(new DatagramPacket(
          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg)),
          new InetSocketAddress(this.ip(), this.port())))
          .sync();
    } else {
      this.writeAndFlush(new DatagramPacket(
          Unpooled.copiedBuffer(msg.getBytes()),
          new InetSocketAddress(this.ip(), this.port())))
          .sync();
    }
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

  public void removeHandler(String name) {
    try {
      future().channel().pipeline().remove(name);
    } catch (Exception e) {
      // ignore
    }
  }

  public void addHandler(String name, ChannelHandler handler) {
    try {
      future().channel().pipeline().addLast(name, handler);
    } catch (Exception e) {
      future().channel().pipeline().remove(name);
      future().channel().pipeline().addLast(name, handler);
    }
  }

  protected ChannelFuture future;

  public ChannelFuture future() {
    return future;
  }

  protected ChannelFutureListener[] listeners;

  public void listeners(ChannelFutureListener... listeners) {
    this.listeners = listeners;
  }

  public ChannelFutureListener[] listeners() {
    return listeners;
  }

  public void addListener(ChannelFutureListener listener) {
    future().addListener(listener);
  }

  protected String ip = "127.0.0.1";
  protected int port = 0;

  public String host() {
    return ip + ":" + port;
  }

  public String ip() {
    return ip;
  }

  public int port() {
    return port;
  }

  protected SocketAddress address;

  public void address(String host, int port) {
    address = InetSocketAddress.createUnresolved(host, port);
  }

  public void address(int port) {
    address = InetSocketAddress.createUnresolved("127.0.0.1", port);
  }

  public SocketAddress address() {
    return address;
  }

  private EventLoopGroup eventLoopGroup;
  private EventLoopGroup bossGroup;

  public EventLoopGroup bossGroup() {
    if (this.bossGroup == null) {
      this.bossGroup = new NioEventLoopGroup(1);
    }
    return bossGroup;
  }

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

  public void bossGroup(EventLoopGroup bossGroup) {
    this.bossGroup = bossGroup;
  }

  private ChannelHandler init;

  public ChannelHandler init() {
    assert init == null;
    return init;
  }

  public void init(ChannelHandler init) {
    this.init = init;
  }

  public boolean isOk() {
    try {
      return future().isSuccess();
    } catch (Exception e) {
      return false;
    }
  }

  public String waitForIt() throws InterruptedException {
    return waitForIt(true);
  }

  public String waitForIt(boolean rm) throws InterruptedException {
    for (int i = 0; i < PropUtil.REQ_MAX; i++) {
      if (LAST_RESP.containsKey(host())) {
        return rm ? LAST_RESP.remove(host()) : LAST_RESP.get(host());
      }
      Thread.sleep(PropUtil.REQ_INTERVAL);
    }
    return "And Then There Were None";
  }

  void waitForIt(Runnable run, int num, int timeout)
      throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(num);
    ChannelHandler handler = new AggregateChannelHandler(latch);
    addHandler("aggregate", handler);
    run.run();
    latch.await(timeout, TimeUnit.MILLISECONDS);

  }

  public int session() {
    return session.get();
  }

  public int inSession() {
    return session.incrementAndGet();
  }

  public int outSession() {
    return session.decrementAndGet();
  }

  public NettyFactory cacheIn() {
    CLIENTS_WRITE_LOCK.lock();
    NettyFactory client = CLIENTS.get(host());
    if (client == null) {
      client = this;
    }
    CLIENTS.put(host(), client);
    client.inSession();
    CLIENTS_WRITE_LOCK.unlock();
    return client;
  }

  @Override
  public void close() {
    CLIENTS_WRITE_LOCK.lock();
    if (outSession() > 0) {
      CLIENTS_WRITE_LOCK.unlock();
      return;
    }
    CLIENTS.remove(host());
    CLIENTS_WRITE_LOCK.unlock();
    synchronized (this) {
      eventLoopGroup().shutdownGracefully();
      if (bossGroup() != null) {
        bossGroup().shutdownGracefully();
      }
    }
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, "solong " + host());
  }
}
