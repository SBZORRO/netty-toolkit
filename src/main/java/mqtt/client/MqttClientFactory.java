package mqtt.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import mqtt.core.BeanMqttClientConfig;
import mqtt.core.MqttChannelHandler;
import mqtt.core.MqttClientImpl;
import mqtt.core.MqttPingHandler;
import tcp.client.ClientFactory;
import tcp.client.TcpClient;

public class MqttClientFactory {

  public static MqttClientImpl bootstrap(String host, int port)
      throws InterruptedException {
    TcpClient tcpClient = new TcpClient(host, port);
    BeanMqttClientConfig config = new BeanMqttClientConfig();
    MqttClientImpl impl = new MqttClientImpl(tcpClient, config);

    ClientFactory.bootstrap(tcpClient,
        new MqttChannelInitializer(impl, config.getTimeoutSeconds()),
        new MqttConnectionListener());
    return impl;
  }

  private static class MqttChannelInitializer extends ChannelInitializer<NioSocketChannel> {
    MqttClientImpl impl;
    int timeout;

    MqttChannelInitializer(MqttClientImpl impl, int timeout) {
      this.impl = impl;
      this.timeout = timeout;
    }

    protected void initChannel(NioSocketChannel ch) throws Exception {
      ch.pipeline().addLast("log4j", new LoggingHandler());
      ch.pipeline().addLast("reconnector", new MqttReconnectHandler());
      ch.pipeline().addLast("mqttDecoder", new MqttDecoder());
      ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
      ch.pipeline().addLast("idleStateHandler",
          new IdleStateHandler(timeout, timeout, 0));
      ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(timeout));
      ch.pipeline().addLast("mqttHandler",
          new MqttChannelHandler(impl));
    }
  }

//  public final LinkedBlockingDeque<Object[]> messageQueue = new LinkedBlockingDeque<>();
//
//  public void subscribe(String topic) {
//    impl.on(topic, (t, payload) -> {
//      byte[] array = new byte[payload.readableBytes()];
//      payload.getBytes(0, array);
//      messageQueue.addLast(new Object[] { t, new String(array) });
//    });
//  }

//  IMqttHandler handler = new IMqttHandler() {
//    @Override
//    public void onMessage(String topic, ByteBuf payload) {
//      byte[] array = new byte[payload.readableBytes()];
//      payload.getBytes(0, array);
//      messageQueue.addLast(new Object[] { topic, new String(array) });
//    }
//  };

}
