package com.sbzorro;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

//import org.springframework.boot.SpringApplication;

import com.google.gson.Gson;

public class App {

  public static final int TCP_PORT = PropUtil.TCP_PORT;
  public static final String TCP_HOST = PropUtil.TCP_HOST;

  public static final int UDP_PORT = PropUtil.UDP_PORT;
  public static final String UDP_HOST = PropUtil.UDP_HOST;

  public static final String url = PropUtil.INFLUXDB_URL;
  public static final String username = PropUtil.INFLUXDB_USERNAME;
  public static final String password = PropUtil.INFLUXDB_PASSWORD;
  public static final String token = PropUtil.INFLUXDB_TOKEN;
  public static final int timeout = PropUtil.INFLUXDB_TIMEOUT;
  public static final String gap = PropUtil.INFLUXDB_GAP;

  public static final String org = PropUtil.INFLUXDB_ORG;
  public static final String bucket = PropUtil.INFLUXDB_BUCKET;

  public static final int REMOTE_MQTT_PORT = PropUtil.REMOTE_MQTT_PORT;
  public static final String REMOTE_MQTT_HOST = PropUtil.REMOTE_MQTT_HOST;

  public static final String mqtt_host = PropUtil.LOCAL_MQTT_HOST;
  public static final int mqtt_port = PropUtil.LOCAL_MQTT_PORT;
  public static final String pub_topic = PropUtil.PUB_TOPIC;
  public static final String sub_topic = PropUtil.SUB_TOPIC;

  public static final int RETRY_INTERVAL = PropUtil.RETRY_INTERVAL;
  public static final int REQ_INTERVAL = PropUtil.REQ_INTERVAL;


  public static final Gson GSON = new Gson();

  public static final ScheduledExecutorService EXE = Executors
      .newSingleThreadScheduledExecutor();

  public static final ExecutorService single_exe = Executors
      .newSingleThreadExecutor();

  public static void main(String[] args) {

    LogUtil.DEBUG.info(PropUtil.APP_START);

//    SpringApplication.run(HttpServer.class, args);

  }

}
