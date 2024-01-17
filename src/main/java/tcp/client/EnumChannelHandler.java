package tcp.client;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

//public class EnumChannelHandler extends ByteToMessageDecoder {
//  private final InetSocketAddress remoteAddress;
//
//  public EnumChannelHandler(InetSocketAddress remoteAddress) {
//    this.remoteAddress = remoteAddress;
//  }
//
//  @Override
//  protected void
//      decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
//          throws Exception {
//    byte[] file = logEvent.getLogfile().getBytes(CharsetUtil.UTF_8);
//    byte[] msg = logEvent.getMsg().getBytes(CharsetUtil.UTF_8);
//    ByteBuf buf = channelHandlerContext.alloc()
//        .buffer(file.length + msg.length + 1);
//    buf.writeBytes(file);
//    buf.writeByte(LogEvent.SEPARATOR);
//    buf.writeBytes(msg);
//    out.add(new DatagramPacket(buf, remoteAddress));
//  }
//}

//public final class EnumChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
//
//  public static void main(String[] args) {}
//
//  public static final Set<String> resp = new HashSet<String>() {
//    {
//      add("start");
//      add("running");
//      add("stoping");
//      add("stopped");
//      add("restart");
//      add("forced");
//      add("command error");
//    }
//  };
//
//  public static final Set<String> cmd = new HashSet<String>() {
//    {
//      add("start");
//      add("stop");
//      add("restart");
//      add("forced");
//      add("read");
//    }
//  };
//
//  @Override
//  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)
//      throws Exception {
//    byte[] ba = new byte[msg.readableBytes()];
//    msg.readBytes(ba);
//
//    String ip = ((InetSocketAddress) ctx.channel().remoteAddress())
//        .getHostString();
//    int port = ((InetSocketAddress) ctx.channel().remoteAddress())
//        .getPort();
//
//    String host = ip + ":" + port;
//    LogUtil.SOCK.info(LogUtil.SOCK_MARKER,
//        host + " >>> " + HexByteUtil.byteToHex(ba));
//
//    NettyFactory.LAST_RESP.put(host, HexByteUtil.byteToHex(ba));
//
////    App.CLIENT.publish("open_exhibition_hall", host + " OK");
//
////    App.EXE.execute(
////        () -> ReadToInflux.writeDevStatus(host, "OK"));
//  }
//}
