package udp.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import tcp.client.ITcpReader;

/**
 * Hello world!
 *
 */
public class DatagramChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  ITcpReader handler;

  public DatagramChannelHandler(ITcpReader handler) {
    this.handler = handler;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg)
      throws Exception {

    byte[] ba = new byte[msg.content().readableBytes()];
    msg.content().readBytes(ba);

    handler.onMessage(ba);
  }
}
