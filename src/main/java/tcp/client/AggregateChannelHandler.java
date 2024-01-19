package tcp.client;

import java.util.concurrent.CountDownLatch;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public final class AggregateChannelHandler extends ChannelInboundHandlerAdapter {

  public CountDownLatch latch = null;

  public AggregateChannelHandler(CountDownLatch latch) {
    this.latch = latch;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx)
      throws Exception {
    latch.countDown();
    System.out.println(latch.getCount());
  }
}
