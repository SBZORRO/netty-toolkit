//package com.sbzorro;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//
//import org.springframework.boot.SpringApplication;
//
//import com.google.gson.Gson;
//
//public class AppPathru {
//  public static final Gson GSON = new Gson();
//
//  public static final ScheduledExecutorService EXE = Executors
//      .newSingleThreadScheduledExecutor();
//
//  public static final ExecutorService single_exe = Executors
//      .newSingleThreadExecutor();
//
//  public static void main(String[] args) {
//
//    LogUtil.DEBUG.info(PropUtil.APP_START);
//
//    SpringApplication.run(HttpServer.class, args);
//
//  }
//
//}
