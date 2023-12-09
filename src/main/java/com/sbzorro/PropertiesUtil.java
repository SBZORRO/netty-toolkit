package com.sbzorro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class PropertiesUtil {

  public static final ResourceBundle bundle = init();

  public static ResourceBundle init() {
    try (FileInputStream file
        = new FileInputStream(new File("dt.properties"))) {
      return new PropertyResourceBundle(file);
    } catch (FileNotFoundException e) {
      LogUtil.DEBUG.info("PROPERTIES NOT FOUND USING DEFAULT");
    } catch (IOException e) {
      LogUtil.DEBUG.info("ERROR READING PROPERTIES USING DEFAULT");
    }
    return ResourceBundle.getBundle("dt");
  }

  public static final String APP_START = bundle.getString("app_start");

  public static final int UDP_PORT
      = Integer.parseInt(bundle.getString("udp_port"));
  public static final String UDP_HOST
      = bundle.getString("udp_host");

  public static final int LOCAL_MQTT_PORT
      = Integer.parseInt(bundle.getString("local_mqtt_port"));
  public static final String LOCAL_MQTT_HOST
      = bundle.getString("local_mqtt_host");
  public static final int REMOTE_MQTT_PORT
      = Integer.parseInt(bundle.getString("remote_mqtt_port"));
  public static final String REMOTE_MQTT_HOST
      = bundle.getString("remote_mqtt_host");

  public static final String PUB_TOPIC = bundle.getString("pub_topic");
  public static final String SUB_TOPIC = bundle.getString("sub_topic");

  public static final String INFLUXDB_URL = bundle.getString("influxdb_url");
  public static final String INFLUXDB_USERNAME
      = bundle.getString("influxdb_username");
  public static final String INFLUXDB_PASSWORD
      = bundle.getString("influxdb_password");
  public static final String INFLUXDB_TOKEN
      = bundle.getString("influxdb_token");
  public static final int INFLUXDB_TIMEOUT
      = Integer.parseInt(bundle.getString("influxdb_timeout"));
  public static final String INFLUXDB_ORG = bundle.getString("influxdb_org");
  public static final String INFLUXDB_BUCKET
      = bundle.getString("influxdb_bucket");
  public static final String INFLUXDB_GAP = bundle.getString("influxdb_gap");

  public static final int REQ_INTERVAL
      = Integer.parseInt(bundle.getString("req_interval"));
  public static final int RETRY_INTERVAL
      = Integer.parseInt(bundle.getString("retry_interval"));

  public static final int TCP_PORT
      = Integer.parseInt(bundle.getString("tcp_port"));
  public static final String TCP_HOST = bundle.getString("tcp_host");

  public static final String MYSQL_DRIVER_NAME
      = bundle.getString("mysql_driver_name");
  public static final String MYSQL_URL = bundle.getString("mysql_url");
  public static final String MYSQL_USERNAME
      = bundle.getString("mysql_username");
  public static final String MYSQL_PASSWORD
      = bundle.getString("mysql_password");
}
