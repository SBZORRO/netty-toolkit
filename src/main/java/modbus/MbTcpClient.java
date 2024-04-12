package modbus;

import tcp.client.ITcpReader;
import tcp.client.InitializerFactory.MyReaderInitializer;
import tcp.client.NettyWrapper;
import tcp.client.TcpClient;
import tcp.client.TcpConnectionListener;

public class MbTcpClient {

  public static void main(String[] args) {}

  public static TcpClient connect(String ip, int port, ITcpReader reader)
      throws InterruptedException {
    NettyWrapper client = new TcpClient(ip, port)
        .addListener(new TcpConnectionListener())
        .init(new MyReaderInitializer(reader));
    return (TcpClient) NettyWrapper.bootstrap(client);
  }
}
