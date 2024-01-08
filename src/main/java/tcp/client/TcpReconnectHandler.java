package tcp.client;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.sbzorro.LogUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;

public final class TcpReconnectHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER,
        "CONNECTED: " + ctx.channel().remoteAddress().toString());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    super.channelInactive(ctx);

//    final EventLoop eventLoop = ctx.channel().eventLoop();
//    eventLoop.schedule(
//        () -> ctx.channel().pipeline().connect(ctx.channel().remoteAddress()),
//        1000, TimeUnit.MILLISECONDS);

    String host = ((InetSocketAddress) ctx.channel().remoteAddress())
        .getHostString();
    int port = ((InetSocketAddress) ctx.channel().remoteAddress())
        .getPort();
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, "RECONNECT: TcpReconnectHandler: "
        + ctx.channel().remoteAddress());

    final EventLoop loop = ctx.channel().eventLoop();
    loop.schedule(() -> TcpClient.CLIENTS.get(host + ":" + port).connect(), 5,
        TimeUnit.SECONDS);
  }
}
