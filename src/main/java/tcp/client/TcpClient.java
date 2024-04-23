package tcp.client;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TcpClient extends NettyWrapper {

  public TcpClient() {}

  public TcpClient(String host, int port) {
    this.ip = host;
    this.port = port;
    address(host, port);
  }

  public TcpClient(int port) {
    this.port = port;
  }

  @Override
  public ChannelFuture bootstrap() {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(eventLoopGroup());
    bootstrap.channel(NioSocketChannel.class);
//  bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, OPTION_TCP_TIMEOUT);
    bootstrap.option(ChannelOption.SO_RCVBUF, 1024_000_000);
    options(bootstrap);
    bootstrap.remoteAddress(address());
    bootstrap.handler(init());
    future = bootstrap.connect();
    if (listeners() != null) {
      ChannelFutureListener[] arr = listeners()
          .toArray(new ChannelFutureListener[0]);
      future.addListeners(arr);
    }
    return future;
  }

//  @Override
//  public Class<? extends Channel> channelClass() {
//    return NioSocketChannel.class;
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
