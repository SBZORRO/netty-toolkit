package mqtt.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
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
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * Represents an MqttClientImpl connected to a single MQTT server. Will try to
 * keep the connection going at all times
 */
public class MqttClientImpl {
  private final Set<String> serverSubscribtions = new HashSet<>();
  private final IntObjectHashMap<MqttPendingUnsubscribtion> pendingServerUnsubscribes
      = new IntObjectHashMap<>();
  private final IntObjectHashMap<MqttIncomingQos2Publish> qos2PendingIncomingPublishes
      = new IntObjectHashMap<>();
  private final IntObjectHashMap<MqttPendingPublish> pendingPublishes
      = new IntObjectHashMap<>();
  private final IntObjectHashMap<MqttPendingSubscribtion> pendingSubscribtions
      = new IntObjectHashMap<>();
  private final Set<String> pendingSubscribeTopics = new HashSet<>();

  private final HashMap<String, List<MqttSubscribtion>> topicToSubscriptions
      = new HashMap<>();
  private final HashMap<MqttHandler, List<MqttSubscribtion>> handlerToSubscribtion
      = new HashMap<>();

  private final AtomicInteger nextMessageId = new AtomicInteger(1);

  protected ChannelFuture tcpFuture;
  protected Promise<MqttConnectResult> connectFuture;

  public Map<String, List<MqttSubscribtion>> getTopicToSubscriptions() {
    return topicToSubscriptions;
  }

  public AtomicInteger getNextMessageId() {
    return nextMessageId;
  }

  public Promise<MqttConnectResult> getConnectFuture() {
    return connectFuture;
  }

  Channel channel;

  public MqttClientImpl(Channel channel) {
    this.channel = channel;
  }

  /**
   * Retrieve the netty {@link EventLoopGroup} we are using
   * 
   * @return The netty {@link EventLoopGroup} we use for the connection
   */

  public EventLoop executer() {
    return channel().eventLoop().next();
  }

  public Channel channel() {
    return channel;
  }

  /**
   * Subscribe on the given topic. When a message is received, MqttClient will
   * invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the
   * given handler
   *
   * @param topic The topic filter to subscribe to
   * @param handler The handler to invoke when we receive a message
   * @return A future which will be completed when the server acknowledges our
   * subscribe request
   */

  public Future<Void> on(String topic, MqttHandler handler) {
    return on(topic, handler, MqttQoS.AT_LEAST_ONCE);
  }

  /**
   * Subscribe on the given topic, with the given qos. When a message is received,
   * MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)}
   * function of the given handler
   *
   * @param topic The topic filter to subscribe to
   * @param handler The handler to invoke when we receive a message
   * @param qos The qos to request to the server
   * @return A future which will be completed when the server acknowledges our
   * subscribe request
   */

  public Future<Void> on(String topic, MqttHandler handler, MqttQoS qos) {
    return createSubscribtion(topic, handler, false, qos);
  }

  /**
   * Subscribe on the given topic. When a message is received, MqttClient will
   * invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the
   * given handler This subscribtion is only once. If the MqttClient has received
   * 1 message, the subscribtion will be removed
   *
   * @param topic The topic filter to subscribe to
   * @param handler The handler to invoke when we receive a message
   * @return A future which will be completed when the server acknowledges our
   * subscribe request
   */

  public Future<Void> once(String topic, MqttHandler handler) {
    return once(topic, handler, MqttQoS.AT_MOST_ONCE);
  }

  /**
   * Subscribe on the given topic, with the given qos. When a message is received,
   * MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)}
   * function of the given handler This subscribtion is only once. If the
   * MqttClient has received 1 message, the subscribtion will be removed
   *
   * @param topic The topic filter to subscribe to
   * @param handler The handler to invoke when we receive a message
   * @param qos The qos to request to the server
   * @return A future which will be completed when the server acknowledges our
   * subscribe request
   */

  public Future<Void> once(String topic, MqttHandler handler, MqttQoS qos) {
    return createSubscribtion(topic, handler, true, qos);
  }

  /**
   * Remove the subscribtion for the given topic and handler If you want to
   * unsubscribe from all handlers known for this topic, use {@link #off(String)}
   *
   * @param topic The topic to unsubscribe for
   * @param handler The handler to unsubscribe
   * @return A future which will be completed when the server acknowledges our
   * unsubscribe request
   */

  public Future<Void> off(String topic, MqttHandler handler) {
    Promise<Void> future = new DefaultPromise<>(executer());
    for (MqttSubscribtion subscribtion : this.handlerToSubscribtion
        .get(handler)) {
      this.topicToSubscriptions.get(topic).remove(subscribtion);
    }
    this.handlerToSubscribtion.remove(handler);
    this.checkSubscribtions(topic, future);
    return future;
  }

  /**
   * Remove all subscribtions for the given topic. If you want to specify which
   * handler to unsubscribe, use {@link #off(String, MqttHandler)}
   *
   * @param topic The topic to unsubscribe for
   * @return A future which will be completed when the server acknowledges our
   * unsubscribe request
   */

  public Future<Void> off(String topic) {
    Promise<Void> future = new DefaultPromise<>(executer());
    List<MqttSubscribtion> subscribtions
        = this.topicToSubscriptions.remove(topic);
    for (MqttSubscribtion subscribtion : subscribtions) {
      this.handlerToSubscribtion.remove(subscribtion.getHandler(),
          subscribtion);
    }
    this.checkSubscribtions(topic, future);
    return future;
  }

  /**
   * Publish a message to the given payload
   * 
   * @param topic The topic to publish to
   * @param payload The payload to send
   * @return A future which will be completed when the message is sent out of the
   * MqttClient
   */

  public Future<Void> publish(String topic, ByteBuf payload) {
    return publish(topic, payload, MqttQoS.AT_MOST_ONCE, false);
  }

  /**
   * Publish a message to the given payload, using the given qos
   * 
   * @param topic The topic to publish to
   * @param payload The payload to send
   * @param qos The qos to use while publishing
   * @return A future which will be completed when the message is delivered to the
   * server
   */

  public Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos) {
    return publish(topic, payload, qos, false);
  }

  /**
   * Publish a message to the given payload, using optional retain
   * 
   * @param topic The topic to publish to
   * @param payload The payload to send
   * @param retain true if you want to retain the message on the server, false
   * otherwise
   * @return A future which will be completed when the message is sent out of the
   * MqttClient
   */

  public Future<Void> publish(String topic, ByteBuf payload, boolean retain) {
    return publish(topic, payload, MqttQoS.AT_MOST_ONCE, retain);
  }

  /**
   * Publish a message to the given payload, using the given qos and optional
   * retain
   * 
   * @param topic The topic to publish to
   * @param payload The payload to send
   * @param qos The qos to use while publishing
   * @param retain true if you want to retain the message on the server, false
   * otherwise
   * @return A future which will be completed when the message is delivered to the
   * server
   */

  public Future<Void> publish(
      String topic, ByteBuf payload, MqttQoS qos, boolean retain) {
    Promise<Void> future = new DefaultPromise<>(executer());
    MqttFixedHeader fixedHeader
        = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retain, 0);
    MqttPublishVariableHeader variableHeader
        = new MqttPublishVariableHeader(topic, getNewMessageId().messageId());
    MqttPublishMessage message
        = new MqttPublishMessage(fixedHeader, variableHeader, payload);

    MqttPendingPublish pendingPublish = new MqttPendingPublish(
        variableHeader.packetId(), future, payload.retain(), message, qos);
    pendingPublish.setSent(this.sendAndFlushPacket(message) != null);

    if (pendingPublish.isSent()
        && pendingPublish.getQos() == MqttQoS.AT_MOST_ONCE) {
      pendingPublish.getFuture().setSuccess(null); // We don't get an ACK for QOS 0
    } else if (pendingPublish.isSent()) {
      this.pendingPublishes.put(pendingPublish.getMessageId(), pendingPublish);
      pendingPublish.startPublishRetransmissionTimer(executer(),
          this::sendAndFlushPacket);
    }

    return future;
  }

  /////////////////////////////// PRIVATE API ///////////////////////////////

  public ChannelFuture sendAndFlushPacket(Object message) {
    if (channel() == null) {
      return null;
    }
    if (channel().isActive()) {
      return channel().writeAndFlush(message);
    }
    return channel()
        .newFailedFuture(new RuntimeException("Channel is closed"));
  }

  private MqttMessageIdVariableHeader getNewMessageId() {
    this.nextMessageId.compareAndSet(0xffff, 1);
    return MqttMessageIdVariableHeader
        .from(this.nextMessageId.getAndIncrement());
  }

  private Future<Void> createSubscribtion(
      String topic, MqttHandler handler, boolean once, MqttQoS qos) {
    if (this.pendingSubscribeTopics.contains(topic)) {
      Optional<Map.Entry<Integer, MqttPendingSubscribtion>> subscribtionEntry
          = this.pendingSubscribtions.entrySet().stream()
              .filter((e) -> e.getValue().getTopic().equals(topic)).findAny();
      if (subscribtionEntry.isPresent()) {
        subscribtionEntry.get().getValue().addHandler(handler, once);
        return subscribtionEntry.get().getValue().getFuture();
      }
    }
    if (this.serverSubscribtions.contains(topic)) {
      MqttSubscribtion subscribtion
          = new MqttSubscribtion(topic, handler, once);
      if (topicToSubscriptions.containsKey(topic)) {
        topicToSubscriptions.get(topic).add(subscribtion);
      } else {
        List<MqttSubscribtion> li = new LinkedList<>();
        li.add(subscribtion);
        this.topicToSubscriptions.put(topic, li);
      }
      if (handlerToSubscribtion.containsKey(handler)) {
        handlerToSubscribtion.get(handler).add(subscribtion);
      } else {
        List<MqttSubscribtion> li = new LinkedList<>();
        li.add(subscribtion);
        this.handlerToSubscribtion.put(handler, li);
      }
      return channel().newSucceededFuture();
    }

    Promise<Void> future = new DefaultPromise<>(executer());
    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttTopicSubscription subscription = new MqttTopicSubscription(topic, qos);
    MqttMessageIdVariableHeader variableHeader = getNewMessageId();
    MqttSubscribePayload payload
        = new MqttSubscribePayload(Collections.singletonList(subscription));
    MqttSubscribeMessage message
        = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);

    final MqttPendingSubscribtion pendingSubscribtion
        = new MqttPendingSubscribtion(future, topic, message);
    pendingSubscribtion.addHandler(handler, once);
    this.pendingSubscribtions.put(variableHeader.messageId(),
        pendingSubscribtion);
    this.pendingSubscribeTopics.add(topic);
    pendingSubscribtion.setSent(this.sendAndFlushPacket(message) != null); // If not sent, we will send it when the connection is opened

    pendingSubscribtion.startRetransmitTimer(executer(),
        this::sendAndFlushPacket);

    return future;
  }

  private void checkSubscribtions(String topic, Promise<Void> promise) {
    if (!(this.topicToSubscriptions.containsKey(topic)
        && this.topicToSubscriptions.get(topic).size() != 0)
        && this.serverSubscribtions.contains(topic)) {
      MqttFixedHeader fixedHeader = new MqttFixedHeader(
          MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
      MqttMessageIdVariableHeader variableHeader = getNewMessageId();
      MqttUnsubscribePayload payload
          = new MqttUnsubscribePayload(Collections.singletonList(topic));
      MqttUnsubscribeMessage message
          = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);

      MqttPendingUnsubscribtion pendingUnsubscribtion
          = new MqttPendingUnsubscribtion(promise, topic, message);
      this.pendingServerUnsubscribes.put(variableHeader.messageId(),
          pendingUnsubscribtion);
      pendingUnsubscribtion.startRetransmissionTimer(executer(),
          this::sendAndFlushPacket);

      this.sendAndFlushPacket(message);
    } else {
      promise.setSuccess(null);
    }
  }

  public IntObjectHashMap<MqttPendingSubscribtion> getPendingSubscribtions() {
    return pendingSubscribtions;
  }

  public HashMap<String, List<MqttSubscribtion>> getSubscriptions() {
    return topicToSubscriptions;
  }

  public Set<String> getPendingSubscribeTopics() {
    return pendingSubscribeTopics;
  }

  public HashMap<MqttHandler, List<MqttSubscribtion>> getHandlerToSubscribtion() {
    return handlerToSubscribtion;
  }

  public Set<String> getServerSubscribtions() {
    return serverSubscribtions;
  }

  public IntObjectHashMap<MqttPendingUnsubscribtion> getPendingServerUnsubscribes() {
    return pendingServerUnsubscribes;
  }

  public IntObjectHashMap<MqttPendingPublish> getPendingPublishes() {
    return pendingPublishes;
  }

  public IntObjectHashMap<MqttIncomingQos2Publish> getQos2PendingIncomingPublishes() {
    return qos2PendingIncomingPublishes;
  }

}