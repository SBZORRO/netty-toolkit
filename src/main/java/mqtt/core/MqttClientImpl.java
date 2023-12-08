package mqtt.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
import mqtt.client.TcpClient;

public class MqttClientImpl {

  private final IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>> pendingQos2IncomingPublishes
      = new IntObjectHashMap<>();
  private final IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>> pendingPublishes
      = new IntObjectHashMap<>();
  private final IntObjectHashMap<RetransmissionHandler<MqttMessage>> pendingPubreles
      = new IntObjectHashMap<>();

  private final IntObjectHashMap<RetransmissionHandler<MqttUnsubscribeMessage>> pendingServerUnsubscribes
      = new IntObjectHashMap<>();
  private final IntObjectHashMap<RetransmissionHandler<MqttSubscribeMessage>> pendingSubscribtions
      = new IntObjectHashMap<>();

  private final Set<String> serverSubscribtions = new HashSet<>();

  private final HashMap<String, Set<MqttSubscribtion>> topicToSubscriptions
      = new HashMap<>();
  private final HashMap<MqttHandler, Set<MqttSubscribtion>> handlerToSubscribtion
      = new HashMap<>();

  private final AtomicInteger nextMessageId = new AtomicInteger(1);

  protected Promise<MqttConnectResult> connectFuture;

  TcpClient tcpClient;
  MqttClientConfig config;

  public MqttClientImpl(TcpClient tcpClient, MqttClientConfig config) {
    this.tcpClient = tcpClient;
    this.config = config;
  }

  public Channel channel() {
    return tcpClient.channel();
  }

  public Future<Void> on(String topic, MqttHandler handler) {
    return on(topic, handler, MqttQoS.AT_LEAST_ONCE);
  }

  public Future<Void> on(String topic, MqttHandler handler, MqttQoS qos) {
    return createSubscribtion(topic, handler, false, qos);
  }

  public Future<Void> once(String topic, MqttHandler handler) {
    return once(topic, handler, MqttQoS.AT_MOST_ONCE);
  }

  public Future<Void> once(String topic, MqttHandler handler, MqttQoS qos) {
    return createSubscribtion(topic, handler, true, qos);
  }

  public Future<Void> off(String topic, MqttHandler handler) {
    Set<MqttSubscribtion> topicSet = topicToSubscriptions().get(topic);
    for (MqttSubscribtion sub : topicSet) {
      topicSet.remove(sub);
    }

    Set<MqttSubscribtion> handlerSet = handlerToSubscribtion().get(handler);
    for (MqttSubscribtion sub : handlerSet) {
      handlerSet.remove(sub);
    }

    if (topicSet.isEmpty()) {
      topicToSubscriptions.remove(topic);
      serverSubscribtions.remove(topic);
    }

    if (handlerSet.isEmpty()) {
      handlerToSubscribtion().remove(handler);
    }

    return this.checkSubscribtions(topic);
  }

  public Future<Void> off(String topic) {
    this.getServerSubscribtions().remove(topic);

    Set<MqttSubscribtion> subscribtions
        = this.topicToSubscriptions.remove(topic);

    for (MqttSubscribtion subscribtion : subscribtions) {
      this.handlerToSubscribtion.get(subscribtion.getHandler())
          .remove(subscribtion);
    }

    return this.checkSubscribtions(topic);
  }

  public Future<Void> publish(String topic, ByteBuf payload) {
    return publish(topic, payload, MqttQoS.AT_MOST_ONCE, false);
  }

  public Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos) {
    return publish(topic, payload, qos, false);
  }

  public Future<Void> publish(String topic, ByteBuf payload, boolean retain) {
    return publish(topic, payload, MqttQoS.AT_MOST_ONCE, retain);
  }

  public Future<Void> publish(
      String topic, ByteBuf payload, MqttQoS qos, boolean retain) {
    MqttFixedHeader fixedHeader
        = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retain, 0);
    MqttPublishVariableHeader variableHeader
        = new MqttPublishVariableHeader(topic,
            qos.value() > 0 ? getNewMessageId().messageId() : 0);
    MqttPublishMessage message
        = new MqttPublishMessage(fixedHeader, variableHeader, payload);

    if (qos == MqttQoS.AT_MOST_ONCE) {
      return this.tcpClient.writeAndFlush(message);
    } else {
      return this.tcpClient.writeAndFlush(message)
          .addListener((ChannelFutureListener) f -> {
            pendingPublishes.put(message.variableHeader().packetId(),
                RetransmissionHandlerFactory.newPublishHandler(f, message));
          });
    }
  }

  // store sub directly, set active = false
  private Future<Void> createSubscribtion(
      String topic, MqttHandler handler, boolean once, MqttQoS qos) {
//    if (this.pendingSubscribtions.containsValue(topic)) {
////    pendingSubscribtions.get(topic).getTimer();
//      return (Future<Void>) pendingSubscribtions.get(topic).getTimer();
//    }

    // directly set sub
    MqttSubscribtion subscribtion
        = new MqttSubscribtion(topic, handler, once);
    if (topicToSubscriptions.containsKey(topic)) {
      topicToSubscriptions.get(topic).add(subscribtion);
    } else {
      Set<MqttSubscribtion> li = new HashSet<>();
      li.add(subscribtion);
      this.topicToSubscriptions.put(topic, li);
    }
    if (handlerToSubscribtion.containsKey(handler)) {
      handlerToSubscribtion.get(handler).add(subscribtion);
    } else {
      Set<MqttSubscribtion> li = new HashSet<>();
      li.add(subscribtion);
      this.handlerToSubscribtion.put(handler, li);
    }

    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttTopicSubscription subscription
        = new MqttTopicSubscription(topic, qos);
    MqttMessageIdVariableHeader variableHeader = getNewMessageId();
    MqttSubscribePayload payload
        = new MqttSubscribePayload(Collections.singletonList(subscription));
    MqttSubscribeMessage message
        = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);

    return this.tcpClient.writeAndFlush(message)
        .addListener((ChannelFutureListener) f -> {
          pendingSubscribtions.put(message.variableHeader().messageId(),
              RetransmissionHandlerFactory.newSubscribeHandler(f, message));
        });
  }

  private ChannelFuture checkSubscribtions(String topic) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = getNewMessageId();
    MqttUnsubscribePayload payload
        = new MqttUnsubscribePayload(Collections.singletonList(topic));
    MqttUnsubscribeMessage message
        = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);

    return this.tcpClient.writeAndFlush(message)
        .addListener((ChannelFutureListener) f -> {
          pendingServerUnsubscribes.put(message.variableHeader().messageId(),
              RetransmissionHandlerFactory.newUnsubscribeHandler(f, message));
        });
  }

  private MqttMessageIdVariableHeader getNewMessageId() {
    this.nextMessageId.compareAndSet(0xffff, 1);
    return MqttMessageIdVariableHeader
        .from(this.nextMessageId.getAndIncrement());
  }

  public Map<String, Set<MqttSubscribtion>> topicToSubscriptions() {
    return topicToSubscriptions;
  }

  public Map<MqttHandler, Set<MqttSubscribtion>> handlerToSubscribtion() {
    return handlerToSubscribtion;
  }

  public Set<String> getServerSubscribtions() {
    return serverSubscribtions;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttUnsubscribeMessage>> pendingServerUnsubscribes() {
    return pendingServerUnsubscribes;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttSubscribeMessage>> pendingSubscribtions() {
    return pendingSubscribtions;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>> getPendingPublishes() {
    return pendingPublishes;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttMessage>> pendingPubreles() {
    return pendingPubreles;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>> pendingQos2IncomingPublishes() {
    return pendingQos2IncomingPublishes;
  }

  public Map<String, Set<MqttSubscribtion>> getTopicToSubscriptions() {
    return topicToSubscriptions;
  }

  public AtomicInteger getNextMessageId() {
    return nextMessageId;
  }

  public Promise<MqttConnectResult> connectFuture() {
    return connectFuture;
  }

  public void connectFuture(Promise<MqttConnectResult> f) {
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
        config.getPassword() != null ? config.getPassword().getBytes()
            : new byte[0]);
  }
}
