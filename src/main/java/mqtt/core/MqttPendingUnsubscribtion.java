package mqtt.core;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Promise;

import java.util.function.Consumer;

public final class MqttPendingUnsubscribtion {

  private final Promise<Void> future;
  private final String topic;

  private final RetransmissionHandler<MqttUnsubscribeMessage> retransmissionHandler
      = new RetransmissionHandler<>();

  MqttPendingUnsubscribtion(Promise<Void> future, String topic,
      MqttUnsubscribeMessage unsubscribeMessage) {
    this.future = future;
    this.topic = topic;

    this.retransmissionHandler.setOriginalMessage(unsubscribeMessage);
  }

  public Promise<Void> getFuture() {
    return future;
  }

  public String getTopic() {
    return topic;
  }

  void startRetransmissionTimer(
      EventLoop eventLoop, Consumer<Object> sendPacket) {
    this.retransmissionHandler
        .setHandle(
            (fixedHeader, originalMessage) -> sendPacket
                .accept(
                    new MqttUnsubscribeMessage(fixedHeader,
                        originalMessage.variableHeader(),
                        originalMessage.payload())));
    this.retransmissionHandler.start(eventLoop);
  }

  public void onUnsubackReceived() {
    this.retransmissionHandler.stop();
  }
}
