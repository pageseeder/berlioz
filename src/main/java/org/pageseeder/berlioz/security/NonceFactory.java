package org.pageseeder.berlioz.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class to generate 16-byte long nonce values encoded with Base64
 * for the Content-Security-Policy header.
 *
 * <p>This class wraps a {@link SecureRandom} instance. It is thread-safe, a single
 * instance can be used to generate many nonce values.
 *
 * <p>The base 64 value does not use padding.
 *
 * @version 0.12.6
 * @since 0.12.6
 */
public final class NonceFactory {

  private final SecureRandom r = new SecureRandom();

  private final Base64.Encoder encoder = Base64.getEncoder().withoutPadding();

  /**
   * Default length in bytes;
   */
  private static final int DEFAULT_LENGTH = 16;

  /**
   * Length in bytes;
   */
  private final int length;

  /**
   * Creates a new instance to generate 16-byte long nonce values.
   */
  public NonceFactory() {
    this.length = DEFAULT_LENGTH;
  }

  /**
   * Creates a new instance with a specified
   *
   * @param length Length in bytes of the nonce.
   *
   * @throws IllegalArgumentException If length is less than 4 or greater than 256.
   */
  public NonceFactory(int length) {
    if (length < 4 || length > 256) throw new IllegalArgumentException();
    this.length = length;
  }

  /**
   * Generate a new nonce.
   *
   * @return a new nonce encoded with Base64 without padding.
   */
  public String generate() {
    byte[] bytes = new byte[this.length];
    r.nextBytes(bytes);
    return this.encoder.encodeToString(bytes);
  }

}
