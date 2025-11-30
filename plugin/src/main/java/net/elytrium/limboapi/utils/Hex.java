package net.elytrium.limboapi.utils;

public class Hex {

  public static final byte[] BYTE_TABLE = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };
  public static final char[] CHAR_TABLE = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  public static byte[] encodeBytes(byte[] data) {
    byte[] result = new byte[data.length << 1];
    for (int from = 0, to = 0; from < data.length; ++from) {
      result[to++] = Hex.BYTE_TABLE[(data[from] & 0xF0) >>> 4];
      result[to++] = Hex.BYTE_TABLE[data[from] & 0x0F];
    }

    return result;
  }

  public static char[] encodeChars(byte[] data) {
    char[] result = new char[data.length << 1];
    for (int from = 0, to = 0; from < data.length; ++from) {
      result[to++] = Hex.CHAR_TABLE[(data[from] & 0xF0) >>> 4];
      result[to++] = Hex.CHAR_TABLE[data[from] & 0x0F];
    }

    return result;
  }

  public static String encodeString(byte[] data) {
    return new String(Hex.encodeChars(data));
  }
}
