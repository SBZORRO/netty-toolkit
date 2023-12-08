package mqtt.core;

import java.util.regex.Pattern;

public final class MqttSubscribtion {

  private final String topic;
  private final Pattern topicRegex;
  private final MqttHandler handler;

  private boolean once = false;

  private boolean called = false;

  private boolean isActive = false;

  public MqttSubscribtion(String topic, MqttHandler handler, boolean once) {
    if (topic == null) {
      throw new NullPointerException("topic");
    }
    if (handler == null) {
      throw new NullPointerException("handler");
    }
    this.topic = topic;
    this.handler = handler;
    this.once = once;
    this.topicRegex = Pattern
        .compile(topic.replace("+", "[^/]+").replace("#", ".+") + "$");
  }

  public String getTopic() {
    return topic;
  }

  public MqttHandler getHandler() {
    return handler;
  }

  public boolean isOnce() {
    return once;
  }

  public boolean isCalled() {
    return called;
  }

  public boolean matches(String topic) {
    return this.topicRegex.matcher(topic).matches();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    MqttSubscribtion that = (MqttSubscribtion) o;

    return once == that.once && topic.equals(that.topic)
        && handler.equals(that.handler);
  }

  @Override
  public int hashCode() {
    int result = topic.hashCode();
    result = 31 * result + handler.hashCode();
    result = 31 * result + (once ? 1 : 0);
    return result;
  }

  public void setCalled(boolean called) {
    this.called = called;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }
}
