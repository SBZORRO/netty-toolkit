package mqtt.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.DefaultPromise;

public final class MqttChannelHandler
    extends SimpleChannelInboundHandler<MqttMessage> {

  private final MqttClientImpl impl;

  public MqttChannelHandler(MqttClientImpl impl) {
    this.impl = impl;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg)
      throws Exception {
    switch (msg.fixedHeader().messageType()) {
      case CONNACK:
        handleConack(ctx.channel(), (MqttConnAckMessage) msg);
        break;
      case SUBACK:
        handleSubAck((MqttSubAckMessage) msg);
        break;
      case PUBLISH:
        handlePublish(ctx.channel(), (MqttPublishMessage) msg);
        break;
      case UNSUBACK:
        handleUnsuback((MqttUnsubAckMessage) msg);
        break;
      case PUBACK:
        handlePuback((MqttPubAckMessage) msg);
        break;
      case PUBREC:
        handlePubrec(ctx.channel(), msg);
        break;
      case PUBREL:
        handlePubrel(ctx.channel(), msg);
        break;
      case PUBCOMP:
        handlePubcomp(msg);
        break;
      default:
        break;
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);

    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT,
        false, MqttQoS.AT_MOST_ONCE, false, 0);

    ctx.channel().writeAndFlush(
        new MqttConnectMessage(fixedHeader,
            impl.mqttConnectVariableHeader(),
            impl.mqttConnectPayload()))
        .addListener((ChannelFutureListener) f -> impl
            .connectFuture(new DefaultPromise<>(f.channel().eventLoop())));
  }

  private void invokeHandlersForIncomingPublish(MqttPublishMessage message) {
    Set<BeanMqttSubscribtion> li = new HashSet<>();
    for (Map.Entry<String, Set<BeanMqttSubscribtion>> entry : this.impl
        .topicToSubscriptions().entrySet()) {
      li.addAll(entry.getValue());
    }
    for (BeanMqttSubscribtion subscribtion : li) {
      if (subscribtion.matches(message.variableHeader().topicName())) {
        if (subscribtion.isOnce() && subscribtion.isCalled()) {
          continue;
        }
        message.payload().markReaderIndex();
        subscribtion.setCalled(true);
        subscribtion.getHandler().onMessage(
            message.variableHeader().topicName(), message.payload());
        if (subscribtion.isOnce()) {
          this.impl.off(subscribtion.getTopic(), subscribtion.getHandler());
        }
        message.payload().resetReaderIndex();
      }
    }
    /*
     * Set<MqttSubscribtion> subscribtions =
     * ImmutableSet.copyOf(this.client.getSubscriptions().get(message.variableHeader
     * ().topicName())); for (MqttSubscribtion subscribtion : subscribtions) {
     * if(subscribtion.isOnce() && subscribtion.isCalled()){ continue; }
     * message.payload().markReaderIndex(); subscribtion.setCalled(true);
     * subscribtion.getHandler().onMessage(message.variableHeader().topicName(),
     * message.payload()); if(subscribtion.isOnce()){
     * this.client.off(subscribtion.getTopic(), subscribtion.getHandler()); }
     * message.payload().resetReaderIndex(); }
     */
//    message.payload().release();
  }

  private void handleConack(Channel channel, MqttConnAckMessage message) {
    switch (message.variableHeader().connectReturnCode()) {
      case CONNECTION_ACCEPTED:
        impl.connectFuture().setSuccess(new BeanMqttConnectResult(true,
            MqttConnectReturnCode.CONNECTION_ACCEPTED, channel.closeFuture()));
        break;

      case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
      case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
      case CONNECTION_REFUSED_NOT_AUTHORIZED:
      case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
      case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
        impl.connectFuture().setSuccess(new BeanMqttConnectResult(false,
            message.variableHeader().connectReturnCode(),
            channel.closeFuture()));
        channel.close();
        // Don't start reconnect logic here
        break;
      default:
        break;
    }
  }

  private void handleSubAck(MqttSubAckMessage message) {
    RetransmissionHandler<MqttSubscribeMessage> pendingSubscribtions
        = this.impl.pendingSubscribtions()
            .get(message.variableHeader().messageId());

    this.impl.pendingSubscribtions()
        .remove(message.variableHeader().messageId());
    pendingSubscribtions.stop();
    List<MqttTopicSubscription> li = pendingSubscribtions.getOriginalMessage()
        .payload().topicSubscriptions();

    for (MqttTopicSubscription topic : li) {
      this.impl.getServerSubscribtions().add(topic.topicName());
    }
  }

  private void handlePublish(Channel channel, MqttPublishMessage message) {
    switch (message.fixedHeader().qosLevel()) {
      case AT_MOST_ONCE:
        invokeHandlersForIncomingPublish(message);
        break;

      case AT_LEAST_ONCE:
        invokeHandlersForIncomingPublish(message);
        if (message.variableHeader().packetId() != -1) {
          MqttFixedHeader fixedHeader = new MqttFixedHeader(
              MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
          MqttMessageIdVariableHeader variableHeader
              = MqttMessageIdVariableHeader
                  .from(message.variableHeader().packetId());
          channel.writeAndFlush(
              new MqttPubAckMessage(fixedHeader, variableHeader));
        }
        break;

      case EXACTLY_ONCE:
        if (message.variableHeader().packetId() != -1) {

          MqttFixedHeader fixedHeader = new MqttFixedHeader(
              MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
          MqttMessageIdVariableHeader variableHeader
              = MqttMessageIdVariableHeader
                  .from(message.variableHeader().packetId());
          MqttMessage pubrecMessage
              = new MqttMessage(fixedHeader, variableHeader);

          message.payload().retain();

          channel.writeAndFlush(pubrecMessage)
              .addListener((ChannelFutureListener) f -> {
                this.impl.pendingQos2IncomingPublishes().put(
                    message.variableHeader().packetId(),
                    RetransmissionHandlerFactory.newQos2IncomingPubHandler(f, message));
              });
        }
        break;
      default:
        break;
    }
  }

  private void handleUnsuback(MqttUnsubAckMessage message) {
    RetransmissionHandler<MqttUnsubscribeMessage> pendingServerUnsubscribes
        = this.impl.pendingServerUnsubscribes()
            .get(message.variableHeader().messageId());

    this.impl.pendingServerUnsubscribes()
        .remove(message.variableHeader().messageId());
    pendingServerUnsubscribes.stop();
  }

  // qos 1
  private void handlePuback(MqttPubAckMessage message) {
    this.impl.getPendingPublishes()
        .remove(message.variableHeader().messageId()).stop();
  }

  // qos 2 part 1
  private void handlePubrec(Channel channel, MqttMessage message) {
    this.impl.getPendingPublishes().remove(
        ((MqttMessageIdVariableHeader) message.variableHeader()).messageId())
        .stop();

    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL,
        false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader
        = (MqttMessageIdVariableHeader) message.variableHeader();
    MqttMessage pubrelMessage = new MqttMessage(fixedHeader, variableHeader);
    channel.writeAndFlush(pubrelMessage)
        .addListener(
            (ChannelFutureListener) f -> {
              this.impl.pendingPubreles().put(
                  ((MqttMessageIdVariableHeader) message.variableHeader())
                      .messageId(),
                  RetransmissionHandlerFactory.newPubrelHandler(f, message));
            });
  }

  // qos 2 part 2
  private void handlePubrel(Channel channel, MqttMessage message) {
    if (this.impl.pendingQos2IncomingPublishes()
        .containsKey(((MqttMessageIdVariableHeader) message.variableHeader())
            .messageId())) {
      RetransmissionHandler<MqttPublishMessage> incomingQos2Publish
          = this.impl.pendingQos2IncomingPublishes()
              .get(((MqttMessageIdVariableHeader) message.variableHeader())
                  .messageId());
      this.invokeHandlersForIncomingPublish(
          incomingQos2Publish.getOriginalMessage());
      incomingQos2Publish.stop();
      this.impl.pendingQos2IncomingPublishes().remove(incomingQos2Publish
          .getOriginalMessage().variableHeader().packetId());
    }
    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP,
        false, MqttQoS.AT_MOST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader
        .from(((MqttMessageIdVariableHeader) message.variableHeader())
            .messageId());
    channel.writeAndFlush(new MqttMessage(fixedHeader, variableHeader));
  }

  // qos 2 part 3
  private void handlePubcomp(MqttMessage message) {
    MqttMessageIdVariableHeader variableHeader
        = (MqttMessageIdVariableHeader) message.variableHeader();
    this.impl.getPendingPublishes().remove(variableHeader.messageId()).stop();
  }
}
