package tcp.client;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class TcpServer extends NettyFactory {

  public TcpServer() {}

  public TcpServer(String host, int port) {
    this.ip = host;
    this.port = port;
    address(host, port);
  }

  public TcpServer(int port) {
    this.port = port;
  }

  public ChannelFuture bootstrap() {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(bossGroup(), eventLoopGroup());
    bootstrap.channel(channelClass());
    bootstrap.option(ChannelOption.SO_BACKLOG, 100);
    bootstrap.handler(new LoggingHandler(LogLevel.INFO));
    bootstrap.childHandler(init());

    future = bootstrap.bind(ip, port).addListeners(listeners);
    return future;
  }

  public Class<? extends ServerChannel> channelClass() {
    return NioServerSocketChannel.class;
  }
}
