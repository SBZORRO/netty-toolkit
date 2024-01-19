package com.sbzorro;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.netty.buffer.Unpooled;
import mqtt.client.MqttClientFactory;
import mqtt.core.MqttClientImpl;

public class App2 {

  public static final Gson GSON = new Gson();

  public static void main(String[] args) throws Exception {
    MqttClientImpl SUBER = MqttClientFactory.bootstrap("localhost", 1883);
    SUBER.on("test", (topic, payload) -> {
      byte[] array = new byte[payload.readableBytes()];
      payload.getBytes(0, array);

      String msg = new String(array);
      LogUtil.MQTT.info(msg);

      JsonObject json = GSON.fromJson(msg, JsonObject.class);

      SUBER.publish("test1",
          Unpooled.copiedBuffer(json.toString().getBytes()));
    });
//    try {
//      LogUtil.DEBUG.info(PropUtil.APP_START);
//
//      websocketx.server.WebSocketServer.start();
////      SpringApplication.run(HttpServer.class, args);
////      CLIENT.connect("localhost", 1883);
////      CLIENT.connect("mqtt.guoshu-ai.com", 1883);
//
////      App.test();
////      sub("test");
//
////      App.start();
//
////      TcpClient client;
////
////      client = new TcpClient();
////      client.init(new LightInitializer());
////      client.remoteAddress(LightChannelHandler.ip, LightChannelHandler.port);
////      client.connect();
//
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
  }

  public static void sub(String t) {

//    SUBER.connect("mqtt.guoshu-ai.com", 1883);

  }
}
