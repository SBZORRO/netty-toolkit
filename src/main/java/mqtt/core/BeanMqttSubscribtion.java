package mqtt.core;

import java.util.regex.Pattern;

public final class BeanMqttSubscribtion {

  private final String topic;
  private final Pattern topicRegex;
  private final IMqttHandler handler;

  private boolean once = false;

  private boolean called = false;

  private boolean isActive = false;

  public BeanMqttSubscribtion(String topic, IMqttHandler handler,
      boolean once) {
    this.topic = topic;
    this.handler = handler;
    this.once = once;
    this.topicRegex = Pattern
        .compile(topic.replace("+", "[^/]+").replace("#", ".+") + "$");
  }

  public String getTopic() {
    return topic;
  }

  public IMqttHandler getHandler() {
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

    BeanMqttSubscribtion that = (BeanMqttSubscribtion) o;

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
