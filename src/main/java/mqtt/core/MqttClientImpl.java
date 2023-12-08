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

//  public EventLoop executer() {
//    return channel().eventLoop().next();
//  }

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

//    this.tcpClient.writeAndFlush(message) != null
    if (qos == MqttQoS.AT_MOST_ONCE) {
      return this.tcpClient.writeAndFlush(message);
    } else {
      return this.tcpClient.writeAndFlush(message)
          .addListener((ChannelFutureListener) f -> {
            pendingPublishes.put(message.variableHeader().packetId(),
                MqttPendingPublish.newPublishHandler(f, message));
          });
    }
//    Promise<Void> future = new DefaultPromise<>(executer());
//
//    MqttPendingPublish pendingPublish = new MqttPendingPublish(
//        variableHeader.packetId(), future, payload.retain(), message, qos);
//    pendingPublish.setSent(this.tcpClient.writeAndFlush(message) != null);
//
//    if (pendingPublish.isSent()
//        && pendingPublish.getQos() == MqttQoS.AT_MOST_ONCE) {
//      pendingPublish.getFuture().setSuccess(null); // We don't get an ACK for QOS 0
//    } else if (pendingPublish.isSent()) {
//      this.pendingPublishes.put(pendingPublish.getMessageId(), pendingPublish);
//      pendingPublish.startPublishRetransmissionTimer(executer(),
//          this::sendAndFlushPacket);
//    }

//    return future;
  }

  // store sub directly, set active = false
  private Future<Void> createSubscribtion(
      String topic, MqttHandler handler, boolean once, MqttQoS qos) {
//    if (this.pendingSubscribeTopics.contains(topic)) {
//      Optional<Map.Entry<Integer, MqttPendingSubscribtion>> subscribtionEntry
//          = this.pendingSubscribtions.entrySet().stream()
//              .filter((e) -> e.getValue().getTopic().equals(topic)).findAny();
//      if (subscribtionEntry.isPresent()) {
//        subscribtionEntry.get().getValue().addHandler(handler, once);
//        return subscribtionEntry.get().getValue().getFuture();
//      }
//    }

//    if (this.pendingSubscribtions.containsValue(topic)) {
////    pendingSubscribtions.get(topic).getTimer();
//      return (Future<Void>) pendingSubscribtions.get(topic).getTimer();
//    }

    // directly set sub
//    if (this.serverSubscribtions.contains(topic)) {
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
//    return channel().newSucceededFuture();
//    }

//    Promise<Void> future = new DefaultPromise<>(executer());
    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttTopicSubscription subscription
        = new MqttTopicSubscription(topic, qos);
    MqttMessageIdVariableHeader variableHeader = getNewMessageId();
    MqttSubscribePayload payload
        = new MqttSubscribePayload(Collections.singletonList(subscription));
    MqttSubscribeMessage message
        = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);

//    this.pendingSubscribeTopics.add(topic);

//    final MqttPendingSubscribtion pendingSubscribtion
//    = new MqttPendingSubscribtion(future, topic, message);

//      pendingSubscribtion.addHandler(handler, once);

//    this.pendingSubscribtions.put(variableHeader.messageId(),
//        pendingSubscribtion);
//    this.tcpClient.writeAndFlush(message) != null
//    pendingSubscribtion.setSent(this.tcpClient.writeAndFlush(message) != null); // If not sent, we will send it when the connection is opened
//  pendingSubscribtion.startRetransmitTimer(executer(),
//  this::sendAndFlushPacket);
//    return future;

    return this.tcpClient.writeAndFlush(message)
        .addListener((ChannelFutureListener) f -> {
          pendingSubscribtions.put(message.variableHeader().messageId(),
              MqttPendingPublish.newSubscribeHandler(f, message));
        });

  }

  private ChannelFuture checkSubscribtions(String topic) {
//    if (!(this.topicToSubscriptions.containsKey(topic)
//        && this.topicToSubscriptions.get(topic).size() != 0)
//        && this.serverSubscribtions.contains(topic)) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = getNewMessageId();
    MqttUnsubscribePayload payload
        = new MqttUnsubscribePayload(Collections.singletonList(topic));
    MqttUnsubscribeMessage message
        = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);

//      MqttPendingUnsubscribtion pendingUnsubscribtion
//          = new MqttPendingUnsubscribtion(promise, topic, message);
//      this.pendingServerUnsubscribes.put(variableHeader.messageId(),
//          pendingUnsubscribtion);
//      pendingUnsubscribtion.startRetransmissionTimer(executer(),
//          this::sendAndFlushPacket);

//      this.sendAndFlushPacket(message);

    return this.tcpClient.writeAndFlush(message)
        .addListener((ChannelFutureListener) f -> {
          pendingServerUnsubscribes.put(message.variableHeader().messageId(),
              MqttPendingPublish.newUnsubscribeHandler(f, message));
        });
//    } else {
//      promise.setSuccess(null);
//    }
  }

//public ChannelFuture sendAndFlushPacket(Object message) {
//  if (channel() == null) {
//    return null;
//  }
//  if (channel().isActive()) {
//    return channel().writeAndFlush(message);
//  }
//  return channel()
//      .newFailedFuture(new RuntimeException("Channel is closed"));
//}

  private MqttMessageIdVariableHeader getNewMessageId() {
    this.nextMessageId.compareAndSet(0xffff, 1);
    return MqttMessageIdVariableHeader
        .from(this.nextMessageId.getAndIncrement());
  }

  public Map<String, Set<MqttSubscribtion>> topicToSubscriptions() {
    return topicToSubscriptions;
  }

//  public Set<String> getPendingSubscribeTopics() {
//    return pendingSubscribeTopics;
//  }

  public Map<MqttHandler, Set<MqttSubscribtion>> handlerToSubscribtion() {
    return handlerToSubscribtion;
  }

  public Set<String> getServerSubscribtions() {
    return serverSubscribtions;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttUnsubscribeMessage>> pendingServerUnsubscribes() {
    return pendingServerUnsubscribes;
  }

//public IntObjectHashMap<MqttPendingSubscribtion> getPendingSubscribtions() {
//return pendingSubscribtions;
//}

  public IntObjectHashMap<RetransmissionHandler<MqttSubscribeMessage>> pendingSubscribtions() {
    return pendingSubscribtions;
  }

//  public IntObjectHashMap<MqttPendingPublish> getPendingPublishes() {
//    return pendingPublishes;
//  }

  public IntObjectHashMap<RetransmissionHandler<MqttPublishMessage>> getPendingPublishes() {
    return pendingPublishes;
  }

  public IntObjectHashMap<RetransmissionHandler<MqttMessage>> pendingPubreles() {
    return pendingPubreles;
  }

//  public IntObjectHashMap<MqttIncomingQos2Publish> getQos2PendingIncomingPublishes() {
//    return qos2PendingIncomingPublishes;
//  }
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
