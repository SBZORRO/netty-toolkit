package tcp.client;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.sbzorro.LogUtil;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

public class TcpConnectionListener implements ChannelFutureListener {

  @Override
  public void operationComplete(ChannelFuture channelFuture) throws Exception {
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER,
        "operationComplete: TcpConnectionListener: "
            + channelFuture.channel().remoteAddress());
    String host = ((InetSocketAddress) channelFuture.channel().remoteAddress())
        .getHostString();
    int port = ((InetSocketAddress) channelFuture.channel().remoteAddress())
        .getPort();
    if (!channelFuture.isSuccess()) {
      LogUtil.SOCK.info(LogUtil.SOCK_MARKER,
          "RECONNECT: TcpConnectionListener: "
              + channelFuture.channel().remoteAddress());

      final EventLoop loop = channelFuture.channel().eventLoop();
      loop.schedule(() -> TcpClient.map.get(host + port).connect(), 5,
          TimeUnit.SECONDS);

//      TcpClient.map.get(host + port).connect();
//      }, 1000, TimeUnit.MILLISECONDS);
    } else if (channelFuture.isSuccess()) {
      LogUtil.SOCK.info(LogUtil.SOCK_MARKER, "SUCCESS: TcpConnectionListener: "
          + channelFuture.channel().remoteAddress());

//      MyMqttClient.CLIENT.on("DigitalTwin/#", (topic, payload) -> {
//        byte[] array = new byte[payload.readableBytes()];
//        payload.getBytes(0, array);
//        LogUtil.MQTT.info(LogUtil.mqttMarker(topic), new String(array));
//      });
    }
  }
}
