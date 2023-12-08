package mqtt.client;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.jerei.App;
import com.jerei.LogUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;

public final class MqttReconnectHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    LogUtil.SOCK
        .info("CONNECTED: " + ctx.channel().remoteAddress().toString());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    String host
        = ((InetSocketAddress) ctx.channel().remoteAddress())
            .getHostString();
    int port
        = ((InetSocketAddress) ctx.channel().remoteAddress())
            .getPort();

    LogUtil.SOCK.info("RECONNECT: MqttReconnectHandler: "
        + ctx.channel().remoteAddress());

    final EventLoop loop = ctx.channel().eventLoop();
    loop.schedule(() -> App.sub("test-zt"), 5, TimeUnit.SECONDS);

//    App.sub("test-zt");
  }
}
