package mqtt.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.sbzorro.LogUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import tcp.client.TcpClient;

public class MqttClientImpl {

  final IntObjectHashMap<RtnsHandler<MqttPublishMessage>> pendingPublishes = new IntObjectHashMap<>();
  final IntObjectHashMap<RtnsHandler<MqttMessage>> pendingPubreles = new IntObjectHashMap<>();

  final IntObjectHashMap<RtnsHandler<MqttUnsubscribeMessage>> pendingUnsubs = new IntObjectHashMap<>();
  final IntObjectHashMap<RtnsHandler<MqttSubscribeMessage>> pendingSubs = new IntObjectHashMap<>();

  private final Set<String> subStrSet = new HashSet<>();
  private final Set<BeanSub> subBeanSet = new HashSet<>();

  private final Map<String, BeanSub> topicToSub = new ConcurrentHashMap<>();

  private final AtomicInteger nextMessageId = new AtomicInteger(1);

  protected Promise<BeanMqttConnectResult> connectFuture;

  TcpClient tcpClient;
  BeanMqttClientConfig config;

  public MqttClientImpl(TcpClient tcpClient, BeanMqttClientConfig config) {
    this.tcpClient = tcpClient;
    this.config = config;
  }

  public Channel channel() {
    return tcpClient.channel();
  }

  public Future<Void> on(String topic, IMqttHandler handler) {
    return on(topic, handler, MqttQoS.AT_MOST_ONCE);
  }

  public Future<Void> on(String topic, IMqttHandler handler, MqttQoS qos) {
    return createSubscribtion(topic, handler, qos);
  }

  public Future<Void> off(String topic, IMqttHandler handler) {
    BeanSub sub = topicToSub.get(topic);
    sub.handler().remove(handler);

    if (sub.handler().isEmpty()) {
      subStrSet.remove(topic);
      subBeanSet.remove(sub);
      topicToSub.remove(topic);
    }
    return this.sendUnsub(topic);
  }

  public Future<Void> off(String topic) {
    subStrSet.remove(topic);
    subBeanSet.remove(topicToSub.remove(topic));;
    return this.sendUnsub(topic);
  }

  public Future<Void> publish(String topic, ByteBuf payload) {
    return sendPub(topic, payload, MqttQoS.AT_MOST_ONCE, false);
  }

  public Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos) {
    return sendPub(topic, payload, qos, false);
  }

  public Future<Void> publish(String topic, ByteBuf payload, boolean retain) {
    return sendPub(topic, payload, MqttQoS.AT_MOST_ONCE, retain);
  }

  public static AtomicInteger total = new AtomicInteger(100);

  public Future<Void>
      sendPub(String topic, ByteBuf payload, MqttQoS qos, boolean retain) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH,
        false, qos, retain, 0);
    MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(
        topic, qos.value() > 0 ? getMessageId() : 0);
    MqttPublishMessage message = new MqttPublishMessage(fixedHeader,
        variableHeader, payload);

    if (qos == MqttQoS.AT_MOST_ONCE) {
      return this.tcpClient.writeAndFlush(message);
    } else {
      return this.tcpClient.writeAndFlush(message)
          .addListener((ChannelFutureListener) f -> {
            LogUtil.SOCK.info("pub::::"
                + total.incrementAndGet()
                + "\n"
                + "pckt: "
                + message.variableHeader().packetId());

            System.out.println("pendingPublishes: "
                + pendingPublishes.size()
                + "\npendingPubreles: "
                + pendingPubreles.size());
            pendingPublishes.put(message.variableHeader().packetId(),
                RetransmissionHandlerFactory.newPublishHandler(f, message));
          });
    }
  }

  // store sub directly, set active = false
  private Future<Void>
      createSubscribtion(String topic, IMqttHandler handler, MqttQoS qos) {
    BeanSub subscribtion = null;
    if (!subStrSet.contains(topic)) {
      subscribtion = new BeanSub(topic, handler);
      subStrSet.add(topic);
      subBeanSet.add(subscribtion);
      topicToSub.put(topic, subscribtion);
    }
    return this.sendSub(topic, qos);
  }

  private ChannelFuture sendSub(String topic, MqttQoS qos) {
    // sub qos must be one
    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttTopicSubscription subscription = new MqttTopicSubscription(topic, qos);
    MqttMessageIdVariableHeader variableHeader = getNewMessageIdVariableHeader();
    MqttSubscribePayload payload = new MqttSubscribePayload(
        Collections.singletonList(subscription));
    MqttSubscribeMessage message = new MqttSubscribeMessage(fixedHeader,
        variableHeader, payload);

    return this.tcpClient.writeAndFlush(message)
        .addListener((ChannelFutureListener) f -> {
          pendingSubs.put(message.variableHeader().messageId(),
              RetransmissionHandlerFactory.newSubscribeHandler(f, message));
        });
  }

  private ChannelFuture sendUnsub(String topic) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = getNewMessageIdVariableHeader();
    MqttUnsubscribePayload payload = new MqttUnsubscribePayload(
        Collections.singletonList(topic));
    MqttUnsubscribeMessage message = new MqttUnsubscribeMessage(fixedHeader,
        variableHeader, payload);

    return this.tcpClient.writeAndFlush(message)
        .addListener((ChannelFutureListener) f -> {
          pendingUnsubs.put(message.variableHeader().messageId(),
              RetransmissionHandlerFactory.newUnsubscribeHandler(f, message));
        });
  }

  private int getMessageId() {
    int messageId = this.nextMessageId.getAndIncrement();
    while ((messageId & 0x0000ffff) == 0) {
      messageId = this.nextMessageId.getAndIncrement();
    }
    return messageId & 0x0000ffff;
  }

  private MqttMessageIdVariableHeader getNewMessageIdVariableHeader() {
    int messageId = this.nextMessageId.getAndIncrement();
    while ((messageId & 0x0000ffff) == 0) {
      messageId = this.nextMessageId.getAndIncrement();
    }
    return MqttMessageIdVariableHeader.from(messageId & 0x0000ffff);
  }

  public IntObjectHashMap<RtnsHandler<MqttUnsubscribeMessage>>
      pendingUnsubs() {
    return pendingUnsubs;
  }

  public IntObjectHashMap<RtnsHandler<MqttSubscribeMessage>> pendingSubs() {
    return pendingSubs;
  }

  public IntObjectHashMap<RtnsHandler<MqttPublishMessage>> pendingPublishes() {
    return pendingPublishes;
  }

  public IntObjectHashMap<RtnsHandler<MqttMessage>> pendingPubreles() {
    return pendingPubreles;
  }

  public Map<String, BeanSub> topicToSub() {
    return topicToSub;
  }

  public Set<String> subStrSet() {
    return subStrSet;
  }

  public Set<BeanSub> subBeanSet() {
    return subBeanSet;
  }

  public AtomicInteger getNextMessageId() {
    return nextMessageId;
  }

  public Promise<BeanMqttConnectResult> connectFuture() {
    return connectFuture;
  }

  public void connectFuture(Promise<BeanMqttConnectResult> f) {
    this.connectFuture = f;
  }

  public MqttConnectVariableHeader mqttConnectVariableHeader() {

    return new MqttConnectVariableHeader(
        config.getProtocolVersion().protocolName(),  // Protocol Name
        config.getProtocolVersion().protocolLevel(), // Protocol Level
        config.getUsername() != null,                // Has Username
        config.getPassword() != null,                // Has Password
        config.getLastWill() != null                 // Will Retain
            && config.getLastWill().isRetain(),
        config.getLastWill() != null                 // Will QOS
            ? config.getLastWill().getQos().value()
            : 0,
        config.getLastWill() != null,                // Has Will
        config.isCleanSession(),                     // Clean Session
        config.getTimeoutSeconds()                   // Timeout
    );
  }

  public MqttConnectPayload mqttConnectPayload() {
    return new MqttConnectPayload(config.getClientId(),
        config.getLastWill() != null ? config.getLastWill().getTopic() : null,
        config.getLastWill() != null
            ? config.getLastWill().getMessage().getBytes()
            : new byte[0],
        config.getUsername(),
        config.getPassword() != null
            ? config.getPassword().getBytes()
            : new byte[0]);
  }
}
