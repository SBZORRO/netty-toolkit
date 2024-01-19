package tcp.client;

import java.util.concurrent.CountDownLatch;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public final class UdpAggregateChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  public CountDownLatch latch = null;

  public UdpAggregateChannelHandler(CountDownLatch latch) {
    this.latch = latch;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg)
      throws Exception {
    latch.countDown();
    System.out.println(latch.getCount());
  }
}
