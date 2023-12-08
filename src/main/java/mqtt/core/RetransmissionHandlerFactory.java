package mqtt.core;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;

public final class RetransmissionHandlerFactory {

  private RetransmissionHandlerFactory() {}

  public static RetransmissionHandler<MqttPublishMessage> newPublishHandler(
      ChannelFuture obj, final MqttPublishMessage message) {

    RetransmissionHandler<MqttPublishMessage> publishRetransmissionHandler
        = new RetransmissionHandler<>();
    publishRetransmissionHandler.setOriginalMessage(message);

    publishRetransmissionHandler
        .setHandle(
            (fixedHeader, originalMessage) -> obj.channel().writeAndFlush(
                new MqttPublishMessage(fixedHeader,
                    originalMessage.variableHeader(),
                    message.payload().retain())));
    publishRetransmissionHandler.start(obj.channel().eventLoop());

    return publishRetransmissionHandler;
  }

  public static RetransmissionHandler<MqttMessage> newPubrelHandler(
      ChannelFuture obj, final MqttMessage message) {
    RetransmissionHandler<MqttMessage> pubrelRetransmissionHandler
        = new RetransmissionHandler<>();
    pubrelRetransmissionHandler.setOriginalMessage(message);

    pubrelRetransmissionHandler
        .setHandle(
            (fixedHeader, originalMessage) -> obj.channel().writeAndFlush(
                new MqttMessage(fixedHeader,
                    originalMessage.variableHeader())));

    pubrelRetransmissionHandler.start(obj.channel().eventLoop());

    return pubrelRetransmissionHandler;
  }

  public static RetransmissionHandler<MqttPublishMessage> newQos2IncomingPubHandler(
      ChannelFuture obj, final MqttPublishMessage message) {
    RetransmissionHandler<MqttPublishMessage> qos2IncomingPubHandler
        = new RetransmissionHandler<>();
    qos2IncomingPubHandler.setOriginalMessage(message);

    qos2IncomingPubHandler.setHandle(
        (fh, originalMessage) -> {

          MqttMessageIdVariableHeader variableHeader
              = MqttMessageIdVariableHeader
                  .from(message.variableHeader().packetId());

          obj.channel().writeAndFlush(
              new MqttMessage(fh, variableHeader));
        });

    qos2IncomingPubHandler.start(obj.channel().eventLoop());

    return qos2IncomingPubHandler;
  }

  public static RetransmissionHandler<MqttUnsubscribeMessage> newUnsubscribeHandler(
      ChannelFuture obj, final MqttUnsubscribeMessage message) {

    RetransmissionHandler<MqttUnsubscribeMessage> subscribeRetransmissionHandler
        = new RetransmissionHandler<>();
    subscribeRetransmissionHandler.setOriginalMessage(message);

    subscribeRetransmissionHandler
        .setHandle(
            (fixedHeader, originalMessage) -> obj.channel().writeAndFlush(
                new MqttUnsubscribeMessage(fixedHeader,
                    originalMessage.variableHeader(),
                    message.payload())));
    subscribeRetransmissionHandler.start(obj.channel().eventLoop());

    return subscribeRetransmissionHandler;
  }

  public static RetransmissionHandler<MqttSubscribeMessage> newSubscribeHandler(
      ChannelFuture obj, final MqttSubscribeMessage message) {

    RetransmissionHandler<MqttSubscribeMessage> subscribeRetransmissionHandler
        = new RetransmissionHandler<>();
    subscribeRetransmissionHandler.setOriginalMessage(message);

    subscribeRetransmissionHandler
        .setHandle(
            (fixedHeader, originalMessage) -> obj.channel().writeAndFlush(
                new MqttSubscribeMessage(fixedHeader,
                    originalMessage.variableHeader(),
                    message.payload())));
    subscribeRetransmissionHandler.start(obj.channel().eventLoop());

    return subscribeRetransmissionHandler;
  }
}
