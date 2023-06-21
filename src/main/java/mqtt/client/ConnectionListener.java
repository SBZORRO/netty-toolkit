package mqtt.client;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

public class ConnectionListener implements ChannelFutureListener {
  @Override
  public void operationComplete(ChannelFuture channelFuture) throws Exception {
    if (!channelFuture.isSuccess()) {
      final EventLoop loop = channelFuture.channel().eventLoop();
      loop.schedule(() -> {
//          String host
//              = ((InetSocketAddress) channelFuture.channel().remoteAddress())
//                  .getHostString();
//          int port
//              = ((InetSocketAddress) channelFuture.channel().remoteAddress())
//                  .getPort();
        channelFuture.channel().pipeline()
            .connect(channelFuture.channel().remoteAddress());
//          MyMqttClient.connect(host, port);
      }, 1000, TimeUnit.MILLISECONDS);
    } else if (channelFuture.isSuccess())

    {

//      MyMqttClient.CLIENT.on("DigitalTwin/#", (topic, payload) -> {
//        byte[] array = new byte[payload.readableBytes()];
//        payload.getBytes(0, array);
//        LogUtil.MQTT.info(LogUtil.mqttMarker(topic), new String(array));
//      });
    }
  }
}
