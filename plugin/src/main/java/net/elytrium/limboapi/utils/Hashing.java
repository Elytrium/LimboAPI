package net.elytrium.limboapi.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashing {

  private static ThreadLocal<MessageDigest> SHA1;

  public static byte[] sha1(String input) {
    return Hashing.sha1(input.getBytes(StandardCharsets.UTF_8));
  }

  public static byte[] sha1(byte[] input) {
    if (Hashing.SHA1 == null) {
      Hashing.SHA1 = Hashing.messageDigest("SHA-1");
    }

    return Hashing.SHA1.get().digest(input);
  }

  @SuppressWarnings("SameParameterValue")
  private static ThreadLocal<MessageDigest> messageDigest(String algorithm) {
    return ThreadLocal.withInitial(() -> {
      try {
        return MessageDigest.getInstance(algorithm);
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
