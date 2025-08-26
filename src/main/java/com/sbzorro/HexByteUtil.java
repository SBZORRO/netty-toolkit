package com.sbzorro;

import java.nio.charset.StandardCharsets;

public class HexByteUtil {
  public static final char[] hexChar = new char[] { '0', '1', '2', '3', '4',
      '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

  public static boolean isHex(String msg) {
    if (msg.length() % 2 != 0) {
      return false;
    }
    for (int i = 0; i < msg.length(); i++) {
      char c = msg.charAt(i);
      if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
        return false;
      }
    }
    return true;
  }

//59 45 52 43 20 00 00 00 03 01 00 00 00 00 00 00 39 39 39 39 39 39 39 39 75 00 01 00 00 01 00 00
  public static byte[] cmdToByte(String cmd) {
    byte[] ba = new byte[cmd.length() / 3 + 1];
    for (int i = 0, j = 0; i < cmd.length(); i = i + 3, j++) {
      ba[j] = hexToByte(cmd.charAt(i), cmd.charAt(i + 1));
    }
    return ba;
  }

  public static byte[] cmdToByteNoSep(String cmd) {
    byte[] ba = new byte[cmd.length() / 2];
    for (int i = 0, j = 0; i < cmd.length(); i = i + 2, j++) {
      ba[j] = hexToByte(cmd.charAt(i), cmd.charAt(i + 1));
    }
    return ba;
  }

  public static byte hexToByte(char x, char y) {
    char c = x;
    byte b = 0;
    if (c >= '0' && c <= '9') {
      b = (byte) (((c - 48) & 0xf) << 4);
    } else if (c >= 'a' && c <= 'f') {
      b = (byte) (((c - 87) & 0xf) << 4);
    }

    c = y;
    if (c >= '0' && c <= '9') {
      b |= (byte) (((c - 48) & 0xf));
    } else if (c >= 'a' && c <= 'f') {
      b |= (byte) (((c - 87) & 0xf));
    }
    return b;
  }

  public static String byteToHex(int b) {
    return hexChar[(b >> 4) & 0xf] + hexChar[b & 0xf] + "";
  }

  public static String byteToHex(byte b) {
    return hexChar[(b >> 4) & 0xf] + hexChar[b & 0xf] + "";
  }

  public static String byteToHex(byte[] ba) {
    StringBuilder sb = new StringBuilder();
    for (byte b : ba) {
      sb.append(HexByteUtil.hexChar[(b >> 4) & 0xf]);
      sb.append(HexByteUtil.hexChar[b & 0xf]);
      sb.append(" ");
    }
    return sb.toString();
  }

  public static float toReal(byte[] ba) {
    return Float.intBitsToFloat(HexByteUtil.toU2int(ba));
  }

  public static float toFloat(byte[] ba) {
    return Float.intBitsToFloat(HexByteUtil.toU2int(ba));
  }

  public static short toShort(byte[] ba) {
    short p = 0;
    for (int i = 0; i < ba.length; i++) {
      p = (short) ((p << 8) | (ba[i] & 0xff));
    }
    return p;
  }

  public static int toU2int(byte[] ba) {
    int p = 0;
    for (int i = 0; i < ba.length; i++) {
      p = (p << 8) | (ba[i] & 0xff);
    }
    return p;
  }

  public static long toU4int(byte[] ba) {
    long p = 0;
    for (int i = 0; i < ba.length; i++) {
      p = (p << 8) | (ba[i] & 0xff);
    }
    return p;
  }

  public static int toU2int(byte[] ba, int offset, int len) {
    int p = 0;
    for (int i = offset; i < offset + len; i++) {
      p = (p << 8) | (ba[i] & 0xff);
    }
    return p;
  }

  public static int toU2intLe(byte[] ba, int offset, int len) {
    int p = 0;
    for (int i = offset; i < offset + len; i++) {
      p = p | (ba[i] & 0xff) << 8 * (i - offset);
    }
    return p;
  }

  public static long toU4int(byte[] ba, int offset, int len) {
    long p = 0;
    for (int i = offset; i < offset + len; i++) {
      p = (p << 8) | (ba[i] & 0xff);
    }
    return p;
  }

  // binary to decimal
  public static int[] assembleByteLe(byte[] array, int size) {
    int[] res = new int[array.length / size];
    for (int i = 0; i < array.length; i = i + size) {
      int p = 0;
      for (int j = 0; j < size; j++) {
        p = p | (array[i + j] & 0xff) << 8 * j;
      }
      res[i / size] = p;
    }
    return res;
  }

  public static int[] assembleByteBe(byte[] array, int size) {
    int[] res = new int[array.length / size];
    for (int i = 0; i < array.length; i = i + size) {
      int p = 0;
      for (int j = 0; j < size; j++) {
        p = (p << 8) | (array[i + j] & 0xff);
      }
      res[i / size] = p;
    }
    return res;
  }

  public static int[] assembleByteBe(byte[] array, int offset, int len,
      int size) {
    int[] res = new int[len / size];
    for (int i = offset, k = 0; i < offset + len; i = i + size, k++) {
      int p = 0;
      for (int j = 0; j < size; j++) {
        p = (p << 8) | (array[i + j] & 0xff);
      }
      res[k] = p;
      System.out.print(p + " ");
    }
    System.out.println();
    return res;
  }

  public static int[] assembleByteLe(byte[] array, int offset, int[] size) {
    int[] res = new int[size.length];
    for (int i = offset, l = 0, k = 0;
        k < size.length;
        i = i + size[k], k++, l++) {
      int p = 0;
      for (int j = 0; j < size[k]; j++) {
        p |= (array[i + j] & 0xff) << 8 * j;
      }
      res[l] = p;
    }
    return res;
  }

  public static int[] assembleByteBe(byte[] array, int offset, int[] size) {
    int[] res = new int[size.length];
    for (int i = offset, l = 0, k = 0;
        k < size.length;
        i = i + size[k], k++, l++) {
//      int p = array[i] < 0 ? -1 : 0; // signed
      int p = 0; // signed
      for (int j = 0; j < size[k]; j++) {
        p = (p << 8) | (array[i + j] & 0xff);
      }
      res[l] = p;
    }
    return res;
  }

  public static byte[] reorderByteLe(byte[] array, int offset, int[] size) {
    byte[] res = new byte[array.length];
    for (int i = offset, k = 0; k < size.length; i = i + size[k], k++) {
      for (int j = size[k] - 1, l = 0; j >= 0; j--, l++) {
        res[i + l] = array[i + j];
      }
    }
    return res;
  }

  public static float[] assembleByteBeToReal(byte[] array, int size) {
    float[] res = new float[array.length / size];
    for (int i = 0; i < array.length; i = i + size) {
      int p = 0;
      for (int j = 0; j < size; j++) {
        p = (p << 8) | (array[i + j] & 0xff);
      }
      res[i / size] = Float.intBitsToFloat(p);
    }
    return res;
  }

  public static float[] assembleByteBeToFixed(byte[] b, int offset, int len,
      int size, float d) {
    float[] res = new float[len / size];
    for (int i = offset, k = 0; i < offset + len; i = i + size, k++) {
      int p = b[i] < 0 ? -1 : 0;
      for (int j = 0; j < size; j++) {
        p = (p << 8) | (b[i + j] & 0xff);
      }
      res[k] = p / d;

      System.out.print(res[k] + " ");
    }
    System.out.println();
    return res;
  }

  public static String toString(byte[] b, int offset, int len) {
    byte[] a = b;
    if (offset != 0 || len != 0) {
      int l = 0;
      for (int i = offset; i < offset + len; i++, l++) {
        if (b[i] == 0) {
          break;
        }
      }
      a = new byte[l];
      System.arraycopy(b, offset, a, 0, l);
    }
    return new String(a, StandardCharsets.UTF_8);
  }

  public static void main(String[] args) {
    String cmd = "00009e03dd0700002b4e0500ab3305000e01";
    byte[] ba = new byte[cmd.length() / 2];
    for (int i = 0, j = 0; i < cmd.length(); i = i + 2, j++) {
      ba[j] = hexToByte(cmd.charAt(i), cmd.charAt(i + 1));
    }

    System.out.println(byteToHex(ba));

//    assembleByteLe(ba, 7, new int[] { 2, 2, 2, 2, 4, 4, 2 });
  }
}
