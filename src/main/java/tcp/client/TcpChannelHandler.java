package tcp.client;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class TcpChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

  ITcpReader handler;

  public TcpChannelHandler(ITcpReader handler) {
    this.handler = handler;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)
      throws Exception {
    byte[] ba = new byte[msg.readableBytes()];
    msg.readBytes(ba);

    InetSocketAddress addr = ((InetSocketAddress) ctx.channel()
        .remoteAddress());
    String ip = addr.getHostString();
    int port = addr.getPort();

    handler.onMessage(ip + ":" + port, ba);
  }
}
