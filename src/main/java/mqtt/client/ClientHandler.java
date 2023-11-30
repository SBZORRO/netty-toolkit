package mqtt.client;


import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    System.out.println("channel active");
    ByteBuf msg = Unpooled.wrappedBuffer(
        "Hi I just connected\n".getBytes(StandardCharsets.US_ASCII));
    System.out.println(msg.readableBytes());
    ctx.writeAndFlush(msg);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg)
      throws Exception {
    ByteBuf byteBuf = (ByteBuf) msg;
    byteBuf.readBytes(System.out, ((ByteBuf) msg).readableBytes());
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // Close the connection when an exception is raised.
    cause.printStackTrace();
    ctx.close();
  }
}
