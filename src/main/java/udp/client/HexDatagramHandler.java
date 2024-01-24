package udp.client;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import tcp.client.NettyWrapper;

/**
 * Hello world!
 *
 */
public class HexDatagramHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg)
      throws Exception {

    byte[] ba = new byte[msg.content().readableBytes()];
    msg.content().readBytes(ba);

    String ip = msg.sender().getHostString();
    int port = msg.sender().getPort();

    String host = ip + ":" + port;
    LogUtil.SOCK.info(LogUtil.UDP_MARKER,
        host + " >>> " + HexByteUtil.byteToHex(ba));
    NettyWrapper.LAST_RESP.put(host, HexByteUtil.byteToHex(ba));
  }
}
