package tcp.client;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.sbzorro.LogUtil;
import com.sbzorro.PropUtil;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.ObjectUtil;

public abstract class NettyWrapper implements Closeable {
  public static final Map<String, NettyWrapper> CLIENTS = new ConcurrentHashMap<>();

  public static final ReentrantReadWriteLock CLIENTS_LOCK = new ReentrantReadWriteLock();
  public static final WriteLock CLIENTS_WRITE_LOCK = CLIENTS_LOCK.writeLock();

  public static final Map<String, String> LAST_CMD = new ConcurrentHashMap<>();
  public static final Map<String, String> LAST_RESP = new ConcurrentHashMap<>();

  public static int OPTION_TCP_TIMEOUT = PropUtil.TCP_TIMEOUT;

  public static final ScheduledExecutorService EXE = Executors
      .newSingleThreadScheduledExecutor();

  public final long startuptime = System.currentTimeMillis();

  private AtomicInteger session = new AtomicInteger(0);

  public abstract ChannelFuture bootstrap();

  public abstract void send(String msg) throws InterruptedException;

  public static NettyWrapper bootstrap(NettyWrapper client) {
    client = client.cacheIn();
    if (client.isOk()) {
      return client;
    }
    synchronized (client) {
      if (client.isOk()) {
        return client;
      }
      client.bootstrap();
    }
    return client;
  }

  public static NettyWrapper bootstrap(NettyWrapper client,
      ChannelInitializer<?> init) {
    client = client.cacheIn();
    if (client.isOk()) {
      return client;
    }
    synchronized (client) {
      if (client.isOk()) {
        return client;
      }
      client.init(init);
      client.bootstrap();
    }
    return client;
  }

  public static void send(NettyWrapper client, String msg)
      throws InterruptedException {
    client.send(msg);
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

  public void removeHandler(ChannelHandler handler) {
    try {
      future().channel().pipeline().remove(handler);
    } catch (Exception e) {
      // ignore
    }
  }

  public NettyWrapper addHandler(String name, ChannelHandler handler) {
    future().channel().pipeline().addLast(name, handler);
    return this;
  }

  protected ChannelFuture future;

  public ChannelFuture future() {
    return future;
  }

  protected ChannelFutureListener[] listeners;

  public NettyWrapper listeners(ChannelFutureListener... listeners) {
    this.listeners = listeners;
    return this;
  }

  public void addListeners(ChannelFutureListener... listeners) {
    assert future != null;
    future.addListeners(listeners);
  }

  public void listen() {
    if (listeners != null) {
      future.addListeners(listeners);
    }
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

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public EventLoopGroup singleGroup() {
    if (this.bossGroup == null) {
      this.bossGroup = new NioEventLoopGroup(1);
    }
    return bossGroup;
  }

  public EventLoopGroup bossGroup() {
    return bossGroup;
  }

  public EventLoopGroup workerGroup() {
    return workerGroup;
  }

  public EventLoopGroup bossGroup(int i) {
    if (this.bossGroup == null) {
      this.bossGroup = new NioEventLoopGroup(i);
    }
    return bossGroup;
  }

  public EventLoopGroup workerGroup(int i) {
    if (this.workerGroup == null) {
      this.workerGroup = new NioEventLoopGroup(i);
    }
    return workerGroup;
  }

  public void eventLoopGroup(EventLoopGroup workerGroup) {
    this.workerGroup = workerGroup;
  }

  public void bossGroup(EventLoopGroup bossGroup) {
    this.bossGroup = bossGroup;
  }

  private ChannelHandler init;

  public ChannelHandler init() {
    assert init == null;
    return init;
  }

  public NettyWrapper init(ChannelHandler init) {
    this.init = init;
    return this;
  }

  private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<>();

  public Map<ChannelOption<?>, Object> options() {
    return options;
  }

  public <B extends AbstractBootstrap<B, C>, C extends Channel> void options(
      AbstractBootstrap<B, C> b) {
    for (Map.Entry<ChannelOption<?>, Object> entry : options.entrySet()) {
      b.option((ChannelOption<Object>) entry.getKey(), entry.getValue());
    }
  }

  public <T> NettyWrapper option(ChannelOption<T> option, T value) {
    ObjectUtil.checkNotNull(option, "option");
    synchronized (options) {
      if (value == null) {
        options.remove(option);
      } else {
        options.put(option, value);
      }
    }
    return this;
  }

  public boolean isOk() {
    try {
      return future().isSuccess();
    } catch (Exception e) {
      return false;
    }
  }

  static void sendRetrans(NettyWrapper client, String msg) {
    RetransmissionHandler<String> handler = new RetransmissionHandler<>(
        (originalMessage) -> {
          try {
            client.send(msg);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }, msg, NettyWrapper.EXE, PropUtil.REQ_INTERVAL, PropUtil.REQ_MAX);
    handler.start();
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

  public NettyWrapper cacheIn() {
    CLIENTS_WRITE_LOCK.lock();
    NettyWrapper client = CLIENTS.get(host());
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
      workerGroup().shutdownGracefully();
      if (bossGroup() != null) {
        bossGroup().shutdownGracefully();
      }
    }
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, "solong " + host());
  }
}
