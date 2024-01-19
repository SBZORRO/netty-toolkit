package tcp.client;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.sbzorro.LogUtil;
import com.sbzorro.PropUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

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
