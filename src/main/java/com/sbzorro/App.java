package com.sbzorro;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import mqtt.client.MyMqttClient;
import websocket.WebSocketServer;

public class App {

  public static final int TCP_PORT = PropertiesUtil.TCP_PORT;
  public static final String TCP_HOST = PropertiesUtil.TCP_HOST;

  public static final int UDP_PORT = PropertiesUtil.UDP_PORT;
  public static final String UDP_HOST = PropertiesUtil.UDP_HOST;

  public static final String url = PropertiesUtil.INFLUXDB_URL;
  public static final String username = PropertiesUtil.INFLUXDB_USERNAME;
  public static final String password = PropertiesUtil.INFLUXDB_PASSWORD;
  public static final String token = PropertiesUtil.INFLUXDB_TOKEN;
  public static final int timeout = PropertiesUtil.INFLUXDB_TIMEOUT;
  public static final String gap = PropertiesUtil.INFLUXDB_GAP;

  public static final String org = PropertiesUtil.INFLUXDB_ORG;
  public static final String bucket = PropertiesUtil.INFLUXDB_BUCKET;

  public static final String mqtt_host = PropertiesUtil.LOCAL_MQTT_HOST;
  public static final int mqtt_port = PropertiesUtil.LOCAL_MQTT_PORT;
  public static final String pub_topic = PropertiesUtil.PUB_TOPIC;
  public static final String sub_topic = PropertiesUtil.SUB_TOPIC;

  public static final int REMOTE_MQTT_PORT = PropertiesUtil.REMOTE_MQTT_PORT;
  public static final String REMOTE_MQTT_HOST = PropertiesUtil.REMOTE_MQTT_HOST;

  public static final int RETRY_INTERVAL = PropertiesUtil.RETRY_INTERVAL;
  public static final int REQ_INTERVAL = PropertiesUtil.REQ_INTERVAL;

  public static final String weitingzhuozidengguang_ip = "192.168.11.248";
  public static final String wtzzdg_light1_open = "fe 05 00 00 ff 00 98 35";
  public static final String wtzzdg_light2_open = "fe 05 00 01 ff 00 c9 f5";
  public static final String wtzzdg_light1_close = "fe 05 00 00 00 00 d9 c5";
  public static final String wtzzdg_light2_close = "fe 05 00 01 00 00 88 05";
  public static final int wtzzdg_port = 5899;

  public static final String[] led_ip_arr = new String[] {
      "192.168.11.230",
      "192.168.11.231",
      "192.168.11.232",
      "192.168.11.241"
  };

  public static final String led_open = "cc dd a1 01 00 00 ff ff a0 40";
  public static final String led_close = "cc dd a1 01 ff ff ff ff 9e 3c";
  public static final int led_port = 50000;

  public static final Map<String, String> all_dev = new HashMap<String, String>() {
    {
      // 灯
      put("192.168.11.225:5899:1-1", "数字孪生");
      put("192.168.11.225:5899:1-2", "数字化转型灯光");
      put("192.168.11.225:5899:1-3", "伏锂码云平台");
      put("192.168.11.225:5899:1-4", "数字化生产");
      put("192.168.11.225:5899:1-5", "数字化管控");
      put("192.168.11.225:5899:1-6", "携手向未来");
      put("192.168.11.225:5899:2-4", "机房灯光");
      put("192.168.11.225:5899:2-1", "企业文化屏幕");

      // 主机
      put("192.168.11.243:5899", "工业互联网");
      put("192.168.11.247:5899", "数字孪生");
      put("192.168.11.245:5899", "Box投影");
      put("192.168.11.246:5899", "企业文化");
      put("192.168.11.244:5899", "智慧工厂");

      // 投影仪
      put("192.168.11.214:4352", "投影仪");
      put("192.168.11.216:4352", "投影仪");
      put("192.168.11.217:4352", "投影仪");
      put("192.168.11.218:4352", "投影仪");

      // LED
      put("192.168.11.230:50000", "LED");
      put("192.168.11.231:50000", "LED");
      put("192.168.11.232:50000", "LED");
      put("192.168.11.241:50000", "LED");

      // 桌子
      put("192.168.11.248:5899", "桌子");
    }
  };

  public static final Map<String, Integer> last_cmd = new ConcurrentHashMap<>();

  public static final Gson GSON = new Gson();

  public static final MyMqttClient SUBER = new MyMqttClient();
  public static final MyMqttClient CLIENT = SUBER;
//  public static final MyMqttClient CLIENT = new MyMqttClient();

  public static final ExecutorService EXE = Executors
      .newSingleThreadExecutor();

  public static void main(String[] args) throws Exception {

    try {
      LogUtil.DEBUG.info(PropertiesUtil.APP_START);

      websocketx.server.WebSocketServer.start();
//      SpringApplication.run(HttpServer.class, args);
//      CLIENT.connect("localhost", 1883);
//      CLIENT.connect("mqtt.guoshu-ai.com", 1883);

//      App.test();
//      sub("test");

//      App.start();

//      TcpClient client;
//
//      client = new TcpClient();
//      client.init(new LightInitializer());
//      client.remoteAddress(LightChannelHandler.ip, LightChannelHandler.port);
//      client.connect();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sub(String t) {

//    SUBER.connect("mqtt.guoshu-ai.com", 1883);
    SUBER.connect("localhost", 1883);
    SUBER.subscribe(t, (topic, payload) -> {
      byte[] array = new byte[payload.readableBytes()];
      payload.getBytes(0, array);

      String msg = new String(array);
      LogUtil.MQTT.info(msg);

      try {
        SUBER.publish("open_exhibition_hall", "open_exhibition_hall");

//      {"Id":"192.168.11.205","DeviceType":"Software","Command":"SV!ChooseLight!1-1!0"}

        JsonObject json = GSON.fromJson(msg, JsonObject.class);

//      String id = json.get("Id").getAsString();
        String Command = json.get("Command").getAsString();
        String[] cmdArr = Command.split("!");
        String type = cmdArr[1];

      } catch (Throwable e) {
        e.printStackTrace();
      }
    });
  }

  public static void start() throws InterruptedException {}
}
