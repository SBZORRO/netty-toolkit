package mqtt.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class BeanSub {

  private final String topic;
  private final Pattern topicRegex;
  private final List<IMqttHandler> handler = new ArrayList<>();

  public BeanSub(String topic, IMqttHandler handler) {
    this.topic = topic;
    this.topicRegex = Pattern
        .compile(topic.replace("+", "[^/]+").replace("#", ".+") + "$");
    this.handler.add(handler);
  }

  public String getTopic() {
    return topic;
  }

  public List<IMqttHandler> handler() {
    return handler;
  }

  public void handler(IMqttHandler handler) {
    this.handler.add(handler);
  }

  public boolean matches(String topic) {
    return this.topicRegex.matcher(topic).matches();
  }
}
