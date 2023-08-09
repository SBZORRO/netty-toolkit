package mqtt.client;

import java.util.concurrent.LinkedBlockingDeque;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Promise;
import mqtt.core.MqttChannelHandler;
import mqtt.core.MqttClientImpl;
import mqtt.core.MqttConnectResult;
import mqtt.core.MqttHandler;
import mqtt.core.MqttPingHandler;

public class MyMqttClient extends MqttClientImpl {

  public final LinkedBlockingDeque<Object[]> messageQueue
      = new LinkedBlockingDeque<>();

  public Promise<MqttConnectResult> connect(String host) {
    return connect(host, 1883);
  }

  public Promise<MqttConnectResult> connect(String host, int port) {
    if (this.eventLoop == null) {
      this.eventLoop = new NioEventLoopGroup(1);
    }
//    connectFuture = new DefaultPromise<>(this.eventLoop.next());
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(this.eventLoop);
    bootstrap.channel(clientConfig.getChannelClass());
    bootstrap.remoteAddress(host, port);
    bootstrap.handler(new MqttChannelInitializer());
    tcpFuture = bootstrap.connect();
    tcpFuture.addListener(new ConnectionListener());
//    tcpFuture.addListener(
//        (ChannelFutureListener) f -> MyMqttClient.this.channel = f.channel());
    mqttFuture = this.eventLoop.next().newPromise();
    return mqttFuture;
  }

  public void publish(String topic, String msg) {
    if (mqttFuture.isSuccess()) {
      publish(topic, Unpooled.copiedBuffer(msg.getBytes()));
    }
  }

  public void subscribe(String topic, MqttHandler handler) {
    on(topic, handler);
  }

  public void subscribe(String topic) {
    on(topic, (t, payload) -> {
      byte[] array = new byte[payload.readableBytes()];
      payload.getBytes(0, array);
      messageQueue.addLast(new Object[] { t, new String(array) });
    });
  }

  private class MqttChannelInitializer
      extends ChannelInitializer<SocketChannel> {

    protected void initChannel(SocketChannel ch) throws Exception {
      ch.pipeline().addLast("log4j", new LoggingHandler());
      ch.pipeline().addLast("mqttDecoder", new MqttDecoder());
      ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
      ch.pipeline().addLast("idleStateHandler",
          new IdleStateHandler(
              MyMqttClient.this.clientConfig.getTimeoutSeconds(),
              MyMqttClient.this.clientConfig.getTimeoutSeconds(), 0));
      ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(
          MyMqttClient.this.clientConfig.getTimeoutSeconds()));
      ch.pipeline().addLast("mqttHandler",
          new MqttChannelHandler(MyMqttClient.this));
    }
  }

  public void addHandler(String name, ChannelHandler handler) {
    tcpFuture.channel().pipeline().addLast(name, handler);
  }

  MqttHandler handler = new MqttHandler() {
    @Override
    public void onMessage(String topic, ByteBuf payload) {
      byte[] array = new byte[payload.readableBytes()];
      payload.getBytes(0, array);
      messageQueue.addLast(new Object[] { topic, new String(array) });
    }
  };

}
