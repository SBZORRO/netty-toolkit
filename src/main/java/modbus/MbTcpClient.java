package modbus;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import tcp.client.ClientFactory;
import tcp.client.NettyWrapper;
import tcp.client.TcpClient;
import tcp.client.TcpConnectionListener;
import tcp.client.TcpReconnectHandler;

public class MbTcpClient {

  public static void main(String[] args) {}

  public static TcpClient connect(String ip, int port)
      throws InterruptedException {
    NettyWrapper client = new TcpClient(ip, port);
    return (TcpClient) ClientFactory.bootstrap(client,
        new MyInitializer(),
        new TcpConnectionListener());
  }

  public static void connNotify(String ip, int port)
      throws InterruptedException {
    TcpClient.CLIENTS.put(ip + ":" + port, connect(ip, port));
  }

  public static void disconnNotify(String ip, int port) {
    TcpClient.CLIENTS.remove(ip + ":" + port).close();
  }

  public void shutdown(String ip, int port) {
    TcpClient.CLIENTS.remove(ip + ":" + port).close();
  }

  static class MyInitializer extends ChannelInitializer<NioSocketChannel> {
    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline()
          .addLast(new LengthFieldBasedFrameDecoder(1024, 4, 2, 0, 0));
      ch.pipeline().addLast(new TcpChannelHandler());
      ch.pipeline().addLast(new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }
}
