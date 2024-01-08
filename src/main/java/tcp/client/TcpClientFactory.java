package tcp.client;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;
import com.sbzorro.PropUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;

public class TcpClientFactory {
  public static final Map<String, String> last_cmd = new ConcurrentHashMap<>();
  public static final Map<String, String> last_resp = new ConcurrentHashMap<>();

  public static final ScheduledExecutorService EXE = Executors
      .newSingleThreadScheduledExecutor();

  public static TcpClient connect(String ip, int port)
      throws InterruptedException {
    return connect(ip, port, null, null);
  }

  public static TcpClient connect(String ip, int port, String dcd, String args)
      throws InterruptedException {
    String host = ip + ":" + port;
    TcpClient.CLIENTS.putIfAbsent(host, new TcpClient(ip, port));
    TcpClient client = TcpClient.CLIENTS.get(host);
    if (isClientOk(client)) {
      return client;
    }
    synchronized (client) {
//      TcpClient client = new TcpClient(ip, port);
      client.init(newInitializer(dcd, args));
      client.listeners(new TcpConnectionListener());
      client.remoteAddress(ip, port);
      client.connect().sync();
    }
//    TcpClient.CLIENTS.put(host, client);
    return client;
  }

  public static String send(String ip, int port, String msg)
      throws InterruptedException {
    TcpClient client = connect(ip, port);
    send0(client.future(), msg);
    return msg;
  }

  public static String once(
      String proto, String ip, int port, String msg, String dcd, String args)
      throws InterruptedException {
//    String host = ip + ":" + port;
//    last_cmd.put(host, msg);

//    TcpClient client = connect(ip, port, dcd, args);
    try (TcpClient client = connect(ip, port, dcd, args)) {
//      client.init(new MyChannelInitializer());
//      client.init(new FixedLengthInitializer(len));
//      client.init(newInitializer(dcd, args));
//      client.listeners(new TcpConnectionListener());
//      client.remoteAddress(ip, port);
//      ChannelFuture future = client.connect().sync();
      client.future().channel().pipeline().remove("rcnct");
//      RetransmissionHandler<String> handler = new RetransmissionHandler<>(
//          (originalMessage) -> {
//            LogUtil.DEBUG.info("try");
//            future.channel().writeAndFlush(
//                Unpooled.copiedBuffer(
//                    HexByteUtil.cmdToByteNoSep(originalMessage)));
//          }, msg, EXE, PropUtil.REQ_INTERVAL, PropUtil.REQ_MAX);
//      handler.start();

      send0(client.future(), msg);
      return waitForIt(ip, port);
//      for (int i = 0; i < PropUtil.REQ_MAX; i++) {
//        if (last_resp.containsKey(host)) {
//          return last_resp.remove(host);
//        }
//        Thread.sleep(PropUtil.REQ_INTERVAL);
//      }
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return "And Then There Were None";
  }

  static void send0(ChannelFuture future, String msg)
      throws InterruptedException {
    String ip = ((InetSocketAddress) future.channel().remoteAddress())
        .getHostString();
    int port = ((InetSocketAddress) future.channel().remoteAddress())
        .getPort();

    String host = ip + ":" + port;
    last_cmd.put(host, msg);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, host + " <<< " + msg);

    if (HexByteUtil.isHex(msg)) {
      future.channel().writeAndFlush(
          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg))).sync();
    } else {
      future.channel().writeAndFlush(
          Unpooled.copiedBuffer(msg.getBytes())).sync();
    }
  }

  static void sendRetrans(ChannelFuture future, String msg) {
    RetransmissionHandler<String> handler = new RetransmissionHandler<>(
        (originalMessage) -> {
          try {
            send0(future, msg);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }, msg, EXE, PropUtil.REQ_INTERVAL, PropUtil.REQ_MAX);
    handler.start();
  }

  static String waitForIt(String ip, int port)
      throws InterruptedException {
    String host = ip + ":" + port;
    for (int i = 0; i < PropUtil.REQ_MAX; i++) {
      if (last_resp.containsKey(host)) {
        return last_resp.remove(host);
      }
      Thread.sleep(PropUtil.REQ_INTERVAL);
    }
    return "And Then There Were None";
  }

  static boolean isClientOk(TcpClient client) {
    try {
      return client.future().isSuccess();
    } catch (Exception e) {
      return false;
    }
//    if (client == null || client.future() == null
//        || !client.future().isSuccess()) {
//      return false;
//    }
//    return true;
  }

  public static ChannelInitializer<NioSocketChannel>
      newInitializer(String dcd, String args) {
    if (dcd == null) {
      return new RawInitializer();
    }
    switch (dcd) {
      case "lfb":
        String[] sArr = args.split("_");
        return new LengthFieldBasedInitializer(Integer.parseInt(sArr[0]),
            Integer.parseInt(sArr[1]), Integer.parseInt(sArr[2]),
            Integer.parseInt(sArr[3]));
      case "fl":
        return new FixedLengthInitializer(Integer.parseInt(args));
      case "dlmt":
        return new DlmtInitializer(args);
      case "lb":
        return new LineBasedInitializer();
      case "json":
        return new JsonInitializer();
      case "string":
        return new StringInitializer();
      case "enum":
        String[] enums = args.split("_");
        return new EnumInitializer();
      default:
        return new RawInitializer();
    }
  }

  private static class RawInitializer extends ChannelInitializer<NioSocketChannel> {
    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast(new RawChannelHandler());
      ch.pipeline().addLast("rcnct", new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }

  private static class StringInitializer extends ChannelInitializer<NioSocketChannel> {
    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast(new StringChannelHandler());
      ch.pipeline().addLast("rcnct", new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }

  public static class LengthFieldBasedInitializer extends ChannelInitializer<NioSocketChannel> {

    int lengthFieldOffset;
    int lengthFieldLength;
    int lengthAdjustment;
    int initialBytesToStrip;

    public LengthFieldBasedInitializer(
        int lengthFieldOffset, int lengthFieldLength,
        int lengthAdjustment, int initialBytesToStrip) {
      this.lengthFieldOffset = lengthFieldOffset;
      this.lengthFieldLength = lengthFieldLength;
      this.lengthAdjustment = lengthAdjustment;
      this.initialBytesToStrip = initialBytesToStrip;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
//      ch.pipeline().addLast(
//          new DelimiterBasedFrameDecoder(1024, false,
//              Unpooled.copiedBuffer(HexByteUtil.cmdToByte("bb 0d"))));
      ch.pipeline()
          .addLast(new LengthFieldBasedFrameDecoder(1024, lengthFieldOffset,
              lengthFieldLength, lengthAdjustment, initialBytesToStrip));
//      .addLast(new LengthFieldBasedFrameDecoder(1024, 4, 1, 0, 0));
      ch.pipeline().addLast(new RawChannelHandler());
      ch.pipeline().addLast("rcnct", new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }

  public static class FixedLengthInitializer extends ChannelInitializer<NioSocketChannel> {
    int length;

    public FixedLengthInitializer(int length) {
      this.length = length;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast(new FixedLengthFrameDecoder(length));
      ch.pipeline().addLast(new RawChannelHandler());
      ch.pipeline().addLast("rcnct", new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }

  public static class DlmtInitializer extends ChannelInitializer<NioSocketChannel> {
    String dlmt;

    public DlmtInitializer(String dlmt) {
      this.dlmt = dlmt;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast(
          new DelimiterBasedFrameDecoder(1024, false,
              Unpooled.copiedBuffer(dlmt.getBytes())));
      ch.pipeline().addLast(new RawChannelHandler());
      ch.pipeline().addLast("rcnct", new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }

  public static class LineBasedInitializer extends ChannelInitializer<NioSocketChannel> {

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
      ch.pipeline().addLast(new RawChannelHandler());
      ch.pipeline().addLast("rcnct", new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }

  public static class JsonInitializer extends ChannelInitializer<NioSocketChannel> {

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast(new JsonObjectDecoder());
      ch.pipeline().addLast(new RawChannelHandler());
      ch.pipeline().addLast("rcnct", new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }

  public static class EnumInitializer extends ChannelInitializer<NioSocketChannel> {

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast(new EnumDecoder());
      ch.pipeline().addLast(new RawChannelHandler());
      ch.pipeline().addLast("rcnct", new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }
}

//try (TcpClient client = new TcpClient()) {
//client.init(new LengthFieldBasedInitializer());
//client.listeners(new TcpConnectionListener());
//client.remoteAddress(ip, port);
//ChannelFuture future = client.connect().sync();
//
//future.channel().writeAndFlush(
//    Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(cmd))).sync();
//
//int i = 100;
//while (i < 5000) {
//  if (!"".equals(RawChannelHandler.last_resp)) {
//    return RawChannelHandler.last_resp;
//  }
//  try {
//    Thread.sleep(100);
//  } catch (InterruptedException e) {
//    // TODO Auto-generated catch block
//    e.printStackTrace();
//  }
//  i = i + 100;
//}
//} catch (InterruptedException e) {
//// TODO Auto-generated catch block
//e.printStackTrace();
//}
//return "hello world";
