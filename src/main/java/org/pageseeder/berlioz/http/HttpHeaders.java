/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.http;

/**
 * Collection of HTTP header constants.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.2 - 27 June 2011
 * @since Berlioz 0.6
 */
public final class HttpHeaders { // NO_UCD

  /**
   * Utility class.
   */
  private HttpHeaders() {
  }

  /**
   * 'Accept' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Accept         = "Accept" ":" #( media-range [ accept-params ] )
   *
   *   media-range    = ( "*&#47;*" | ( type "/" "*" ) | ( type "/" subtype ) ) *( ";" parameter )
   *   accept-params  = ";" "q" "=" qvalue *( accept-extension )
   *   accept-extension = ";" token [ "=" ( token | quoted-string ) ]
   * </pre>
   * <p>Examples:</p>
   * <pre>
   *   Accept: audio/*; q=0.2, audio/basic
   *   Accept: text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c
   *   Accept: text/*, text/html, text/html;level=1, *&#47;*
   *   Accept: text/*;q=0.3, text/html;q=0.7, text/html;level=1,text/html;level=2;q=0.4, *&#47;*;q=0.5
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.1">HTTP/1.1 - 14.1 Accept</a>
   */
  public static final String ACCEPT = "Accept";

  /**
   * 'Accept-Charset' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Accept-Charset = "Accept-Charset" ":" 1#( ( charset | "*" )[ ";" "q" "=" qvalue ] )
   * </pre>
   * <p>Examples:</p>
   * <pre>
   *   Accept-Charset: iso-8859-5, unicode-1-1;q=0.8
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.2">HTTP/1.1 - 14.2 Accept-Charset</a>
   */
  public static final String ACCEPT_CHARSET = "Accept-Charset";

  /**
   * 'Accept-Encoding' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Accept-Encoding  = "Accept-Encoding" ":" 1#( codings [ ";" "q" "=" qvalue ] )
   *   codings          = ( content-coding | "*" )
   * </pre>
   * <p>Examples of its use are:</p>
   * <pre>
   *   Accept-Encoding: compress, gzip
   *   Accept-Encoding:
   *   Accept-Encoding: *
   *   Accept-Encoding: compress;q=0.5, gzip;q=1.0
   *   Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.3">HTTP/1.1 - 14.3 Accept-Encoding</a>
   */
  public static final String ACCEPT_ENCODING = "Accept-Encoding";

  /**
   * 'Accept-Language' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Accept-Language = "Accept-Language" ":" 1#( language-range [ ";" "q" "=" qvalue ] )
   *   language-range  = ( ( 1*8ALPHA *( "-" 1*8ALPHA ) ) | "*" )
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Accept-Language: da, en-gb;q=0.8, en;q=0.7
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.4">HTTP/1.1 - 14.4 Accept-Language</a>
   */
  public static final String ACCEPT_LANGUAGE = "Accept-Language";

  /**
   * 'Accept-Ranges' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Accept-Ranges     = "Accept-Ranges" ":" acceptable-ranges
   *   acceptable-ranges = 1#range-unit | "none"
   * </pre>
   * <p>Examples:</p>
   * <pre>
   *   Accept-Ranges: bytes
   *   Accept-Ranges: none
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.5">HTTP/1.1 - 14.5 Accept-Ranges</a>
   */
  public static final String ACCEPT_RANGES = "Accept-Ranges";

  /**
   * 'Age' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Age       = "Age" ":" age-value
   *   age-value = delta-seconds
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.6">HTTP/1.1 - 14.6 Age</a>
   */
  public static final String AGE = "Age";

  /**
   * 'Allow' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Allow   = "Allow" ":" #Method
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Allow: GET, HEAD, PUT
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.7">HTTP/1.1 - 14.7 Allow</a>
   */
  public static final String ALLOW = "Allow";

  /**
   * 'Authorization' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Authorization  = "Authorization" ":" credentials
   * </pre>
   * <p>Example:</p>
   * <pre>
   *
   * </pre>
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.8">HTTP/1.1 - 14.8 Authorization</a>
   */
  public static final String AUTHORIZATION = "Authorization";

  /**
   * 'Cache-Control' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Cache-Control   = "Cache-Control" ":" 1#cache-directive
   *   cache-directive = cache-request-directive | cache-response-directive
   *
   *   cache-request-directive = "no-cache"
   *                           | "no-store"
   *                           | "max-age" "=" delta-seconds
   *                           | "max-stale" [ "=" delta-seconds ]
   *                           | "min-fresh" "=" delta-seconds
   *                           | "no-transform"
   *                           | "only-if-cached"
   *                           | cache-extension
   *
   *   cache-response-directive = "public"
   *                            | "private" [ "=" <"> 1#field-name <"> ]
   *                            | "no-cache" [ "=" <"> 1#field-name <"> ]
   *                            | "no-store"
   *                            | "no-transform"
   *                            | "must-revalidate"
   *                            | "proxy-revalidate"
   *                            | "max-age" "=" delta-seconds
   *                            | "s-maxage" "=" delta-seconds
   *                            | cache-extension
   *
   *   cache-extension = token [ "=" ( token | quoted-string ) ]
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.9">HTTP/1.1 - 14.9 Cache-Control</a>
   */
  public static final String CACHE_CONTROL = "Cache-Control";

  /**
   * 'Connection' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Connection = "Connection" ":" 1#(connection-token)
   *   connection-token  = token
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Connection: close
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.10">HTTP/1.1 - 14.10 Connection</a>
   */
  public static final String CONNECTION = "Connection";

  /**
   * 'Content-Encoding' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Content-Encoding  = "Content-Encoding" ":" 1#content-coding
   * </pre>
   * <p>Example:</p>
   * <pre>
   *    Content-Encoding: gzip
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.11">HTTP/1.1 - 14.11 Content-Encoding</a>
   */
  public static final String CONTENT_ENCODING = "Content-Encoding";

  /**
   * 'Content-Language' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Content-Language  = "Content-Language" ":" 1#language-tag
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Content-Language: da
   *   Content-Language: mi, en
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.12">HTTP/1.1 - 14.12 Content-Language</a>
   */
  public static final String CONTENT_LANGUAGE = "Content-Language";

  /**
   * 'Content-Length' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Content-Length    = "Content-Length" ":" 1*DIGIT
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Content-Length: 3495
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.13">HTTP/1.1 - 14.13 Content-Length</a>
   */
  public static final String CONTENT_LENGTH = "Content-Length";

  /**
   * 'Content-Location' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Content-Location = "Content-Location" ":" ( absoluteURI | relativeURI )
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.14">HTTP/1.1 - 14.14 Content-Location</a>
   */
  public static final String CONTENT_LOCATION = "Content-Location";

  /**
   * 'Content-MD5' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Content-MD5   = "Content-MD5" ":" md5-digest
   *    md5-digest   = <base64 of 128 bit MD5 digest as per RFC 1864>
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.15">HTTP/1.1 - 14.15 Content-MD5</a>
   */
  public static final String CONTENT_MD5 = "Content-MD5";

  /**
   * 'Content-Range' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Content-Range = "Content-Range" ":" content-range-spec
   *
   *   content-range-spec      = byte-content-range-spec
   *   byte-content-range-spec = bytes-unit SP byte-range-resp-spec "/" ( instance-length | "*" )
   *   byte-range-resp-spec    = (first-byte-pos "-" last-byte-pos) | "*"
   *   instance-length         = 1*DIGIT
   * </pre>
   * <p>Examples:</p>
   * <pre>
   *   Content-Range: bytes 0-499/1234    ; The first 500 bytes
   *   Content-Range: bytes 500-999/1234  ; The second 500 bytes
   *   Content-Range: bytes 500-1233/1234 ; All except for the first 500 bytes
   *   Content-Range: bytes 500-1233/1234 ; The last 500 bytes
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.16">HTTP/1.1 - 14.16 Content-Range</a>
   */
  public static final String CONTENT_RANGE = "Content-Range";

  /**
   * 'Content-Type' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Content-Type   = "Content-Type" ":" media-type
   *   media-type     = type "/" subtype *( ";" parameter )
   *   type           = token
   *   subtype        = token
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Content-Type: text/html; charset=ISO-8859-4
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.17">HTTP/1.1 - 14.17 Content-Type</a>
   */
  public static final String CONTENT_TYPE = "Content-Type";

  /**
   * 'Date' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Date  = "Date" ":" HTTP-date
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Date: Tue, 15 Nov 1994 08:12:31 GMT
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.18">HTTP/1.1 - 14.18 Date</a>
   */
  public static final String DATE = "Date";

  /**
   * 'Etag' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   ETag = "ETag" ":" entity-tag
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   ETag: "xyzzy"
   *   ETag: W/"xyzzy"
   *   ETag: ""
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.19">HTTP/1.1 - 14.19 ETag</a>
   */
  public static final String ETAG = "ETag";

  /**
   * 'Expect' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Expect       =  "Expect" ":" 1#expectation
   *   expectation  =  "100-continue" | expectation-extension
   *   expectation-extension =  token [ "=" ( token | quoted-string ) *expect-params ]
   *   expect-params =  ";" token [ "=" ( token | quoted-string ) ]
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Expect: 100-continue
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.20">HTTP/1.1 - 14.20 Expect</a>
   */
  public static final String EXPECT = "Expect";

  /**
   * 'Expires' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Expires = "Expires" ":" HTTP-date
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Expires: Thu, 01 Dec 1994 16:00:00 GMT
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.21">HTTP/1.1 - 14.21 Expires</a>
   */
  public static final String EXPIRES = "Expires";

  /**
   * 'From' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   From   = "From" ":" mailbox
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   From: webmaster@w3.org
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.22">HTTP/1.1 - 14.22 From</a>
   */
  public static final String FROM = "From";

  /**
   * 'Host' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Host = "Host" ":" host [ ":" port ]
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Host: www.w3.org
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.23">HTTP/1.1 - 14.23 Host</a>
   */
  public static final String HOST = "Host";

  /**
   * 'If-Match' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   If-Match = "If-Match" ":" ( "*" | 1#entity-tag )
   * </pre>
   * <p>Examples</p>
   * <pre>
   *   If-Match: "xyzzy"
   *   If-Match: "xyzzy", "r2d2xhyxx", "c3piozw4zz"
   *   If-Match: *
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.24">HTTP/1.1 - 14.24 If-Match</a>
   */
  public static final String IF_MATCH = "If-Match";

  /**
   * 'If-Modified-Since' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   If-Modified-Since = "If-Modified-Since" ":" HTTP-date
   * </pre>
   * <p>An example of the field is:</p>
   * <pre>
   *   If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.25">HTTP/1.1 - 14.25 If-Modified-Since</a>
   */
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

  /**
   * 'If-None-Match' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   If-None-Match = "If-None-Match" ":" ( "*" | 1#entity-tag )
   * </pre>
   * <p>Examples:</p>
   * <pre>
   *   If-None-Match: "xyzzy"
   *   If-None-Match: W/"xyzzy"
   *   If-None-Match: "xyzzy", "r2d2xhyxx", "c3piozw4zz"
   *   If-None-Match: W/"xyzzy", W/"r2d2xhyxx", W/"c3piozw4zz"
   *   If-None-Match: *
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.26">HTTP/1.1 - 14.26 If-None-Match</a>
   */
  public static final String IF_NONE_MATCH = "If-None-Match";

  /**
   * 'If-Range' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   If-Range = "If-Range" ":" ( entity-tag | HTTP-date )
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.27">HTTP/1.1 - 14.27 If-Range</a>
   */
  public static final String IF_RANGE = "If-Range";

  /**
   * 'If-Unmodified-Since' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   If-Unmodified-Since = "If-Unmodified-Since" ":" HTTP-date
   * </pre>
   * <p>An example of the field is:</p>
   * <pre>
   *   If-Unmodified-Since: Sat, 29 Oct 1994 19:43:31 GMT
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.28">HTTP/1.1 - 14.28 If-Unmodified-Since</a>
   */
  public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

  /**
   * 'Last-Modified' entity header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Last-Modified  = "Last-Modified" ":" HTTP-date
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.29">HTTP/1.1 - 14.29 Last-Modified</a>
   */
  public static final String LAST_MODIFIED = "Last-Modified";

  /**
   * 'Location' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Location       = "Location" ":" absoluteURI
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Location: http://www.w3.org/pub/WWW/People.html
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.30">HTTP/1.1 - 14.30 Location</a>
   */
  public static final String LOCATION = "Location";

  /**
   * 'Max-Forwards' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Max-Forwards   = "Max-Forwards" ":" 1*DIGIT
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Max-Forwards: 0
   *   Max-Forwards: 5
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.31">HTTP/1.1 - 14.31 Max-Forwards</a>
   */
  public static final String MAX_FORWARDS = "Max-Forwards";

  /**
   * 'Pragma' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Pragma            = "Pragma" ":" 1#pragma-directive
   *   pragma-directive  = "no-cache" | extension-pragma
   *   extension-pragma  = token [ "=" ( token | quoted-string ) ]
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Pragma: no-cache
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.32">HTTP/1.1 - 14.32 Pragma</a>
   */
  public static final String PRAGMA = "Pragma";

  /**
   * 'Proxy-Authenticate' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Proxy-Authenticate  = "Proxy-Authenticate" ":" 1#challenge
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.33">HTTP/1.1 - 14.33 Proxy-Authenticate</a>
   */
  public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

  /**
   * 'Proxy-Authorization' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Proxy-Authorization     = "Proxy-Authorization" ":" credentials
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.34">HTTP/1.1 - 14.34 Proxy-Authorization</a>
   */
  public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

  /**
   * 'Range' header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Range = "Range" ":" ranges-specifier
   *   ranges-specifier = byte-ranges-specifier
   *   byte-ranges-specifier = bytes-unit "=" byte-range-set
   *   byte-range-set  = 1#( byte-range-spec | suffix-byte-range-spec )
   *   byte-range-spec = first-byte-pos "-" [last-byte-pos]
   *   first-byte-pos  = 1*DIGIT
   *   last-byte-pos   = 1*DIGIT
   *   suffix-byte-range-spec = "-" suffix-length
   *   suffix-length = 1*DIGIT
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Range: bytes=0-499       // The first 500 bytes (byte offsets 0-499, inclusive)
   *   Range: bytes=500-999     // The second 500 bytes (byte offsets 500-999, inclusive)
   *   Range: bytes=-500        // The final 500 bytes
   *   Range: bytes=500-        // All bytes after the first 500 bytes
   *   Range: bytes=0-0,-1      // The first and last bytes only
   *
   *   Range: bytes=500-600,601-999      // legal but non canonical specifications of the...
   *   Range: bytes=500-700,601-999      // ...second 500 bytes (byte offsets 500-999, inclusive):
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.35">HTTP/1.1 - 14.35 Range</a>
   */
  public static final String RANGE = "Range";

  /**
   * 'Referer' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Referer        = "Referer" ":" ( absoluteURI | relativeURI )
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Referer: http://www.w3.org/hypertext/DataSources/Overview.html
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.36">HTTP/1.1 - 14.36 Referer</a>
   */
  public static final String REFERER = "Referer";

  /**
   * 'Retry-After' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Retry-After  = "Retry-After" ":" ( HTTP-date | delta-seconds )
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Retry-After: Fri, 31 Dec 1999 23:59:59 GMT
   *   Retry-After: 120                              // 2 minutes
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.37">HTTP/1.1 - 14.37 Retry-After</a>
   */
  public static final String RETRY_AFTER = "Retry-After";

  /**
   * 'Server' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Server         = "Server" ":" 1*( product | comment )
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Server: CERN/3.0 libwww/2.17
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.38">HTTP/1.1 - 14.38 Server</a>
   */
  public static final String SERVER = "Server";

  /**
   * 'Server-Timing' response header.
   *
   * @see <a href="https://www.w3.org/TR/server-timing/">W3 Server-Timing</a>
   */
  public static final String SERVER_TIMING = "Server-Timing";

  /**
   * 'TE' request header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   TE        = "TE" ":" #( t-codings )
   *   t-codings = "trailers" | ( transfer-extension [ accept-params ] )
   * </pre>
   * <p>Example:</p>
   * <pre>
   *    TE: deflate
   *    TE:
   *    TE: trailers, deflate;q=0.5
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.39">HTTP/1.1 - 14.39 TE</a>
   */
  public static final String TE = "TE";

  /**
   * 'Trailer' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Trailer  = "Trailer" ":" 1#field-name
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.40">HTTP/1.1 - 14.40 Trailer</a>
   */
  public static final String TRAILER = "Trailer";

  /**
   * 'Transfer-Encoding' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Transfer-Encoding       = "Transfer-Encoding" ":" 1#transfer-coding
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Transfer-Encoding: chunked
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.41">HTTP/1.1 - 14.41 Transfer-Encoding</a>
   */
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";

  /**
   * 'Upgrade' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Upgrade        = "Upgrade" ":" 1#product
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Upgrade: HTTP/2.0, SHTTP/1.3, IRC/6.9, RTA/x11
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.42">HTTP/1.1 - 14.42 Upgrade</a>
   */
  public static final String UPGRADE = "Upgrade";

  /**
   * 'User-Agent' header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   User-Agent     = "User-Agent" ":" 1*( product | comment )
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   User-Agent: CERN-LineMode/2.15 libwww/2.17b3
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.43">HTTP/1.1 - 14.43 User-Agent</a>
   */
  public static final String USER_AGENT = "User-Agent";

  /**
   * 'Vary' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Vary  = "Vary" ":" ( "*" | 1#field-name )
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Vary: Accept-Encoding
   *   Vary: *
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.44">HTTP/1.1 - 14.44 Vary</a>
   */
  public static final String VARY = "Vary";

  /**
   * 'Via' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Via =  "Via" ":" 1#( received-protocol received-by [ comment ] )
   *   received-protocol = [ protocol-name "/" ] protocol-version
   *   protocol-name     = token
   *   protocol-version  = token
   *   received-by       = ( host [ ":" port ] ) | pseudonym
   *   pseudonym         = token
   * </pre>
   * <p>Example:</p>
   * <pre>
   *   Via: 1.0 fred, 1.1 nowhere.com (Apache/1.1)
   *   Via: 1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy
   *   Via: 1.0 ricky, 1.1 mertz, 1.0 lucy
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.45">HTTP/1.1 - 14.45 Via</a>
   */
  public static final String VIA = "Via";

  /**
   * 'Warning' general header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   Warning    = "Warning" ":" 1#warning-value
   *   warning-value = warn-code SP warn-agent SP warn-text [SP warn-date]
   *   warn-code  = 3DIGIT
   *   warn-agent = ( host [ ":" port ] ) | pseudonym
   *   warn-text  = quoted-string
   *   warn-date  = <"> HTTP-date <">
   * </pre>
   * <p><i>Note: the <code>warn-agent</code> is the name or pseudonym of the server adding the
   * <code>Warning</code> header, for use in debugging</i></p>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.46">HTTP/1.1 - 14.46 Warning</a>
   */
  public static final String WARNING = "Warning";

  /**
   * 'WWW-Authenticate' response header.
   *
   * <p>Augmented BNF:</p>
   * <pre>
   *   WWW-Authenticate  = "WWW-Authenticate" ":" 1#challenge
   * </pre>
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.47">HTTP/1.1 - 14.47 WWW-Authenticate</a>
   */
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

}
