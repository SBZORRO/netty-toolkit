package mqtt.core;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

public final class MqttPendingPublish {

  public static RetransmissionHandler<MqttPublishMessage> newPublishHandler(
      ChannelFuture obj, final MqttPublishMessage message) {

//    MqttPendingPublish pendingPublish
//        = new MqttPendingPublish(message);
    RetransmissionHandler<MqttPublishMessage> publishRetransmissionHandler
        = new RetransmissionHandler<>();
    publishRetransmissionHandler.setOriginalMessage(message);

//      this.pendingPublishes.put(message.variableHeader().packetId(),
//          pendingPublish);

    publishRetransmissionHandler
        .setHandle(
            (fixedHeader, originalMessage) -> obj.channel().writeAndFlush(
                new MqttPublishMessage(fixedHeader,
                    originalMessage.variableHeader(),
                    message.payload().retain())));
    publishRetransmissionHandler.start(obj.channel().eventLoop());

//    pendingPublish.startPublishRetransmissionTimer(obj.channel().eventLoop(),
//        msg -> obj.channel().writeAndFlush(msg));

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

//          MqttFixedHeader fixedHeader = new MqttFixedHeader(
//              MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
          MqttMessageIdVariableHeader variableHeader
              = MqttMessageIdVariableHeader
                  .from(message.variableHeader().packetId());
//          MqttMessage pubrecMessage
//              = new MqttMessage(fixedHeader, variableHeader);

          obj.channel().writeAndFlush(
              new MqttMessage(fh, variableHeader));
        });

    qos2IncomingPubHandler.start(obj.channel().eventLoop());

    return qos2IncomingPubHandler;
  }
//  private final int messageId;
//  private final Promise<Void> future;
//  private final ByteBuf payload;
//  private final MqttPublishMessage message;
//  private final MqttQoS qos;
//  private boolean sent = false;

//  private final RetransmissionHandler<MqttPublishMessage> publishRetransmissionHandler
//      = new RetransmissionHandler<>();
//  private final RetransmissionHandler<MqttMessage> pubrelRetransmissionHandler
//      = new RetransmissionHandler<>();

//  MqttPendingPublish(MqttPublishMessage message) {
//    this.message = message;
//
//    this.publishRetransmissionHandler.setOriginalMessage(message);
//  }

//  public Promise<Void> getFuture() {
//    return future;
//  }

//  public boolean isSent() {
//    return sent;
//  }

//  public void setSent(boolean sent) {
//    this.sent = sent;
//  }

//  public MqttPublishMessage getMessage() {
//    return message;
//  }

//  void startPublishRetransmissionTimer(
//      EventLoop eventLoop, Consumer<Object> sendPacket) {
//    this.publishRetransmissionHandler
//        .setHandle(
//            (fixedHeader, originalMessage) -> sendPacket
//                .accept(
//                    new MqttPublishMessage(fixedHeader,
//                        originalMessage.variableHeader(),
//                        this.payload.retain())));
//    this.publishRetransmissionHandler.start(eventLoop);
//  }

//  public void onPubackReceived() {
//    this.publishRetransmissionHandler.stop();
//  }

//  public void setPubrelMessage(MqttMessage pubrelMessage) {
//    this.pubrelRetransmissionHandler.setOriginalMessage(pubrelMessage);
//  }

//  public void startPubrelRetransmissionTimer(
//      EventLoop eventLoop, Consumer<Object> sendPacket) {
//    this.pubrelRetransmissionHandler
//        .setHandle(
//            (fixedHeader, originalMessage) -> sendPacket
//                .accept(
//                    new MqttMessage(fixedHeader,
//                        originalMessage.variableHeader())));
//    this.pubrelRetransmissionHandler.start(eventLoop);
//  }

//  public void onPubcompReceived() {
//    this.pubrelRetransmissionHandler.stop();
//  }
}
