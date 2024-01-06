package com.sbzorro;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class StringChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

  public static void main(String[] args) {}

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)
      throws Exception {
    byte[] ba = new byte[msg.readableBytes()];
    msg.readBytes(ba);

    String ip = ((InetSocketAddress) ctx.channel().remoteAddress())
        .getHostString();
    int port = ((InetSocketAddress) ctx.channel().remoteAddress())
        .getPort();

    String host = ip + ":" + port;
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER,
        host + " >>> " + new String(ba));
    TcpClientFactory.last_resp.put(host, new String(ba));

//    App.CLIENT.publish("open_exhibition_hall", host + " OK");

//    App.EXE.execute(
//        () -> ReadToInflux.writeDevStatus(host, "OK"));
  }
}
