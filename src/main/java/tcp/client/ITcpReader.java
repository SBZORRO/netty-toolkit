package tcp.client;

public interface ITcpReader {

  void onMessage(String host, byte[] ba);
}
