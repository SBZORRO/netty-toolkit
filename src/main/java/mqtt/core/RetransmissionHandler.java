package mqtt.core;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.concurrent.ScheduledFuture;

final class RetransmissionHandler<T extends MqttMessage> {
  private ScheduledFuture<?> timer;
  private int timeout = 1;
  private BiConsumer<MqttFixedHeader, T> handler;
  private T originalMessage;

  void start(EventLoop eventLoop) {
    if (eventLoop == null) {
      throw new NullPointerException("eventLoop");
    }
    if (this.handler == null) {
      throw new NullPointerException("handler");
    }
    this.startTimer(eventLoop);
  }

  private void startTimer(EventLoop eventLoop) {
    this.timer = eventLoop.scheduleWithFixedDelay(() -> {
      MqttFixedHeader fixedHeader = new MqttFixedHeader(
          this.originalMessage.fixedHeader().messageType(), true,
          this.originalMessage.fixedHeader().qosLevel(),
          this.originalMessage.fixedHeader().isRetain(),
          this.originalMessage.fixedHeader().remainingLength());
      handler.accept(fixedHeader, originalMessage);
    }, timeout, timeout << 3, TimeUnit.SECONDS);
  }

  void stop() {
    if (this.timer != null) {
      this.timer.cancel(true);
    }
  }

  void setHandle(BiConsumer<MqttFixedHeader, T> runnable) {
    this.handler = runnable;
  }

  void setOriginalMessage(T originalMessage) {
    this.originalMessage = originalMessage;
  }

  T getOriginalMessage() {
    return this.originalMessage;
  }

  public ScheduledFuture<?> getTimer() {
    return timer;
  }
}
