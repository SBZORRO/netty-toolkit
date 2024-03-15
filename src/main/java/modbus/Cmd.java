package modbus;

import java.util.Random;

import com.sbzorro.HexByteUtil;

public class Cmd {

  public static final int read = 0x03;
  public static final int ctrl = 0x06;
  public static final int seq = 0;
  public static final int register = 0;
  public static final int length = 0x10;

  public static final int seq_offset = 0;
  public static final int pid_offset = 2;
  public static final int len_offset = 4;
  public static final int dev_id_offset = 6;
  public static final int func_code_offset = 7;
  public static final int reg_offset = 8;
  public static final int cmd_offset = 10;

  public static byte[] read(int devId, int start, int len) {
    byte[] cmd = skel();
    cmd[dev_id_offset] = (byte) (devId & 0xff);

    cmd[func_code_offset] = read;

    cmd[reg_offset] = (byte) ((start >> 8) & 0xff);
    cmd[reg_offset + 1] = (byte) (start & 0xff);

    cmd[cmd_offset] = (byte) ((len >> 8) & 0xff);
    cmd[cmd_offset + 1] = (byte) (len & 0xff);

    return cmd;
  }

  public static byte[] ctrl(int devId, int start, int oc) {
    byte[] cmd = skel();
    cmd[dev_id_offset] = (byte) (devId & 0xff);

    cmd[func_code_offset] = ctrl;

    cmd[reg_offset] = (byte) ((start >> 8) & 0xff);
    cmd[reg_offset + 1] = (byte) (start & 0xff);

    cmd[cmd_offset + 1] = (byte) (oc & 0xff);
    return cmd;
  }

  public static byte[] open(int devId, int start) {
    byte[] cmd = skel();
    cmd[dev_id_offset] = (byte) (devId & 0xff);

    cmd[func_code_offset] = ctrl;

    cmd[reg_offset] = (byte) ((start >> 8) & 0xff);
    cmd[reg_offset + 1] = (byte) (start & 0xff);

    cmd[cmd_offset + 1] = (byte) (1 & 0xff);

    return cmd;
  }

  public static byte[] shut(int devId, int start) {
    byte[] cmd = skel();
    cmd[dev_id_offset] = (byte) (devId & 0xff);

    cmd[func_code_offset] = ctrl;

    cmd[reg_offset] = (byte) ((start >> 8) & 0xff);
    cmd[reg_offset + 1] = (byte) (start & 0xff);

    return cmd;
  }

  public static final Random rand = new Random();

  public static byte[] skel() {
    int n = rand.nextInt(); // 生成一个随机整数
    return new byte[] { (byte) (n & 0xff), (byte) ((n >> 8) & 0xff), 0x00,
        0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  }

//  Modbus TCP is:
//
//    0001 0000 0006 11 03 006B 0003
//
//  0001: Transaction Identifier
//  0000: Protocol Identifier
//  0006: Message Length (6 bytes to follow)
//  11: The Unit Identifier  (17 = 11 hex)
//  03: The Function Code (read Analog Output Holding Registers)
//  006B: The Data Address of the first register requested. (40108-40001 = 107 =6B hex)
//  0003: The total number of registers requested. (read 3 registers 40108 to 40110)

  public static void main(String[] args) {
    byte[] toSend = Cmd.read(1, 0, 0x08);
    System.out.println(HexByteUtil.byteToHex(toSend));
  }
}

//0001 0000 0006 02 03 0000 ffff

//00 01 
//00 00 
//00 b5 
//02 
//03 
//b2 178
//00 00 00 00 02 96 02 98 00 fe 00 12 00 16 00 5c 01 ed 00 00 00 06 00 06 00
//00 00 0a 00 0a ff ff 30 30 30 30 30 32 07 e8 00 03 00 06 00 11 00 19 00 39 00 
//08 00 00 ff ff ff ff 00 00 ff ff ff ff 00 00 00 00 00 00 00 00 00 78 00 00 00 
//00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 
//00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 
//00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 
//00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 
