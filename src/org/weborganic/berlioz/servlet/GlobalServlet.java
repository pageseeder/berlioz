/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

/**
 * Default Berlioz servlet.
 * 
 * @deprecated Use {@link BerliozServlet} instead.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 1 June 2010
 */
@Deprecated public final class GlobalServlet extends BerliozServlet {

  /**
   * Name of the global property to use to enable HTTP compression using the 
   * <code>Content-Encoding</code> of compressible content.
   * 
   * <p>The property value is <code>true</code> by default.
   */
  public static final String ENABLE_HTTP_COMPRESSION = "berlioz.http.compression";

  /**
   * Name of the global property to use to specify the max age of the <code>Cache-Control</code>
   * HTTP header of cacheable content.
   * 
   * <p>The property value is <code>60</code> (seconds) by default.
   */
  public static final String HTTP_MAX_AGE = "berlioz.http.max-age";

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 2006100926180001L;

}
