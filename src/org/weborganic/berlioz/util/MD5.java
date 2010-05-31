/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class providing a simple method to generate MD5 hash values for text content.
 * 
 * <p>An MD5 hash is typically expressed as a 32-digit hexadecimal number.
 * 
 * @author Christophe Lauret
 * @version 31 May 2010
 */
public final class MD5 {

  /**
   * The length of the hashed value.
   */
  private static final int HASH_VALUE_LENGTH = 32;

  /**
   * Character set for the hashing function.
   */
  private static final Charset UTF8 = Charset.forName("utf-8");

  /**
   * Prevents creation of instance.
   */
  private MD5() {
  }

  /**
   * Returns a hash value for the specified text.
   * 
   * @param text The text value to hash.
   * 
   * @return The {@link HashMap} value for the specified test or <code>null</code> if an error occurred.
   */
  public static String hash(String text) {
    byte[] md5hash = new byte[HASH_VALUE_LENGTH];
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(text.getBytes(UTF8), 0, text.length());
      md5hash = md.digest();
    } catch (NoSuchAlgorithmException ex) {
      ex.printStackTrace();
    }
    return toHex(md5hash);
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Converts the byte data into a sequence of hexadecimal characters.
   * 
   * @param data The byte array to convert.
   * @return the corresponding sequence of hexadecimal characters.
   */
  private static String toHex(byte[] data) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < data.length; i++) {
      int halfbyte = (data[i] >>> 4) & 0x0F;
      int two_halfs = 0;
      do {
        if ((0 <= halfbyte) && (halfbyte <= 9))
          buf.append((char)('0' + halfbyte));
        else
          buf.append((char)('a' + (halfbyte - 10)));
        halfbyte = data[i] & 0x0F;
      } while (two_halfs++ < 1);
    }
    return buf.toString();
  }

}
