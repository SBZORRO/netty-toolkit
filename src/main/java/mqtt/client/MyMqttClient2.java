package mqtt.client;

import java.util.concurrent.LinkedBlockingDeque;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import mqtt.core.MqttChannelHandler;
import mqtt.core.MqttClientConfig;
import mqtt.core.MqttClientImpl;
import mqtt.core.MqttConnectResult;
import mqtt.core.MqttHandler;
import mqtt.core.MqttPingHandler;

public class MyMqttClient2 {
  public Future<MqttConnectResult> future;

  private MqttClientConfig config;

  MqttClientImpl client = new MqttClientImpl(this);

  public MyMqttClient2(MqttClientConfig config) {
    this.config = config;
  }

  public MyMqttClient2() {
    this.config = new MqttClientConfig();
  }

  public MqttClientConfig config() {
    return config;
  }

  public Future<MqttConnectResult> connect(String host) {
    return connect(host, 1883);
  }

  public Future<MqttConnectResult> connect(String host, int port) {
    TcpClient client = new TcpClient();
    client.init(new MqttChannelInitializer());
    client.listeners(new MqttConnectionListener());
    ChannelFuture future = client.connect(host, port);

    return connectFuture;
  }

  public void publish(String topic, String msg) {
//    if (future.isSuccess()) {
    publish(topic, Unpooled.copiedBuffer(msg.getBytes()));
//    }
  }

  public void subscribe(String topic, MqttHandler handler) {
    on(topic, handler);
  }

  private class MqttChannelInitializer
      extends ChannelInitializer<SocketChannel> {
    protected void initChannel(SocketChannel ch) throws Exception {
      ch.pipeline().addLast("log4j", new LoggingHandler());
      ch.pipeline().addLast("reconnector", new MqttReconnectHandler());
      ch.pipeline().addLast("mqttDecoder", new MqttDecoder());
      ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
      ch.pipeline().addLast("idleStateHandler",
          new IdleStateHandler(
              MyMqttClient2.this.config.getTimeoutSeconds(),
              MyMqttClient2.this.config.getTimeoutSeconds(), 0));
      ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(
          MyMqttClient2.this.config.getTimeoutSeconds()));
      ch.pipeline().addLast("mqttHandler",
          new MqttChannelHandler(MyMqttClient2.this));
    }
  }

  public final LinkedBlockingDeque<Object[]> messageQueue
      = new LinkedBlockingDeque<>();

  public void subscribe(String topic) {
    on(topic, (t, payload) -> {
      byte[] array = new byte[payload.readableBytes()];
      payload.getBytes(0, array);
      messageQueue.addLast(new Object[] { t, new String(array) });
    });
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
