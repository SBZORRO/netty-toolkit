package tcp.client;

import java.util.concurrent.CountDownLatch;

import com.sbzorro.LogUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public final class CountDownLatchChannelHandler extends ChannelInboundHandlerAdapter {

  public CountDownLatch latch = null;

  public CountDownLatchChannelHandler(CountDownLatch latch) {
    this.latch = latch;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx)
      throws Exception {
    latch.countDown();
    LogUtil.DEBUG.info(LogUtil.SOCK_MARKER, latch.getCount());
  }
}
