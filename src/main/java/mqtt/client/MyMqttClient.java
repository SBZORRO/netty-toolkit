package mqtt.client;

import java.util.concurrent.LinkedBlockingDeque;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import mqtt.core.BeanMqttClientConfig;
import mqtt.core.IMqttHandler;
import mqtt.core.MqttChannelHandler;
import mqtt.core.MqttClientImpl;
import mqtt.core.MqttPingHandler;
import tcp.client.TcpClient;

public class MyMqttClient {
  public ChannelFuture future;

  private BeanMqttClientConfig config;

  MqttClientImpl impl;
  TcpClient tcpClient;

  public MyMqttClient() {
    this.config = new BeanMqttClientConfig();
    this.impl = new MqttClientImpl(tcpClient, config);
  }

  public BeanMqttClientConfig config() {
    return config;
  }

  public ChannelFuture connect(String host) {
    return connect(host, 1883);
  }

  public ChannelFuture connect(String host, int port) {
    this.tcpClient = new TcpClient(host, port);
    tcpClient.init(new MqttChannelInitializer());
    tcpClient.listeners(new MqttConnectionListener());
    future = tcpClient.bootstrap();
    return future;
  }

  public void publish(String topic, String msg) {
//    if (future.isSuccess()) {
    impl.publish(topic, Unpooled.copiedBuffer(msg.getBytes()));
//    }
  }

  public void subscribe(String topic, IMqttHandler handler) {
    impl.on(topic, handler);
  }

  private class MqttChannelInitializer extends ChannelInitializer<NioSocketChannel> {
    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast("log4j", new LoggingHandler());
      ch.pipeline().addLast("reconnector", new MqttReconnectHandler());
      ch.pipeline().addLast("mqttDecoder", new MqttDecoder());
      ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
      ch.pipeline().addLast("idleStateHandler",
          new IdleStateHandler(
              MyMqttClient.this.config.getTimeoutSeconds(),
              MyMqttClient.this.config.getTimeoutSeconds(), 0));
      ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(
          MyMqttClient.this.config.getTimeoutSeconds()));
      ch.pipeline().addLast("mqttHandler",
          new MqttChannelHandler(MyMqttClient.this.impl));
    }
  }

  public final LinkedBlockingDeque<Object[]> messageQueue = new LinkedBlockingDeque<>();

  public void subscribe(String topic) {
    impl.on(topic, (t, payload) -> {
      byte[] array = new byte[payload.readableBytes()];
      payload.getBytes(0, array);
      messageQueue.addLast(new Object[] { t, new String(array) });
    });
  }

  public void addHandler(String name, ChannelHandler handler) {
    impl.channel().pipeline().addLast(name, handler);
  }

  IMqttHandler handler = new IMqttHandler() {
    @Override
    public void onMessage(String topic, ByteBuf payload) {
      byte[] array = new byte[payload.readableBytes()];
      payload.getBytes(0, array);
      messageQueue.addLast(new Object[] { topic, new String(array) });
    }
  };

}
