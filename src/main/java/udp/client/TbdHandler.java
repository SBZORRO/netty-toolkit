package udp.client;

import java.util.concurrent.TimeUnit;

import com.sbzorro.HexByteUtil;
import com.sbzorro.LogUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import tcp.client.NettyFactory;

/**
 * Hello world!
 *
 */
public class TbdHandler extends SimpleChannelInboundHandler<DatagramPacket> {

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
    NettyFactory.LAST_RESP.put(host, HexByteUtil.byteToHex(ba));
  }

//  @Override
//  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//    final EventLoop eventLoop = ctx.channel().eventLoop();
//    eventLoop.schedule(new Runnable() {
//      @Override
//      public void run() {
////        WolUtil.INSTANCE.bind();
//      }
//    }, 1L, TimeUnit.SECONDS);
//    super.channelInactive(ctx);
//  }
}
