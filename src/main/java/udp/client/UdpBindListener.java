package udp.client;

import java.util.concurrent.TimeUnit;

import com.sbzorro.LogUtil;
import com.sbzorro.PropUtil;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

public class UdpBindListener implements ChannelFutureListener {
  @Override
  public void operationComplete(ChannelFuture channelFuture) throws Exception {
    if (!channelFuture.isSuccess()) {
      LogUtil.DEBUG.debug(LogUtil.UDP_MARKER, "Reconnect");
      final EventLoop loop = channelFuture.channel().eventLoop();
      loop.schedule(new Runnable() {
        @Override
        public void run() {
//          WolUtil.INSTANCE.bind();
        }
      }, PropUtil.RETRY_INTERVAL, TimeUnit.MILLISECONDS);
    }
  }
}
