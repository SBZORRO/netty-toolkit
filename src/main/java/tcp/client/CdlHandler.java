package tcp.client;

import java.util.concurrent.CountDownLatch;

import com.sbzorro.LogUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public final class CdlHandler extends ChannelInboundHandlerAdapter {

  CountDownLatch latch = null;
  int type = 1; // 1: 立即删除;

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx)
      throws Exception {
    latch.countDown();
    if (latch.getCount() == 0 || type == 1) {
      ctx.pipeline().remove(this);
    }
    LogUtil.DEBUG.info(LogUtil.SOCK_MARKER, latch.getCount());
  }

  public void remove(NettyWrapper client) {
    client.removeHandler(this);
  }

  public static CdlHandler newInstance(CountDownLatch latch) {
    CdlHandler cdl = new CdlHandler();
    cdl.setLatch(latch);
    return cdl;
  }

  public static CdlHandler newInstance(int count) {
    CdlHandler cdl = new CdlHandler();
    cdl.setLatch(new CountDownLatch(count));
    return cdl;
  }

  public static CdlHandler newInstance(int count, int type) {
    CdlHandler cdl = new CdlHandler();
    cdl.setLatch(new CountDownLatch(count));
    cdl.setType(type);
    return cdl;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public void setLatch(CountDownLatch latch) {
    this.latch = latch;
  }

  public CountDownLatch getLatch() {
    return latch;
  }
}
