package org.pageseeder.berlioz.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class to generate 16-byte long nonce values encoded with Base64
 * for the Content-Security-Policy header.
 *
 * <p>This class wraps a SecureRandom instance. It is thread-safe, a single
 * instance can be used to generate many nonce values.
 *
 * @version 0.12.5
 * @since 0.12.5
 */
public final class Nonce {

  private final SecureRandom r = new SecureRandom();

  /**
   * Default length in bytes;
   */
  private static final int DEFAULT_LENGTH = 16;

  public Nonce() {
  }

  /**
   * Generate a new nonce.
   *
   * @return a new 16-byte long nonce encoded with Base64.
   */
  public String generate() {
    byte[] bytes = new byte[DEFAULT_LENGTH];
    r.nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

}
