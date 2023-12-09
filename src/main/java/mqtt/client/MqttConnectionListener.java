package mqtt.client;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.sbzorro.App;
import com.sbzorro.LogUtil;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

public class MqttConnectionListener implements ChannelFutureListener {
  @Override
  public void operationComplete(ChannelFuture channelFuture) throws Exception {
    LogUtil.SOCK.info("operationComplete: MqttConnectionListener: "
        + channelFuture.channel().remoteAddress());
    String host
        = ((InetSocketAddress) channelFuture.channel().remoteAddress())
            .getHostString();
    int port
        = ((InetSocketAddress) channelFuture.channel().remoteAddress())
            .getPort();
    if (!channelFuture.isSuccess()) {
      LogUtil.SOCK.info("RECONNECT: MqttConnectionListener: "
          + channelFuture.channel().remoteAddress());

      final EventLoop loop = channelFuture.channel().eventLoop();
      loop.schedule(() -> App.sub("test-zt"), 5, TimeUnit.SECONDS);

//      App.sub("test-zt");
    } else if (channelFuture.isSuccess()) {
      LogUtil.SOCK.info("SUCCESS: MqttConnectionListener: "
          + channelFuture.channel().remoteAddress());
//  MyMqttClient.CLIENT.on("DigitalTwin/#", (topic, payload) -> {
//    byte[] array = new byte[payload.readableBytes()];
//    payload.getBytes(0, array);
//    LogUtil.MQTT.info(LogUtil.mqttMarker(topic), new String(array));
//  });
    }
  }
}
