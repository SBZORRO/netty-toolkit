//package com.sbzorro;
//
//import java.io.IOException;
//
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.TypeAdapter;
//import com.google.gson.stream.JsonReader;
//import com.google.gson.stream.JsonWriter;
//
//import tcp.client.TcpClient;
//import tcp.client.TcpClientFactory;
//import udp.client.UdpClientFactory;
//
//@SpringBootApplication
//@RestController
//public class HttpServer {
//
//  @CrossOrigin
//  @GetMapping("test")
//  public static Object test() {
//    return "test";
//  }
//
//  @CrossOrigin
//  @GetMapping("connections")
//  public static Object connections() {
//    return TcpClient.CLIENTS;
//  }
//
//  @CrossOrigin
//  @GetMapping("{proto}/{ip}/{port}")
//  public static Object connect(
//      @PathVariable String proto,
//      @PathVariable String ip,
//      @PathVariable int port) throws Exception {
//
//    return TcpClientFactory.connect(ip, port).toString();
//  }
//
//  @CrossOrigin
//  @GetMapping("{proto}/{ip}/{port}/recv/{dcd}/{args}")
//  public static Object connect(
//      @PathVariable String proto, @PathVariable String ip,
//      @PathVariable int port, @PathVariable String dcd,
//      @PathVariable String args) throws Exception {
//
//    return TcpClientFactory.connect(ip, port, dcd, args).toString();
//  }
//
//  @CrossOrigin
//  @GetMapping("{proto}/{ip}/{port}/send/{cmd}")
//  public static Object send(
//      @PathVariable String proto, @PathVariable String ip,
//      @PathVariable int port, @PathVariable String cmd)
//      throws Exception {
//
//    return TcpClientFactory.send(ip, port, cmd);
//  }
//
//  @CrossOrigin
//  @GetMapping("{proto}/{ip}/{port}/once/{cmd}/recv/{dcd}/{args}")
//  public static Object once(
//      @PathVariable String proto, @PathVariable String ip,
//      @PathVariable int port, @PathVariable String cmd,
//      @PathVariable String dcd, @PathVariable String args)
//      throws Exception {
//    if ("tcp".equals(proto)) {
//      return TcpClientFactory.once(ip, port, cmd, dcd, args);
//    } else if ("udp".equals(proto)) {
//      return UdpClientFactory.once(ip, port, cmd, dcd, args);
//    }
//    return "buyinggaizoudaozhe";
//  }
//
//  @GetMapping("/stacktraces")
//  public String stack() {
//
//    GsonBuilder builder = new GsonBuilder();
//    builder.registerTypeAdapter(StackTraceElement.class,
//        new StackTraceElementAdapter());
//
//    Gson gson = builder.create();
//    return gson.toJson(Thread.getAllStackTraces());
//  }
//
//  class StackTraceElementAdapter extends TypeAdapter<StackTraceElement> {
//    @Override
//    public void write(JsonWriter writer, StackTraceElement value)
//        throws IOException {
//      if (value == null) {
//        writer.nullValue();
//        return;
//      }
//      writer.value(value.toString());
//    }
//
//    @Override
//    public StackTraceElement read(JsonReader in) throws IOException {
//      // TODO Auto-generated method stub
//      return null;
//    }
//  }
//
//}
