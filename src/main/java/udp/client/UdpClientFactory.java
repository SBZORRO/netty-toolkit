package udp.client;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import tcp.client.CountDownLatchChannelHandler;
import tcp.client.InitializerFactory;
import tcp.client.ClientFactory;

public class UdpClientFactory {

  public static void main(String[] args) throws InterruptedException {
//    bind(5899, null, null);
//    once("172.17.43.172", 5899, "sdfgh", null, null);
  }
//
//  public static NettyFactory
//      bootstrap(String ip, int port, String dcd, String args)
//          throws InterruptedException {
//    NettyFactory client = new UdpClient(ip, port);
//    return NettyFactory.bootstrap(client,
//        new InitializerFactory.UdpInitializer(),
//        new UdpBindListener());
//  }

//  public static String
//      once(
//          String ip, int port, String msg, String dcd, String args, int max,
//          int interval) throws InterruptedException {
//    try (NettyFactory client = bootstrap(ip, port, dcd, args)) {
//      send(client, msg);
//      return client.waitForIt(true, max, interval);
//    }
//  }
//
//  public static String
//      aggregrate(
//          CountDownLatch obj, String ip, int port, String msg, String dcd,
//          String args, int max, int interval)
//          throws InterruptedException {
//    try (NettyFactory client = bootstrap(ip, port, dcd, args)) {
//      client.addHandler("aggregate", new AggregateChannelHandler(obj));
//      send(client, msg);
//      return client.waitForIt(false, max, interval);
//    } catch (InterruptedException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//    return "And Then There Were None";
//  }

//  public static void send(NettyFactory client, String msg)
//      throws InterruptedException {
//    NettyFactory.LAST_CMD.put(client.host(), msg);
//    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, client.host() + " <<< " + msg);
//
//    if (HexByteUtil.isHex(msg)) {
//      client.writeAndFlush(new DatagramPacket(
//          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg)),
//          new InetSocketAddress(client.ip(), client.port())))
//          .sync();
//    } else {
//      client.writeAndFlush(new DatagramPacket(
//          Unpooled.copiedBuffer(msg.getBytes()),
//          new InetSocketAddress(client.ip(), client.port())))
//          .sync();
//    }
//  }
}
