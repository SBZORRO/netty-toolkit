package udp.client;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import tcp.client.TcpClient;

public class WolUtil {
  public static final WolUtil INSTANCE = new WolUtil();
  public static final Map<String, TcpClient> CLIENTS = new ConcurrentHashMap<>();

  public ChannelFuture future = null;

  void bind(int port) {
    EventLoopGroup eventLoop = new NioEventLoopGroup(1);
    Bootstrap bootstrap = new Bootstrap().group(eventLoop)
        .channel(NioDatagramChannel.class)
//        .option(EpollChannelOption.SO_REUSEADDR, true)
//        .option(EpollChannelOption.SO_REUSEPORT, true)
        .handler(new MyInitializer());
    future = bootstrap.bind(port).addListener(new UdpBindListener());
  }

  public static void send(String host, int port, byte[] msg) {
    EventLoopGroup group = new NioEventLoopGroup(1);
    try {
      Bootstrap b = new Bootstrap();
      b.group(group).channel(NioDatagramChannel.class)
          .option(EpollChannelOption.SO_REUSEADDR, true)
          .option(EpollChannelOption.SO_REUSEPORT, true)
          .handler(new ChannelOutboundHandlerAdapter());
      Channel ch = b.bind(9).sync().channel();
      ch.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(msg),
          new InetSocketAddress(host, port))).sync();
//            ch.closeFuture().await();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      group.shutdownGracefully().syncUninterruptibly();
    }
  }

  public void shutdown() {
    future.channel().closeFuture().syncUninterruptibly();
    future.channel().eventLoop().shutdownGracefully().syncUninterruptibly();
  }

  class MyInitializer extends ChannelInitializer<NioDatagramChannel> {

    @Override
    protected void initChannel(NioDatagramChannel ch) throws Exception {
      ch.pipeline().addLast(new RawDatagramHandler());
    }
  }

  public static void
      wakeupEthernetAddresses(String mac, String host, int port) {
    wakeup(mac, host, port);
  }

  public static void wakeup(
      String mac, String host, int port) {
    wakeup(new String[] { mac }, host, port);
  }

  public static void wakeup(
      String[] mac, String host, int port) {
    for (int i = 0; i < mac.length; i++) {
      byte[] wakeupFrame = createWakeupFrame(mac[i]);

      send(host, port, wakeupFrame);
    }
  }

  protected static byte[] createWakeupFrame(String mac) {

    byte[] ethernetAddressBytes = HexByteUtil.cmdToByte(mac);
//        ethernetAddress.toBytes();
    byte[] wakeupFrame = new byte[6 + 16 * ethernetAddressBytes.length];

    Arrays.fill(wakeupFrame, 0, 6, (byte) 0xFF);

    for (int j = 6; j < wakeupFrame.length; j += ethernetAddressBytes.length) {
      System.arraycopy(ethernetAddressBytes, 0, wakeupFrame, j,
          ethernetAddressBytes.length);
    }

    return wakeupFrame;
  }

  public static void main(String[] args)
      throws InterruptedException {
//  MyMqttClient.CLIENT.connect("127.0.0.1", 1883).sync();

    LogUtil.SOCK.info("UDP RECEIVER");

    wakeupEthernetAddresses("", "", 0);

    Thread.interrupted();
  }

  public static final String ip = "255.255.255.255";
  public static final int port = 9;
  public static final Map<String, String> title_mac = new HashMap<>() { {} };

  public static final String[] macArr = new String[] {};
}
