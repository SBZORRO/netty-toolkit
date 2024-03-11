package modbus;

import java.net.InetSocketAddress;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class TcpChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

  public static void main(String[] args) {}

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)
      throws Exception {
    byte[] ba = new byte[msg.readableBytes()];
    msg.readBytes(ba);

    String m = HexByteUtil.byteToHex(ba);
    LogUtil.SOCK.info(LogUtil.SOCK_MARKER, ">:{}", m);

    Recv recv = new Recv(ba);
    if (recv.funcCode == 0x06) {
      return;
    }

    InetSocketAddress addr = ((InetSocketAddress) ctx.channel()
        .remoteAddress());
    String ip = addr.getHostString();
    recv.ip = ip;
  }
}
