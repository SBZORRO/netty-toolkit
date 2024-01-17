package tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TcpClient extends NettyFactory {
  public TcpClient() {}

  public TcpClient(String host, int port) {
    this.ip = host;
    this.port = port;
    address(host, port);
  }

  public TcpClient(int port) {
    this.port = port;
  }

  public ChannelFuture bootstrap() {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(eventLoopGroup());
    bootstrap.channel(channelClass());
    bootstrap.remoteAddress(address());
    bootstrap.handler(init());
    future = bootstrap.connect().addListeners(listeners);
    return future;
  }

  public Class<? extends Channel> channelClass() {
    return NioSocketChannel.class;
  }
}
