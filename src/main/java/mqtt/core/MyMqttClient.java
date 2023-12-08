//package mqtt.core;
//
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.handler.codec.mqtt.MqttDecoder;
//import io.netty.handler.codec.mqtt.MqttEncoder;
//import io.netty.handler.logging.LoggingHandler;
//import io.netty.handler.timeout.IdleStateHandler;
//import io.netty.util.concurrent.Promise;
//import mqtt.client.MqttReconnectHandler;
//import mqtt.client.MyMqttClient;
//
//private class MqttChannelInitializer
//    extends ChannelInitializer<SocketChannel> {
//
//  private final Promise<MqttConnectResult> connectFuture;
//
//  MqttChannelInitializer(Promise<MqttConnectResult> connectFuture) {
//    this.connectFuture = connectFuture;
//  }
//
//  protected void initChannel(SocketChannel ch) throws Exception {
//    ch.pipeline().addLast("log4j", new LoggingHandler());
//    ch.pipeline().addLast("reconnector", new MqttReconnectHandler());
//    ch.pipeline().addLast("mqttDecoder", new MqttDecoder());
//    ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
//    ch.pipeline().addLast("idleStateHandler",
//        new IdleStateHandler(
//            MyMqttClient.this.clientConfig.getTimeoutSeconds(),
//            MyMqttClient.this.clientConfig.getTimeoutSeconds(), 0));
//    ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(
//        MyMqttClient.this.clientConfig.getTimeoutSeconds()));
//    ch.pipeline().addLast("mqttHandler",
//        new MqttChannelHandler(MyMqttClient.this, connectFuture));
//  }
//}
