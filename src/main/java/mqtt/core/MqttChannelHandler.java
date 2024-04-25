package mqtt.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultPromise;

public final class MqttChannelHandler extends SimpleChannelInboundHandler<MqttMessage> {

  private final MqttClientImpl impl;

  public MqttChannelHandler(MqttClientImpl impl) {
    this.impl = impl;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg)
      throws Exception {
    try {
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
    } catch (Exception e) {

      e.printStackTrace();
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    sendConn(ctx.channel());
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("channelInactive");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    System.out.println("exceptionCaught");
    cause.printStackTrace();
  }

  public static long k = 0;

  private void invokeHandlersForIncomingPublish(MqttPublishMessage message) {
    ByteBuf bb = message.payload();
    byte[] array = new byte[bb.readableBytes()];
    bb.getBytes(0, array);
//    ReferenceCountUtil.release(bb);

    String topic = message.variableHeader().topicName();
    BeanSub sub = this.impl.topicToSub().get(topic);
    for (IMqttHandler hdl : sub.handler()) {
      hdl.onMessage(topic, array);
    }
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
    RtnsHandler<MqttSubscribeMessage> pendingSubscribtions = this.impl
        .pendingSubs().get(message.variableHeader().messageId());

    this.impl.pendingSubs().remove(message.variableHeader().messageId());
    pendingSubscribtions.stop();
  }

  private void handlePublish(Channel channel, MqttPublishMessage message) {
    switch (message.fixedHeader().qosLevel()) {
      case AT_MOST_ONCE:
        invokeHandlersForIncomingPublish(message);
        break;

      case AT_LEAST_ONCE:
        invokeHandlersForIncomingPublish(message);
        if (message.variableHeader().packetId() != -1) {
          sendPuback(channel, message);
        }
        break;

      case EXACTLY_ONCE: // Method B
        invokeHandlersForIncomingPublish(message);
        if (message.variableHeader().packetId() != -1) {
          sendPubrec(channel, message);
        }
        break;
      default:
        break;
    }
  }

  private void handleUnsuback(MqttUnsubAckMessage message) {
    RtnsHandler<MqttUnsubscribeMessage> pendingServerUnsubscribes = this.impl
        .pendingUnsubs().get(message.variableHeader().messageId());

    this.impl.pendingUnsubs().remove(message.variableHeader().messageId());
    pendingServerUnsubscribes.stop();
  }

  // qos 1
  private void handlePuback(MqttPubAckMessage message) {
    this.impl.pendingPublishes().remove(message.variableHeader().messageId())
        .stop();
  }

  // qos 2 part 1
  private void handlePubrec(Channel channel, MqttMessage message) {
    this.impl.pendingPublishes().remove(
        ((MqttMessageIdVariableHeader) message.variableHeader()).messageId())
        .stop();

    this.sendPubrel(channel, message);
  }

  // qos 2 part 2
  private void handlePubrel(Channel channel, MqttMessage message) {
    this.sendPubcomp(channel, message);
  }

  // qos 2 part 3
  private void handlePubcomp(MqttMessage message) {
    MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message
        .variableHeader();
    this.impl.pendingPubreles().remove(variableHeader.messageId()).stop();
  }

  private ChannelFuture sendConn(Channel channel) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT,
        false, MqttQoS.AT_MOST_ONCE, false, 0);

    return channel.writeAndFlush(
        new MqttConnectMessage(fixedHeader,
            impl.mqttConnectVariableHeader(),
            impl.mqttConnectPayload()))
        .addListener((ChannelFutureListener) f -> impl
            .connectFuture(new DefaultPromise<>(f.channel().eventLoop())));
  }

  private ChannelFuture
      sendPuback(Channel channel, MqttPublishMessage message) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader
        .from(message.variableHeader().packetId());
    return channel.writeAndFlush(
        new MqttPubAckMessage(fixedHeader, variableHeader));
  }

  private ChannelFuture
      sendPubrec(Channel channel, MqttPublishMessage message) {

    MqttFixedHeader fixedHeader = new MqttFixedHeader(
        MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader
        .from(message.variableHeader().packetId());
    MqttMessage pubrecMessage = new MqttMessage(fixedHeader,
        variableHeader);

    return channel.writeAndFlush(pubrecMessage);
  }

  private ChannelFuture
      sendPubrel(Channel channel, MqttMessage message) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL,
        false, MqttQoS.AT_LEAST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message
        .variableHeader();
    MqttMessage pubrelMessage = new MqttMessage(fixedHeader, variableHeader);
    return channel.writeAndFlush(pubrelMessage)
        .addListener((ChannelFutureListener) f -> {
          this.impl.pendingPubreles().put(
              ((MqttMessageIdVariableHeader) message.variableHeader())
                  .messageId(),
              RetransmissionHandlerFactory.newPubrelHandler(f, message));
        });
  }

  private ChannelFuture sendPubcomp(Channel channel, MqttMessage message) {
    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP,
        false, MqttQoS.AT_MOST_ONCE, false, 0);
    MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader
        .from(((MqttMessageIdVariableHeader) message.variableHeader())
            .messageId());
    return channel.writeAndFlush(new MqttMessage(fixedHeader, variableHeader));
  }

}
