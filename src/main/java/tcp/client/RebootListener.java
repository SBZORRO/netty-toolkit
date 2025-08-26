package tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public abstract class RebootListener implements ChannelFutureListener {
  Bootstrap bootstrap;

  public RebootListener(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  @Override
  public abstract void operationComplete(ChannelFuture future)
      throws Exception;

}
