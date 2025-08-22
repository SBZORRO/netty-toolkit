package udp.client;

import java.net.InetSocketAddress;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import tcp.client.NettyWrapper;

public class UdpClient extends NettyWrapper {

  public UdpClient() {}

  public UdpClient(String host, int port) {
    this.ip = host;
    this.port = port;
  }

  public UdpClient(int port) {
    this.port = port;
    this.ip = "127.0.0.1";
  }

  @Override
  public ChannelFuture bootstrap() {
    Bootstrap bootstrap = new Bootstrap()
        .group(singleGroup())
        .channel(NioDatagramChannel.class)
        .option(EpollChannelOption.SO_REUSEADDR, true)
        .option(EpollChannelOption.SO_REUSEPORT, true)
        .handler(init());
    future = bootstrap.bind(port);

    listen();

    return future;
  }

  @Override
  public void send(String msg) throws InterruptedException {
    NettyWrapper.LAST_CMD.put(this.host(), msg);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, this.host() + " <<< " + msg);

    if (HexByteUtil.isHex(msg)) {
      this.writeAndFlush(new DatagramPacket(
          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg)),
          new InetSocketAddress(this.ip(), this.port())))
          .sync();
    } else {
      this.writeAndFlush(new DatagramPacket(
          Unpooled.copiedBuffer(msg.getBytes()),
          new InetSocketAddress(this.ip(), this.port())))
          .sync();
    }
  }
}
