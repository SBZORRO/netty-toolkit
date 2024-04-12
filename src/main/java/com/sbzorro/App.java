package com.sbzorro;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import mqtt.client.MqttClientFactory;
import mqtt.core.IMqttHandler;
import mqtt.core.MqttClientImpl;

public class App {

  public static final Gson GSON = new Gson();

  public static final Map<String, AtomicInteger> map = new HashMap<>();

  public static void main(String[] args) throws Exception {
    MqttClientImpl SUBER = MqttClientFactory.bootstrap("192.168.50.182",
        1883);
    IMqttHandler handler = new IMqttHandler() {
      @Override
      public void onMessage(String topic, ByteBuf payload) {
        byte[] array = new byte[payload.readableBytes()];
        payload.getBytes(0, array);

        String msg = new String(array);
        LogUtil.MQTT.info(LogUtil.MQTT_MARKER,
            topic + ": " + map.get(topic).incrementAndGet());
        LogUtil.MQTT.info(LogUtil.MQTT_MARKER, map);
        JsonObject json = GSON.fromJson(msg, JsonObject.class);

        SUBER.publish("teset",
            Unpooled.copiedBuffer(json.toString().getBytes()),
            MqttQoS.AT_LEAST_ONCE);
      }
    };
    for (int i = 1; i <= 100; i++) {
      String topic = "teset" + i;
      map.put(topic, new AtomicInteger());
      SUBER.on(topic, handler, MqttQoS.AT_LEAST_ONCE);
    }

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
