package mqtt.core;

import java.util.Random;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import mqtt.client.MqttReconnectHandler;
import mqtt.client.MyMqttClient;

public final class MqttClientConfig {
  private final String randomClientId;

  private String clientId;
  private int timeoutSeconds = 100;
  private MqttVersion protocolVersion = MqttVersion.MQTT_3_1;
  private String username = null;
  private String password = null;
  private boolean cleanSession = false;
  private MqttLastWill lastWill;
  private Class<? extends Channel> channelClass = NioSocketChannel.class;

  ChannelInitializer<SocketChannel> init;

  private class MqttChannelInitializer
      extends ChannelInitializer<SocketChannel> {
    protected void initChannel(SocketChannel ch) throws Exception {
      ch.pipeline().addLast("log4j", new LoggingHandler());
      ch.pipeline().addLast("reconnector", new MqttReconnectHandler());
      ch.pipeline().addLast("mqttDecoder", new MqttDecoder());
      ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
      ch.pipeline().addLast("idleStateHandler",
          new IdleStateHandler(
              MqttClientConfig.this.getTimeoutSeconds(),
              MqttClientConfig.this.getTimeoutSeconds(), 0));
      ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(
          MqttClientConfig.this.getTimeoutSeconds()));
      ch.pipeline().addLast("mqttHandler",
          new MqttChannelHandler(MyMqttClient.this));
    }
  }

  public MqttConnectVariableHeader mqttConnectVariableHeader() {

    return new MqttConnectVariableHeader(
        getProtocolVersion().protocolName(),  // Protocol Name
        getProtocolVersion().protocolLevel(), // Protocol Level
        getUsername() != null,                // Has Username
        getPassword() != null,                // Has Password
        getLastWill() != null                 // Will Retain
            && getLastWill().isRetain(),
        getLastWill() != null                 // Will QOS
            ? getLastWill().getQos().value()
            : 0,
        getLastWill() != null,                // Has Will
        isCleanSession(),                     // Clean Session
        getTimeoutSeconds()                   // Timeout
    );
  }

  public MqttConnectPayload mqttConnectPayload() {
    return new MqttConnectPayload(getClientId(),
        getLastWill() != null ? getLastWill().getTopic() : null,
        getLastWill() != null ? getLastWill().getMessage().getBytes()
            : new byte[0],
        getUsername(),
        getPassword() != null ? getPassword().getBytes() : new byte[0]);
  }

  public MqttClientConfig() {
    Random random = new Random();
    String id = "netty-mqtt/";
    String[] options
        = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .split("");
    for (int i = 0; i < 8; i++) {
      id += options[random.nextInt(options.length)];
    }
    this.clientId = id;
    this.randomClientId = id;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    if (clientId == null) {
      this.clientId = randomClientId;
    } else {
      this.clientId = clientId;
    }
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    if (timeoutSeconds != -1 && timeoutSeconds <= 0) {
      throw new IllegalArgumentException("timeoutSeconds must be > 0 or -1");
    }
    this.timeoutSeconds = timeoutSeconds;
  }

  public MqttVersion getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(MqttVersion protocolVersion) {
    if (protocolVersion == null) {
      throw new NullPointerException("protocolVersion");
    }
    this.protocolVersion = protocolVersion;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isCleanSession() {
    return cleanSession;
  }

  public void setCleanSession(boolean cleanSession) {
    this.cleanSession = cleanSession;
  }

  public MqttLastWill getLastWill() {
    return lastWill;
  }

  public void setLastWill(MqttLastWill lastWill) {
    this.lastWill = lastWill;
  }

  public Class<? extends Channel> getChannelClass() {
    return channelClass;
  }

  public void setChannelClass(Class<? extends Channel> channelClass) {
    this.channelClass = channelClass;
  }
}
