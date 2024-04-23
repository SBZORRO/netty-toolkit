package mqtt.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.netty.channel.nio.NioEventLoopGroup;

public class MqttTest {

  int timeout = 1;

  @Test
  public void startTimer() throws InterruptedException {
//    new NioEventLoopGroup().scheduleWithFixedDelay(() -> {
//      System.out.println(timeout);
//      System.out.println(System.currentTimeMillis());
//      timeout = timeout + 1;
//
//    }, timeout, timeout, TimeUnit.SECONDS);
//    Thread.currentThread().join();
  }

  AtomicInteger nextMessageId = new AtomicInteger();

  @Test
  public void getNewMessageIdVariableHeader() throws InterruptedException {

//    for (int i = 0; i < 10; i++) {
//      Thread t = new Thread(() -> {
//        for (int j = 0; j < 0x10000; j++) {
//
//          int messageId = gai();
//          if (messageId == 0) {
//            messageId = gai();
//          }
////        System.out.println(messageId & 0x0000ffff);
//        }
//      });
//      t.start();
//      t.join();
//    }
  }

  public synchronized int gai() {
    int messageId = nextMessageId.getAndIncrement();
    System.out.println(messageId & 0x0000ffff);
    return messageId;
  }
}
