package mqtt.core;

import io.netty.buffer.ByteBuf;

public interface MqttHandler {

  void onMessage(String topic, ByteBuf payload);
}
