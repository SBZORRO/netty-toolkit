package mqtt.client;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

public class TcpConnectionListener implements ChannelFutureListener {

  @Override
  public void operationComplete(ChannelFuture channelFuture) throws Exception {
    if (!channelFuture.isSuccess()) {
      final EventLoop loop = channelFuture.channel().eventLoop();
      loop.schedule(() -> {
        channelFuture.channel().pipeline()
            .connect(channelFuture.channel().remoteAddress());
      }, 1000, TimeUnit.MILLISECONDS);
    }
  }
}
