package mqtt.core;

public interface IMqttHandler {

  void onMessage(String topic, byte[] payload);
}
