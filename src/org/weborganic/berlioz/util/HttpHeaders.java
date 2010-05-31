/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

/**
 * Collection of HTTP header constants.
 * 
 * @author Christophe Lauret
 * @version 5 January 2010
 */
public final class HttpHeaders {

  /**
   * Utility class. 
   */
  private HttpHeaders() {
  }

  /**
   * 'Accept' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.1">HTTP/1.1 - 14.1 Accept</a>
   */
  public static final String ACCEPT = "Accept";

  /**
   * 'Accept-Charset' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.2">HTTP/1.1 - 14.2 Accept-Charset</a>
   */
  public static final String ACCEPT_CHARSET = "Accept-Charset";

  /**
   * 'Accept-Encoding' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.3">HTTP/1.1 - 14.3 Accept-Encoding</a>
   */
  public static final String ACCEPT_ENCODING = "Accept-Encoding";

  /**
   * 'Accept-Language' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.4">HTTP/1.1 - 14.4 Accept-Language</a>
   */
  public static final String ACCEPT_LANGUAGE = "Accept-Language";

  /**
   * 'Accept-Ranges' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.5">HTTP/1.1 - 14.5 Accept-Ranges</a>
   */
  public static final String ACCEPT_RANGES = "Accept-Ranges";

  /**
   * 'Age' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.6">HTTP/1.1 - 14.6 Age</a>
   */
  public static final String AGE = "Age";

  /**
   * 'Allow' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.7">HTTP/1.1 - 14.7 Allow</a>
   */
  public static final String ALLOW = "Allow";

  /**
   * 'Authorization' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.8">HTTP/1.1 - 14.8 Authorization</a>
   */
  public static final String AUTHORIZATION = "Authorization";

  /**
   * 'Cache-Control' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.9">HTTP/1.1 - 14.9 Cache-Control</a>
   */
  public static final String CACHE_CONTROL = "Cache-Control";

  /**
   * 'Connection' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.10">HTTP/1.1 - 14.10 Connection</a>
   */
  public static final String CONNECTION = "Connection";

  /** 
   * 'Content-Encoding' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.11">HTTP/1.1 - 14.11 Content-Encoding</a>
   */
  public static final String CONTENT_ENCODING = "Content-Encoding";

  /** 
   * 'Content-Language' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.12">HTTP/1.1 - 14.12 Content-Language</a>
   */
  public static final String CONTENT_LANGUAGE = "Content-Language";

  /**
   * 'Content-Length' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.13">HTTP/1.1 - 14.13 Content-Length</a>
   */
  public static final String CONTENT_LENGTH = "Content-Length";

  /**
   * 'Content-Location' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.14">HTTP/1.1 - 14.14 Content-Location</a>
   */
  public static final String CONTENT_LOCATION = "Content-Location";

  /**
   * 'Content-MD5' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.15">HTTP/1.1 - 14.15 Content-MD5</a>
   */
  public static final String CONTENT_MD5 = "Content-MD5";

  /**
   * 'Content-Range' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.16">HTTP/1.1 - 14.16 Content-Range</a>
   */
  public static final String CONTENT_RANGE = "Content-Range";

  /**
   * 'Content-Type' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.17">HTTP/1.1 - 14.17 Content-Type</a>
   */
  public static final String CONTENT_TYPE = "Content-Type";

  /**
   * 'Date' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.18">HTTP/1.1 - 14.18 Date</a>
   */
  public static final String DATE = "Date";

  /** 
   * 'Etag' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.19">HTTP/1.1 - 14.19 ETag</a>
   */
  public static final String ETAG = "ETag";

  /** 
   * 'Expect' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.20">HTTP/1.1 - 14.20 Expect</a>
   */
  public static final String EXPECT = "Expect";

  /** 
   * 'Expires' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.21">HTTP/1.1 - 14.21 Expires</a>
   */
  public static final String EXPIRES = "Expires";

  /** 
   * 'From' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.22">HTTP/1.1 - 14.22 From</a>
   */
  public static final String FROM = "From";

  /**
   * 'Host' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.23">HTTP/1.1 - 14.23 Host</a>
   */
  public static final String HOST = "Host";

  /** 
   * 'If-Match' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.24">HTTP/1.1 - 14.24 If-Match</a>
   */
  public static final String IF_MATCH = "If-Match";

  /** 
   * 'If-Modified-Since' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.25">HTTP/1.1 - 14.25 If-Modified-Since</a>
   */
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

  /** 
   * 'If-None-Match' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.26">HTTP/1.1 - 14.26 If-None-Match</a>
   */
  public static final String IF_NONE_MATCH = "If-None-Match";

  /** 
   * 'If-Range' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.27">HTTP/1.1 - 14.27 If-Range</a>
   */
  public static final String IF_RANGE = "If-Range";

  /** 
   * 'If-Unmodified-Since' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.28">HTTP/1.1 - 14.28 If-Unmodified-Since</a>
   */
  public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

  /** 
   * 'Last-Modified' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.29">HTTP/1.1 - 14.29 Last-Modified</a>
   */
  public static final String LAST_MODIFIED = "Last-Modified";

  /** 
   * 'Location' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.30">HTTP/1.1 - 14.30 Location</a>
   */
  public static final String LOCATION = "Location";

  /** 
   * 'Max-Forwards' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.31">HTTP/1.1 - 14.31 Max-Forwards</a>
   */
  public static final String MAX_FORWARDS = "Max-Forwards";

  /** 
   * 'Pragma' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.32">HTTP/1.1 - 14.32 Pragma</a>
   */
  public static final String PRAGMA = "Pragma";

  /** 
   * 'Proxy-Authenticate' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.33">HTTP/1.1 - 14.33 Proxy-Authenticate</a>
   */
  public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

  /** 
   * 'Proxy-Authorization' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.34">HTTP/1.1 - 14.34 Proxy-Authorization</a>
   */
  public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

  /** 
   * 'Range' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.35">HTTP/1.1 - 14.35 Range</a>
   */
  public static final String RANGE = "Range";

  /** 
   * 'Referer' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.36">HTTP/1.1 - 14.36 Referer</a>
   */
  public static final String REFERER = "Referer";

  /** 
   * 'Retry-After' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.37">HTTP/1.1 - 14.37 Retry-After</a>
   */
  public static final String RETRY_AFTER = "Retry-After";

  /** 
   * 'Server' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.38">HTTP/1.1 - 14.38 Server</a>
   */
  public static final String SERVER = "Server";

  /** 
   * 'TE' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.39">HTTP/1.1 - 14.39 TE</a>
   */
  public static final String TE = "TE";

  /** 
   * 'Trailer' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.40">HTTP/1.1 - 14.40 Trailer</a>
   */
  public static final String TRAILER = "Trailer";

  /** 
   * 'Transfer-Encoding' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.41">HTTP/1.1 - 14.41 Transfer-Encoding</a>
   */
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";

  /** 
   * 'Upgrade' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.42">HTTP/1.1 - 14.42 Upgrade</a>
   */
  public static final String UPGRADE = "Upgrade";

  /** 
   * 'User-Agent' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.43">HTTP/1.1 - 14.43 User-Agent</a>
   */
  public static final String USER_AGENT = "User-Agent";

  /** 
   * 'Vary' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.44">HTTP/1.1 - 14.44 Vary</a>
   */
  public static final String VARY = "Vary";

  /** 
   * 'Via' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.45">HTTP/1.1 - 14.45 Via</a>
   */
  public static final String VIA = "Via";

  /** 
   * 'Warning' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.46">HTTP/1.1 - 14.46 Warning</a>
   */
  public static final String WARNING = "Warning";

  /** 
   * 'WWW-Authenticate' header.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.47">HTTP/1.1 - 14.47 WWW-Authenticate</a>
   */
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

}
