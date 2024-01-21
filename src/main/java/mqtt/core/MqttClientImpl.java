package mqtt.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import tcp.client.TcpClient;

public class MqttClientImpl {

  private final IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>> pendingQos2IncomingPublishes = new IntObjectHashMap<>();
  private final IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>> pendingPublishes = new IntObjectHashMap<>();
  private final IntObjectHashMap<RetransmissionHandler<MqttMessage>> pendingPubreles = new IntObjectHashMap<>();

  private final IntObjectHashMap<RetransmissionHandler<MqttUnsubscribeMessage>> pendingServerUnsubscribes = new IntObjectHashMap<>();
  private final IntObjectHashMap<RetransmissionHandler<MqttSubscribeMessage>> pendingSubscribtions = new IntObjectHashMap<>();

  private final Set<String> serverSubscribtions = new HashSet<>();

  private final Map<String, Set<BeanMqttSubscribtion>> topicToSubscriptions = new ConcurrentHashMap<>();
  private final Map<IMqttHandler, Set<BeanMqttSubscribtion>> handlerToSubscribtion = new ConcurrentHashMap<>();

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
    return createSubscribtion(topic, handler, false, qos);
  }

  public Future<Void> once(String topic, IMqttHandler handler) {
    return once(topic, handler, MqttQoS.AT_MOST_ONCE);
  }

  public Future<Void> once(String topic, IMqttHandler handler, MqttQoS qos) {
    return createSubscribtion(topic, handler, true, qos);
  }

  public Future<Void> off(String topic, IMqttHandler handler) {
    Set<BeanMqttSubscribtion> topicSet = topicToSubscriptions().get(topic);
    for (BeanMqttSubscribtion sub : topicSet) {
      topicSet.remove(sub);
    }

    Set<BeanMqttSubscribtion> handlerSet = handlerToSubscribtion()
        .get(handler);
    for (BeanMqttSubscribtion sub : handlerSet) {
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

    Set<BeanMqttSubscribtion> subscribtions = this.topicToSubscriptions
        .remove(topic);

    for (BeanMqttSubscribtion subscribtion : subscribtions) {
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
    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH,
        false, qos, retain, 0);
    MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(
        topic, qos.value() > 0 ? getNewMessageIdVariableHeader().messageId() : 0);
    MqttPublishMessage message = new MqttPublishMessage(fixedHeader,
        variableHeader, payload);

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
      String topic, IMqttHandler handler, boolean once, MqttQoS qos) {
    // directly set sub
    BeanMqttSubscribtion subscribtion = new BeanMqttSubscribtion(topic,
        handler, once);
    this.topicToSubscriptions.putIfAbsent(topic, new HashSet<>());
    topicToSubscriptions.get(topic).add(subscribtion);

    this.handlerToSubscribtion.putIfAbsent(handler, new HashSet<>());
    handlerToSubscribtion.get(handler).add(subscribtion);

    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.SUBSCRIBE, false, qos, false, 0);
    MqttTopicSubscription subscription = new MqttTopicSubscription(topic, qos);
    MqttMessageIdVariableHeader variableHeader = getNewMessageIdVariableHeader();
    MqttSubscribePayload payload = new MqttSubscribePayload(
        Collections.singletonList(subscription));
    MqttSubscribeMessage message = new MqttSubscribeMessage(fixedHeader,
        variableHeader, payload);

    return this.tcpClient.writeAndFlush(message)
        .addListener((ChannelFutureListener) f -> {
          pendingSubscribtions.put(message.variableHeader().messageId(),
              RetransmissionHandlerFactory.newSubscribeHandler(f, message));
        });
  }

  private ChannelFuture checkSubscribtions(String topic) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = getNewMessageIdVariableHeader();
    MqttUnsubscribePayload payload = new MqttUnsubscribePayload(
        Collections.singletonList(topic));
    MqttUnsubscribeMessage message = new MqttUnsubscribeMessage(fixedHeader,
        variableHeader, payload);

    return this.tcpClient.writeAndFlush(message)
        .addListener((ChannelFutureListener) f -> {
          pendingServerUnsubscribes.put(message.variableHeader().messageId(),
              RetransmissionHandlerFactory.newUnsubscribeHandler(f, message));
        });
  }

  private MqttMessageIdVariableHeader getNewMessageIdVariableHeader() {
    this.nextMessageId.compareAndSet(0xffff, 1);
    return MqttMessageIdVariableHeader
        .from(this.nextMessageId.getAndIncrement());
  }

  public Map<String, Set<BeanMqttSubscribtion>> topicToSubscriptions() {
    return topicToSubscriptions;
  }

  public Map<IMqttHandler, Set<BeanMqttSubscribtion>> handlerToSubscribtion() {
    return handlerToSubscribtion;
  }

  public Set<String> getServerSubscribtions() {
    return serverSubscribtions;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttUnsubscribeMessage>>
      pendingServerUnsubscribes() {
    return pendingServerUnsubscribes;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttSubscribeMessage>>
      pendingSubscribtions() {
    return pendingSubscribtions;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>>
      getPendingPublishes() {
    return pendingPublishes;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttMessage>>
      pendingPubreles() {
    return pendingPubreles;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>>
      pendingQos2IncomingPublishes() {
    return pendingQos2IncomingPublishes;
  }

  public Map<String, Set<BeanMqttSubscribtion>> getTopicToSubscriptions() {
    return topicToSubscriptions;
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
