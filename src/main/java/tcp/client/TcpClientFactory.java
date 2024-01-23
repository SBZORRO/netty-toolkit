package tcp.client;

import com.sbzorro.PropUtil;

public class TcpClientFactory {

//  public static NettyFactory bind(int port, String dcd, String args)
//      throws InterruptedException {
//    NettyFactory client = new TcpServer(port);
//    return NettyFactory.bootstrap(client,
//        InitializerFactory.newInitializer(dcd, args),
//        new TcpConnectionListener());
//  }

//  public static NettyFactory
//      bootstrap(String ip, int port, String dcd, String args)
//          throws InterruptedException {
//    NettyFactory client = new TcpClient(ip, port);
//    return NettyFactory.bootstrap(client,
//        InitializerFactory.newInitializer(dcd, args),
//        new TcpConnectionListener());
//  }

//  public static NettyFactory bootstrap(String ip, int port)
//      throws InterruptedException {
//    return bootstrap(ip, port, null, null);
//  }

//  public static String send(String ip, int port, String msg)
//      throws InterruptedException {
//    NettyFactory client = bootstrap(ip, port);
//    NettyFactory.send(client, msg);
//    return msg;
//  }

//  public static void send(NettyFactory client, String msg)
//      throws InterruptedException {
//    String host = client.host();
//    NettyFactory.LAST_CMD.put(host, msg);
//    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, host + " <<< " + msg);
//
//    if (HexByteUtil.isHex(msg)) {
//      client.writeAndFlush(
//          Unpooled.copiedBuffer(HexByteUtil.cmdToByteNoSep(msg))).sync();
//    } else {
//      client.writeAndFlush(Unpooled.copiedBuffer(msg.getBytes())).sync();
//    }
//  }

  static void sendRetrans(NettyWrapper client, String msg) {
    RetransmissionHandler<String> handler = new RetransmissionHandler<>(
        (originalMessage) -> {
          try {
            ClientFactory.send(client, msg);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }, msg, NettyWrapper.EXE, PropUtil.REQ_INTERVAL, PropUtil.REQ_MAX);
    handler.start();
  }

}
