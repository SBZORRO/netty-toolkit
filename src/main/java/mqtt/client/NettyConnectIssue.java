package mqtt.client;


import java.io.File;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

public class NettyConnectIssue {
  private static final boolean SSL = true;
  private static final String HOST = "127.0.0.1";
  private static final int PORT = 12345;

  public static void main(String[] args) throws Exception {
    java.security.Security
        .addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

    System.out.println("Working dir " + new File(".").getAbsolutePath());
    // Configure SSL.git
    final SslContext sslCtx;
    if (SSL) {

      sslCtx = SslContextBuilder.forClient().sslProvider(SslProvider.OPENSSL)
          .trustManager(new File("certs/ca.pem"))
          .keyManager(new File("certs/client.pem"),
              new File("certs/client.key"))
          .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
          .sessionCacheSize(0).sessionTimeout(0).build();
    } else {
      sslCtx = null;
    }

    // Configure the client.
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      Bootstrap b = new Bootstrap();
      b.group(group).channel(NioSocketChannel.class)
          .option(ChannelOption.TCP_NODELAY, true)
          .handler(new SocketChannelChannelInitializer(sslCtx));

      // Start the client.

      ChannelFuture connect = b.connect(HOST, PORT).sync();
      connect.channel().closeFuture().sync();

    } finally {
      // Shut down the event loop to terminate all threads.
      group.shutdownGracefully();
    }

    System.out.println("Main thread complete.");
  }

  private static class SocketChannelChannelInitializer
      extends ChannelInitializer<SocketChannel> {
    private final SslContext sslCtx;

    public SocketChannelChannelInitializer(SslContext sslCtx) {
      this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {

      ChannelPipeline p = ch.pipeline();
      if (sslCtx != null) {
        p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
      }
      p.addLast(new ClientHandler());
    }
  }

}
