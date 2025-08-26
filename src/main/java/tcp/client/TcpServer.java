package tcp.client;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class TcpServer extends NettyWrapper {

  ServerBootstrap bootstrap;

  public TcpServer() {}

  public TcpServer(String host, int port) {
    this.ip = host;
    this.port = port;
    address(host, port);
  }

  public TcpServer(int port) {
    this.port = port;
  }

  @Override
  public ChannelFuture bootstrap() {
    future = this.bootstrap.bind(ip, port);
    listen();
    return future;
  }

  @Override
  public ServerBootstrap initBootstrap() {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(singleGroup(), singleGroup());
    bootstrap.channel(NioServerSocketChannel.class);
    bootstrap.option(ChannelOption.SO_BACKLOG, 100);
    bootstrap.handler(new LoggingHandler(LogLevel.INFO));
    bootstrap.childHandler(init());
    this.bootstrap = bootstrap;
    return bootstrap;
  }

//  @Override
//  public Class<? extends ServerChannel> channelClass() {
//    return NioServerSocketChannel.class;
//  }

  @Override
  public void send(String msg) throws InterruptedException {
    String host = this.host();
    NettyWrapper.LAST_CMD.put(host, msg);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, host + " <<< " + msg);

    if (HexByteUtil.isHex(msg)) {
      this.writeAndFlush(
          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg))).sync();
    } else {
      this.writeAndFlush(Unpooled.copiedBuffer(msg.getBytes())).sync();
    }
  }
}
