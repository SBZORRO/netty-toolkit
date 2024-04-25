package mqtt.core;

import java.util.Random;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttVersion;

public final class BeanMqttClientConfig {
  private final String randomClientId;

  private String clientId;
  private int timeoutSeconds = 100;
  private MqttVersion protocolVersion = MqttVersion.MQTT_3_1_1;
  private String username = null;
  private String password = null;
  private boolean cleanSession = false;
  private BeanMqttLastWill lastWill;

  ChannelInitializer<SocketChannel> init;

  public BeanMqttClientConfig() {
    Random random = new Random();
    String id = "netty-mqtt/";
    String[] options = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
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

  public BeanMqttLastWill getLastWill() {
    return lastWill;
  }

  public void setLastWill(BeanMqttLastWill lastWill) {
    this.lastWill = lastWill;
  }
}
