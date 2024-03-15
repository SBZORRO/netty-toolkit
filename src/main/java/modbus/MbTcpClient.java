package modbus;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import tcp.client.ClientFactory;
import tcp.client.ITcpReader;
import tcp.client.NettyWrapper;
import tcp.client.TcpChannelHandler;
import tcp.client.TcpClient;
import tcp.client.TcpConnectionListener;
import tcp.client.TcpReconnectHandler;

public class MbTcpClient {

  public static void main(String[] args) {}

  public static TcpClient connect(String ip, int port, ITcpReader reader)
      throws InterruptedException {

    NettyWrapper client = new TcpClient(ip, port);
    return (TcpClient) ClientFactory.bootstrap(client, new MyInitializer())
        .addListener(new TcpConnectionListener())
        .addHandler("reader", new TcpChannelHandler(reader));
  }

  static class MyInitializer extends ChannelInitializer<NioSocketChannel> {

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline()
          .addLast(new LengthFieldBasedFrameDecoder(1024, 4, 2, 0, 0));
      ch.pipeline().addLast(new TcpReconnectHandler());
      ch.pipeline().addLast(new ChannelOutboundHandlerAdapter());
    }
  }
}
