package mqtt.core;

import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.concurrent.ScheduledFuture;

final class RtnsHandler<T extends MqttMessage> {
  private ScheduledFuture<?> timer;
  private int timeout = 2;
  private Runnable handler;
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
    this.timer = eventLoop.scheduleWithFixedDelay(handler, timeout,
        timeout << 1, TimeUnit.SECONDS);
  }

  void stop() {
    if (this.timer != null) {
      this.timer.cancel(true);
    }
  }

  void setHandle(Runnable handler) {
    this.handler = handler;
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
