package tcp.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;

public class InitializerFactory {
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