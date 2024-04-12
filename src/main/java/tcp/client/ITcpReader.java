package tcp.client;

public interface ITcpReader {

  void onMessage(byte[] ba);
}
