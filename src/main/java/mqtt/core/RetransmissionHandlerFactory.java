package mqtt.core;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;

public final class RetransmissionHandlerFactory {

  private RetransmissionHandlerFactory() {}

  public static RtnsHandler<MqttPublishMessage> newPublishHandler(
      ChannelFuture obj, final MqttPublishMessage message) {

    RtnsHandler<MqttPublishMessage> pubRtnsHandler = new RtnsHandler<>();
    pubRtnsHandler.setOriginalMessage(message);

    pubRtnsHandler.setHandle(() -> obj.channel().writeAndFlush(
        new MqttPublishMessage(
            new MqttFixedHeader(
                message.fixedHeader().messageType(), true,
                message.fixedHeader().qosLevel(),
                message.fixedHeader().isRetain(),
                message.fixedHeader().remainingLength()),
            message.variableHeader(),
            message.payload().retain())));
    pubRtnsHandler.start(obj.channel().eventLoop());

    return pubRtnsHandler;
  }

  public static RtnsHandler<MqttMessage> newPubrelHandler(
      ChannelFuture obj, final MqttMessage message) {
    RtnsHandler<MqttMessage> pubrelRtnsHandler = new RtnsHandler<>();
    pubrelRtnsHandler.setOriginalMessage(message);
    pubrelRtnsHandler.setHandle(() -> obj.channel().writeAndFlush(message));
    pubrelRtnsHandler.start(obj.channel().eventLoop());
    return pubrelRtnsHandler;
  }

//  public static RtnsHandler<MqttPublishMessage> newPubrecHandler(
//      ChannelFuture obj, final MqttPublishMessage message) {
//    RtnsHandler<MqttPublishMessage> qos2IncomingPubHandler = new RtnsHandler<>();
//    qos2IncomingPubHandler.setOriginalMessage(message);
//    qos2IncomingPubHandler.setHandle(
//        (fh, originalMessage) -> {
//          MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader
//              .from(message.variableHeader().packetId());
//          obj.channel().writeAndFlush(new MqttMessage(fh, variableHeader));
//        });
//    qos2IncomingPubHandler.start(obj.channel().eventLoop());
//    return qos2IncomingPubHandler;
//  }

  public static RtnsHandler<MqttUnsubscribeMessage> newUnsubscribeHandler(
      ChannelFuture obj, final MqttUnsubscribeMessage message) {
    RtnsHandler<MqttUnsubscribeMessage> subRtnsHandler = new RtnsHandler<>();
    subRtnsHandler.setOriginalMessage(message);
    subRtnsHandler.setHandle(() -> obj.channel().writeAndFlush(message));
    subRtnsHandler.start(obj.channel().eventLoop());
    return subRtnsHandler;
  }

  public static RtnsHandler<MqttSubscribeMessage> newSubscribeHandler(
      ChannelFuture obj, final MqttSubscribeMessage message) {
    RtnsHandler<MqttSubscribeMessage> subRtnsHandler = new RtnsHandler<>();
    subRtnsHandler.setOriginalMessage(message);
    subRtnsHandler.setHandle(() -> obj.channel().writeAndFlush(message));
    subRtnsHandler.start(obj.channel().eventLoop());
    return subRtnsHandler;
  }
}
