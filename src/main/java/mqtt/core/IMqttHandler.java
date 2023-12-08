package mqtt.core;

import io.netty.buffer.ByteBuf;

public interface IMqttHandler {

  void onMessage(String topic, ByteBuf payload);
}
