package com.sbzorro;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class LogUtil {
//    public static void init() {
//        try (BufferedInputStream in = new BufferedInputStream(
//                ClassLoader.getSystemResourceAsStream("log4j2.xml"))) {
//
//            ConfigurationSource source;
//            source = new ConfigurationSource(in);
//            Configurator.initialize(null, source);
//        } catch (FileNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            System.exit(1);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }

  public static final Logger DEBUG = LogManager.getLogger("debug");
  public static final Logger MQTT = LogManager.getLogger("mqtt");
  public static final Logger SOCK = LogManager.getLogger("sock");
  public static final Logger MES = LogManager.getLogger("mes");

  public static final Marker HTTP_MARKER = MarkerManager.getMarker("http");
  public static final Marker MES_MARKER
      = MarkerManager.getMarker("mes").setParents(HTTP_MARKER);
  public static final Marker DA_MARKER
      = MarkerManager.getMarker("da").setParents(HTTP_MARKER);
  public static final Marker REQ_MARKER
      = MarkerManager.getMarker("req").setParents(HTTP_MARKER);
  public static final Marker RES_MARKER
      = MarkerManager.getMarker("res").setParents(HTTP_MARKER);

  public static final Marker SOCK_MARKER = MarkerManager.getMarker("sock");
  public static final Marker MQTT_MARKER
      = MarkerManager.getMarker("mqtt").setParents(SOCK_MARKER);
  public static final Marker UDP_MARKER
      = MarkerManager.getMarker("udp").setParents(SOCK_MARKER);
  public static final Marker TCP_MARKER
      = MarkerManager.getMarker("tcp").setParents(SOCK_MARKER);

  public static Marker mqttMarker(String topic) {
    return MarkerManager.getMarker(topic).setParents(MQTT_MARKER);
  }
}
