package modbus;

public class Recv {

  public String ip;

  byte[] seq = new byte[2];
  byte[] protocal = new byte[2];
  byte[] len = new byte[2];
  public byte unitId;
  public byte funcCode;

  byte[] offset = new byte[2];
  byte[] reg = new byte[2];

  byte amount;
  byte[] data;

  int status;

  public Recv(byte[] ba) {
    System.arraycopy(ba, 0, seq, 0, 2);
    System.arraycopy(ba, 2, protocal, 0, 2);
    System.arraycopy(ba, 4, len, 0, 2);

    unitId = ba[6];
    funcCode = ba[7];

    if (funcCode == 0x06) {

    } else if (funcCode == 0x03) {
      amount = ba[8];
      data = new byte[amount];
      System.arraycopy(ba, 9, data, 0, amount);
    }
  }

//  String status() {
//    if (status == null || "".equals(status)) {
//      HexByteUtil.byteToHex(data);
//    }
//    return status;
//  }

  public int statusBitMap() {
    int s = 0;
    int p = 0;
    if (data != null && data.length > 0 && status == 0) {
      for (int i = data.length - 2; i >= 0; i = i - 2) {
        p = ((data[i] & 0xff) << 8) | (data[i + 1] & 0xff);
        if (p == 1 || p == 0) {
          s = s | (p << (i / 2));
        }
      }
      status = s;
    }
    return status;
  }

//  0000000: 0002 0000 0013 0103 1000 0100 0100 0100
//  0000010: 0100 0100 0100 0100 01
}
