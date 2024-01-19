package udp.client;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import tcp.client.AggregateChannelHandler;
import tcp.client.NettyFactory;

public class UdpClientFactory {

  public static void main(String[] args) throws InterruptedException {
//    bind(5899, null, null);
    once("172.17.43.172", 5899, "sdfgh", null, null);
  }

  public static NettyFactory
      bootstrap(String ip, int port, String dcd, String args)
          throws InterruptedException {
    NettyFactory client = new UdpClient(ip, port);
    return bootstrap(client, dcd, args);
  }

  public static NettyFactory
      bootstrap(NettyFactory client, String dcd, String args)
          throws InterruptedException {
    client = client.cacheIn();
    if (client.isOk()) {
      return client;
    }
    synchronized (client) {
      if (client.isOk()) {
        return client;
      }

      client.init(new MyInitializer());
      client.listeners(new UdpBindListener());
      client.bootstrap().sync();
    }
    return client;
  }

  public static String
      once(String ip, int port, String msg, String dcd, String args) {
    try (NettyFactory client = bootstrap(ip, port, dcd, args)) {
//      client.future().channel().pipeline().remove("rcnct");
      send0(client, msg);
      return client.waitForIt();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return "And Then There Were None";
  }

  public static String
      aggregrate(
          CountDownLatch obj, String ip, int port, String msg, String dcd,
          String args)
          throws InterruptedException {
    try (NettyFactory client = bootstrap(ip, port, dcd, args)) {
      client.removeHandler("rcnct");
      client.addHandler("aggregate", new AggregateChannelHandler(obj));
      send0(client, msg);
      return client.waitForIt(false);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return "And Then There Were None";
  }

  static void send0(NettyFactory client, String msg)
      throws InterruptedException {
    NettyFactory.LAST_CMD.put(client.host(), msg);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, client.host() + " <<< " + msg);

    if (HexByteUtil.isHex(msg)) {
      client.writeAndFlush(new DatagramPacket(
          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg)),
          new InetSocketAddress(client.ip(), client.port())))
          .sync();
    } else {
      client.writeAndFlush(new DatagramPacket(
          Unpooled.copiedBuffer(msg.getBytes()),
          new InetSocketAddress(client.ip(), client.port())))
          .sync();
    }
  }

  static class MyInitializer extends ChannelInitializer<NioDatagramChannel> {
    @Override
    protected void initChannel(NioDatagramChannel ch) throws Exception {
      ch.pipeline().addLast(new TbdHandler());
    }
  }
}
