package mqtt.client;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;

public final class TcpReconnectHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    final EventLoop eventLoop = ctx.channel().eventLoop();
    eventLoop.schedule(
        () -> ctx.channel().pipeline().connect(ctx.channel().remoteAddress()),
        1000, TimeUnit.MILLISECONDS);
  }
}
