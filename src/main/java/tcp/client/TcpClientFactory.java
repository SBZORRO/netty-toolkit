package tcp.client;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;
import com.sbzorro.PropUtil;

import io.netty.buffer.Unpooled;

public class TcpClientFactory {

  public static NettyFactory bind(int port, String dcd, String args)
      throws InterruptedException {
    NettyFactory client = new TcpServer(port);
    return bootstrap(client, dcd, args);
  }

  public static NettyFactory
      connect(String ip, int port, String dcd, String args)
          throws InterruptedException {
    NettyFactory client = new TcpClient(ip, port);
    return bootstrap(client, dcd, args);
  }

  public static NettyFactory
      bootstrap(NettyFactory client, String dcd, String args)
          throws InterruptedException {
    client = client.cacheIn();
    if (client.isOk()) {
      return client;
    }
    synchronized (client) {
      if (client.isOk()) {
        return client;
      }
      client.init(InitializerFactory.newInitializer(dcd, args));
      client.listeners(new TcpConnectionListener());
      client.bootstrap().sync();
    }
    return client;
  }

  public static NettyFactory connect(String ip, int port)
      throws InterruptedException {
    return connect(ip, port, null, null);
  }

  public static String send(String ip, int port, String msg)
      throws InterruptedException {
    NettyFactory client = connect(ip, port);
    send0(client, msg);
    return msg;
  }

  public static String
      once(String ip, int port, String msg, String dcd, String args)
          throws InterruptedException {

    try (NettyFactory client = connect(ip, port, dcd, args)) {
      client.removeHandler("rcnct");
      send0(client, msg);
      return client.waitForIt();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return "And Then There Were None";
  }

  static void send0(NettyFactory client, String msg)
      throws InterruptedException {
    String host = client.host();
    NettyFactory.LAST_CMD.put(host, msg);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, host + " <<< " + msg);

    if (HexByteUtil.isHex(msg)) {
      client.writeAndFlush(
          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg))).sync();
    } else {
      client.writeAndFlush(Unpooled.copiedBuffer(msg.getBytes())).sync();
    }
  }

  static void sendRetrans(NettyFactory client, String msg) {
    RetransmissionHandler<String> handler = new RetransmissionHandler<>(
        (originalMessage) -> {
          try {
            send0(client, msg);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }, msg, NettyFactory.EXE, PropUtil.REQ_INTERVAL, PropUtil.REQ_MAX);
    handler.start();
  }

}
