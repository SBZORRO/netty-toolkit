package tcp.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import udp.client.UdpBindListener;
import udp.client.UdpClient;

public abstract class ClientFactory {

  public static void send(NettyWrapper client, String msg)
      throws InterruptedException {
    client.send(msg);
  }

  public static NettyWrapper bootstrap(String proto, String ip, int port)
      throws InterruptedException {
    return bootstrap(proto, ip, port, null, null);
  }

  public static NettyWrapper
      bootstrap(String proto, String ip, int port, String dcd, String args)
          throws InterruptedException {
    proto = proto.toLowerCase();
    if ("udp".equals(proto)) {
      return bootstrapUdp(ip, port, null, null);
    } else if ("tcp".equals(proto)) {
      return bootstrapTcp(ip, port, dcd, args);
    } else if ("ts".equals(proto)) {
      return bind(port, dcd, args);
    }
    return null;
  }

  public static NettyWrapper
      bootstrapTcp(String ip, int port, String dcd, String args)
          throws InterruptedException {
    NettyWrapper client = new TcpClient(ip, port);
    return ClientFactory.bootstrap(client,
        InitializerFactory.newInitializer(dcd, args),
        new TcpConnectionListener());
  }

  public static NettyWrapper bootstrapTcp(String ip, int port)
      throws InterruptedException {
    return bootstrapTcp(ip, port, null, null);
  }

  public static NettyWrapper
      bootstrapUdp(String ip, int port, String dcd, String args)
          throws InterruptedException {
    NettyWrapper client = new UdpClient(ip, port);
    return ClientFactory.bootstrap(client,
        new InitializerFactory.UdpInitializer(),
        new UdpBindListener());
  }

  public static NettyWrapper bind(int port, String dcd, String args)
      throws InterruptedException {
    NettyWrapper client = new TcpServer(port);
    return ClientFactory.bootstrap(client,
        InitializerFactory.newInitializer(dcd, args),
        new TcpConnectionListener());
  }

  public static String
      once(
          NettyWrapper client,
          String ip, int port, String msg, String dcd, String args,
          int timeout) throws InterruptedException {
//    try (NettyFactory client = bootstrap(ip, port, dcd, args)) {
    CdlHandler cdl = CdlHandler.newInstance(1);
    client.addHandler("latch", cdl);
    client.send(msg);
    boolean to = cdl.getLatch().await(timeout, TimeUnit.MILLISECONDS);
    return NettyWrapper.LAST_RESP.remove(client.host());
  }

  public static String
      aggregrate(
          NettyWrapper client,
          CountDownLatch latch, String ip, int port, String msg, String dcd,
          String args, int timeout)
          throws InterruptedException {
    CdlHandler cdl = CdlHandler.newInstance(latch);
    client.addHandler("latch", cdl);
//    client.addHandler("latch", new CdlHandler(latch));
    client.send(msg);
//    boolean to = latch.await(timeout, TimeUnit.MILLISECONDS);
    return NettyWrapper.LAST_RESP.get(client.host());
  }

  public static NettyWrapper
      bootstrap(
          NettyWrapper client, ChannelInitializer<?> init,
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
}
