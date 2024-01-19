package udp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.socket.nio.NioDatagramChannel;
import tcp.client.NettyFactory;

public class UdpClient extends NettyFactory {

  public UdpClient() {}

  public UdpClient(String host, int port) {
    this.ip = host;
    this.port = port;
  }

  public UdpClient(int port) {
    this.port = port;
    this.ip = "127.0.0.1";
  }

  public ChannelFuture bootstrap() {
    Bootstrap bootstrap = new Bootstrap()
        .group(eventLoopGroup())
        .channel(channelClass())
        .option(EpollChannelOption.SO_REUSEADDR, true)
        .option(EpollChannelOption.SO_REUSEPORT, true)
        .handler(init());
//    .handler(new MyInitializer());
//    future = bootstrap.bind(port).addListener(new UdpConnectionListener());
    future = bootstrap.bind(12345).addListeners(listeners);
    return future;
  }

  public Class<? extends Channel> channelClass() {
    return NioDatagramChannel.class;
  }
}
