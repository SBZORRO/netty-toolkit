package mqtt.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import mqtt.client.MyMqttClient2;

public final class MqttOutboundHandler
    extends SimpleChannelInboundHandler<MqttMessage> {

  private Promise<MqttConnectResult> connectFuture;

  private final MyMqttClient2 client;
  private final MqttClientImpl impl;

  public MqttOutboundHandler(MyMqttClient2 client) {
    this.client = client;
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
//    MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
//        this.client.getClientConfig().getProtocolVersion().protocolName(),  // Protocol Name
//        this.client.getClientConfig().getProtocolVersion().protocolLevel(), // Protocol Level
//        this.client.getClientConfig().getUsername() != null,                // Has Username
//        this.client.getClientConfig().getPassword() != null,                // Has Password
//        this.client.getClientConfig().getLastWill() != null                 // Will Retain
//            && this.client.getClientConfig().getLastWill().isRetain(),
//        this.client.getClientConfig().getLastWill() != null                 // Will QOS
//            ? this.client.getClientConfig().getLastWill().getQos().value()
//            : 0,
//        this.client.getClientConfig().getLastWill() != null,                // Has Will
//        this.client.getClientConfig().isCleanSession(),                     // Clean Session
//        this.client.getClientConfig().getTimeoutSeconds()                   // Timeout
//    );
//    MqttConnectPayload payload
//        = new MqttConnectPayload(this.client.getClientConfig().getClientId(),
//            this.client.getClientConfig().getLastWill() != null
//                ? this.client.getClientConfig().getLastWill().getTopic()
//                : null,
//            this.client.getClientConfig().getLastWill() != null
//                ? this.client.getClientConfig().getLastWill().getMessage()
//                    .getBytes()
//                : new byte[0],
//            this.client.getClientConfig().getUsername(),
//            this.client.getClientConfig().getPassword() != null
//                ? this.client.getClientConfig().getPassword().getBytes()
//                : new byte[0]);
    ctx.channel().writeAndFlush(
        new MqttConnectMessage(fixedHeader,
            client.config().mqttConnectVariableHeader(),
            client.config().mqttConnectPayload()));
  }

  private void invokeHandlersForIncomingPublish(MqttPublishMessage message) {
    List<MqttSubscribtion> li = new LinkedList<>();
    for (Map.Entry<String, List<MqttSubscribtion>> entry : this.impl
        .getSubscriptions().entrySet()) {
      li.addAll(entry.getValue());
    }
    for (MqttSubscribtion subscribtion : li) {
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
        this.connectFuture = new DefaultPromise<>(channel.eventLoop());
        this.connectFuture.setSuccess(new MqttConnectResult(true,
            MqttConnectReturnCode.CONNECTION_ACCEPTED, channel.closeFuture()));

        this.impl.getPendingSubscribtions().entrySet().stream()
            .filter((e) -> !e.getValue().isSent()).forEach((e) -> {
              channel.write(e.getValue().getSubscribeMessage());
              e.getValue().setSent(true);
            });

        this.impl.getPendingPublishes().forEach((id, publish) -> {
          if (publish.isSent())
            return;
          channel.write(publish.getMessage());
          publish.setSent(true);
          if (publish.getQos() == MqttQoS.AT_MOST_ONCE) {
            publish.getFuture().setSuccess(null); // We don't get an ACK for QOS 0
            this.impl.getPendingPublishes().remove(publish.getMessageId());
          }
        });
        channel.flush();
        break;

      case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
      case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
      case CONNECTION_REFUSED_NOT_AUTHORIZED:
      case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
      case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
        this.connectFuture.setSuccess(new MqttConnectResult(false,
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
    MqttPendingSubscribtion pendingSubscribtion = this.impl
        .getPendingSubscribtions().get(message.variableHeader().messageId());
    if (pendingSubscribtion == null) {
      return;
    }
    pendingSubscribtion.onSubackReceived();
    for (MqttPendingSubscribtion.MqttPendingHandler handler : pendingSubscribtion
        .getHandlers()) {
      MqttSubscribtion subscribtion
          = new MqttSubscribtion(pendingSubscribtion.getTopic(),
              handler.getHandler(), handler.isOnce());
      if (this.impl.getSubscriptions()
          .containsKey(pendingSubscribtion.getTopic())) {
        this.impl.getSubscriptions().get(pendingSubscribtion.getTopic())
            .add(subscribtion);

      } else {
        List<MqttSubscribtion> li = new LinkedList<>();
        li.add(subscribtion);
        this.impl.getSubscriptions().put(pendingSubscribtion.getTopic(), li);
      }

      if (this.impl.getHandlerToSubscribtion()
          .containsKey(handler.getHandler())) {
        this.impl.getHandlerToSubscribtion().get(handler.getHandler())
            .add(subscribtion);
      } else {
        List<MqttSubscribtion> li = new LinkedList<>();
        li.add(subscribtion);
        this.impl.getHandlerToSubscribtion().put(handler.getHandler(), li);
      }

    }
    this.impl.getPendingSubscribeTopics()
        .remove(pendingSubscribtion.getTopic());

    this.impl.getServerSubscribtions().add(pendingSubscribtion.getTopic());
    pendingSubscribtion.getFuture().setSuccess(null);
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

          MqttIncomingQos2Publish incomingQos2Publish
              = new MqttIncomingQos2Publish(message, pubrecMessage);
          this.impl.getQos2PendingIncomingPublishes()
              .put(message.variableHeader().packetId(), incomingQos2Publish);
          message.payload().retain();
          incomingQos2Publish.startPubrecRetransmitTimer(
              channel.eventLoop().next(),
              this.impl::sendAndFlushPacket);

          channel.writeAndFlush(pubrecMessage);
        }
        break;
      default:
        break;
    }
  }

  private void handleUnsuback(MqttUnsubAckMessage message) {
    MqttPendingUnsubscribtion unsubscribtion
        = this.impl.getPendingServerUnsubscribes()
            .get(message.variableHeader().messageId());
    if (unsubscribtion == null) {
      return;
    }
    unsubscribtion.onUnsubackReceived();
    this.impl.getServerSubscribtions().remove(unsubscribtion.getTopic());
    unsubscribtion.getFuture().setSuccess(null);
    this.impl.getPendingServerUnsubscribes()
        .remove(message.variableHeader().messageId());
  }

  private void handlePuback(MqttPubAckMessage message) {
    MqttPendingPublish pendingPublish = this.impl.getPendingPublishes()
        .get(message.variableHeader().messageId());
    pendingPublish.getFuture().setSuccess(null);
    pendingPublish.onPubackReceived();
    this.impl.getPendingPublishes()
        .remove(message.variableHeader().messageId());
//    pendingPublish.getPayload().release();
  }

  private void handlePubrec(Channel channel, MqttMessage message) {
    MqttPendingPublish pendingPublish = this.impl.getPendingPublishes().get(
        ((MqttMessageIdVariableHeader) message.variableHeader()).messageId());
    pendingPublish.onPubackReceived();

    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL,
        false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader
        = (MqttMessageIdVariableHeader) message.variableHeader();
    MqttMessage pubrelMessage = new MqttMessage(fixedHeader, variableHeader);
    channel.writeAndFlush(pubrelMessage);

    pendingPublish.setPubrelMessage(pubrelMessage);
    pendingPublish.startPubrelRetransmissionTimer(
        channel.eventLoop().next(), this.impl::sendAndFlushPacket);
  }

  private void handlePubrel(Channel channel, MqttMessage message) {
    if (this.impl.getQos2PendingIncomingPublishes()
        .containsKey(((MqttMessageIdVariableHeader) message.variableHeader())
            .messageId())) {
      MqttIncomingQos2Publish incomingQos2Publish
          = this.impl.getQos2PendingIncomingPublishes()
              .get(((MqttMessageIdVariableHeader) message.variableHeader())
                  .messageId());
      this.invokeHandlersForIncomingPublish(
          incomingQos2Publish.getIncomingPublish());
      incomingQos2Publish.onPubrelReceived();
      this.impl.getQos2PendingIncomingPublishes().remove(incomingQos2Publish
          .getIncomingPublish().variableHeader().packetId());
    }
    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP,
        false, MqttQoS.AT_MOST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader
        .from(((MqttMessageIdVariableHeader) message.variableHeader())
            .messageId());
    channel.writeAndFlush(new MqttMessage(fixedHeader, variableHeader));
  }

  private void handlePubcomp(MqttMessage message) {
    MqttMessageIdVariableHeader variableHeader
        = (MqttMessageIdVariableHeader) message.variableHeader();
    MqttPendingPublish pendingPublish
        = this.impl.getPendingPublishes().get(variableHeader.messageId());
    pendingPublish.getFuture().setSuccess(null);
    this.impl.getPendingPublishes().remove(variableHeader.messageId());
//    pendingPublish.getPayload().release();
    pendingPublish.onPubcompReceived();
  }
}
