package tcp.client;

import java.net.InetSocketAddress;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class RawChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

  public static void main(String[] args) {}

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)
      throws Exception {
    byte[] ba = new byte[msg.readableBytes()];
    msg.readBytes(ba);

    String ip = ((InetSocketAddress) ctx.channel().remoteAddress())
        .getHostString();
    int port = ((InetSocketAddress) ctx.channel().remoteAddress())
        .getPort();

    String host = ip + ":" + port;
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER,
        host + " >>> " + HexByteUtil.byteToHex(ba));
    TcpClientFactory.last_resp.put(host, HexByteUtil.byteToHex(ba));

//    App.CLIENT.publish("open_exhibition_hall", host + " OK");

//    App.EXE.execute(
//        () -> ReadToInflux.writeDevStatus(host, "OK"));
  }
}
