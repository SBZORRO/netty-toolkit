package com.jerei;

import java.io.UnsupportedEncodingException;

public class HexByteUtil {
  public static final char[] hexChar = new char[] { '0', '1', '2', '3', '4',
      '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

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
    }
    return sb.toString();
  }

  public static String toWchar(byte[] ba) {
    try {
      return new String(ba, "UTF-16");
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      return "未知艺术家";
    }
  }

  public static String toDateTime(byte[] ba) {
    return "";
  }

  public static float toReal(byte[] ba) {
    return Float.intBitsToFloat(HexByteUtil.toU2int(ba));
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

  public static int[] assembleByteLe(byte[] array, int len, int[] size) {
    int[] res = new int[len];
    for (int i = 0, l = 0, k = 0; i < array.length; i
        = i + size[k], k++, l++) {
      int p = 0;
      for (int j = size[k] - 1; j >= 0; j--) {
        p |= (array[i + j] & 0xff) << 8 * j;
      }
      res[l] = p;
    }
    return res;
  }

  public static void main(String[] args) {
    String cmd = "00009e03dd0700002b4e0500ab3305000e01";
    byte[] ba = new byte[cmd.length() / 2];
    for (int i = 0, j = 0; i < cmd.length(); i = i + 2, j++) {
      ba[j] = hexToByte(cmd.charAt(i), cmd.charAt(i + 1));
    }

    assembleByteLe(ba, 7, new int[] { 2, 2, 2, 2, 4, 4, 2 });
  }
}
