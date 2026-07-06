/*
 * Copyright 2016 Allette Systems (Australia)
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
package org.pageseeder.mock.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * An HTTP servlet response implementation to
 *
 */
public class MockHttpServletResponse implements HttpServletResponse {

  private String characterEncoding;

  private int contentLength;

  private int bufferSize;

  private String contentType;

  private int status = SC_OK;

  private String errorMessage;

  private StringWriter out = new StringWriter();

  private PrintWriter print = new PrintWriter(this.out);

  private ByteArrayOutputStream bytes = new ByteArrayOutputStream();

  private MockServletOutputstream stream = new MockServletOutputstream(this.bytes);

  private Map<String, List<String>> headers = new HashMap<>();

  private Locale locale = null;

  private List<Cookie> cookies = new ArrayList<>();

  private boolean isCommitted = false;

  private boolean isError = false;


  public MockHttpServletResponse() {
    // No initialization required; defaults are set at field declarations
  }

  @Override
  public String getCharacterEncoding() {
    return this.characterEncoding;
  }

  @Override
  public String getContentType() {
    return this.contentType;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return this.stream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return this.print;
  }

  @Override
  public void setCharacterEncoding(String charset) {
    this.characterEncoding = charset;
  }

  @Override
  public void setContentLength(int len) {
    this.contentLength = len;
  }

  @Override
  public void setContentLengthLong(long len) {
    this.contentLength = (int)len;
  }

  @Override
  public void setContentType(String type) {
    this.contentType = type;
  }

  @Override
  public void setBufferSize(int size) {
    this.bufferSize = size;
  }

  @Override
  public int getBufferSize() {
    return this.bufferSize;
  }

  @Override
  public void flushBuffer() throws IOException {
    this.print.flush();
    this.stream.flush();
    this.isCommitted = true;
  }

  @Override
  public void resetBuffer() {
    checkCommitted();
    this.out = new StringWriter();
    this.print = new PrintWriter(this.out);
    this.bytes.reset();
  }

  @Override
  public boolean isCommitted() {
    return this.isCommitted;
  }

  @Override
  public void reset() {
    checkCommitted();
    resetBuffer();
    this.characterEncoding = null;
    this.contentLength = 0;
    this.contentType = null;
    this.status = SC_OK;
    this.errorMessage = null;
    this.headers.clear();
    this.cookies.clear();
    this.locale = null;
    this.isError = false;
  }

  @Override
  public void setLocale(Locale loc) {
    this.locale = loc;
  }

  @Override
  public Locale getLocale() {
    return this.locale;
  }

  @Override
  public void addCookie(Cookie cookie) {
    this.cookies.add(cookie);
  }

  @Override
  public boolean containsHeader(String name) {
    return this.headers.containsKey(name);
  }

  @Override
  public String encodeURL(String url) {
    return url;
  }

  @Override
  public String encodeRedirectURL(String url) {
    return url;
  }

  @Override
  public String encodeUrl(String url) {
    return encodeURL(url);
  }

  @Override
  public String encodeRedirectUrl(String url) {
    return encodeRedirectURL(url);
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    checkCommitted();
    this.status = sc;
    this.errorMessage = msg;
    this.isError = true;
    this.isCommitted = true;
  }

  @Override
  public void sendError(int sc) throws IOException {
    checkCommitted();
    this.status = sc;
    this.isError = true;
    this.isCommitted = true;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    checkCommitted();
    this.status = SC_FOUND;
    setHeader("Location", encodeRedirectURL(location));
    this.isCommitted = true;
  }

  @Override
  public void setDateHeader(String name, long date) {
    setHeader(name, formatHTTPDate(date));
  }

  @Override
  public void addDateHeader(String name, long date) {
    addHeader(name, formatHTTPDate(date));
  }

  @Override
  public void setHeader(String name, String value) {
    List<String> values = new ArrayList<>(1);
    values.add(value);
    this.headers.put(name, values);
  }

  @Override
  public void addHeader(String name, String value) {
    List<String> values = this.headers.get(name);
    if (values == null) {
      values = new ArrayList<>();
    }
    values.add(value);
    this.headers.put(name, values);
  }

  @Override
  public void setIntHeader(String name, int value) {
    List<String> values = new ArrayList<>(1);
    values.add(Integer.toString(value));
    this.headers.put(name, values);
  }

  @Override
  public void addIntHeader(String name, int value) {
    List<String> values = this.headers.get(name);
    if (values == null) {
      values = new ArrayList<>();
    }
    values.add(Integer.toString(value));
    this.headers.put(name, values);
  }

  @Override
  public void setStatus(int sc) {
    this.status = sc;
  }

  @Override
  public void setStatus(int sc, String sm) {
    this.status = sc;
    this.errorMessage = sm;
  }

  @Override
  public int getStatus() {
    return this.status;
  }

  @Override
  public String getHeader(String name) {
    List<String> values = this.headers.get(name);
    return values != null && !values.isEmpty() ? values.get(0) : null;
  }

  @Override
  public Collection<String> getHeaders(String name) {
    List<String> values = this.headers.get(name);
    return values != null? values : Collections.emptyList();
  }

  @Override
  public Collection<String> getHeaderNames() {
    return this.headers.keySet();
  }


  // Method add

  public String getStatusMessage() {
    return this.errorMessage;
  }

  public String getOutputAsString() {
    this.print.flush();
    if (this.bytes.size() > 0) {
      return new String(this.bytes.toByteArray(), getCharset());
    }
    return this.out.toString();
  }

  public boolean errorSent() {
    return this.isError;
  }

  public int getContentLength() {
    return this.contentLength;
  }

  public List<Cookie> getCookies() {
    return Collections.unmodifiableList(this.cookies);
  }

  // private helpers
  // --------------------------------------------------------------------------

  private String formatHTTPDate(long time) {
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(time).atZone(ZoneOffset.UTC));
  }

  private void checkCommitted() {
    if (this.isCommitted) throw new IllegalStateException("Response already committed");
  }

  private Charset getCharset() {
    if (this.characterEncoding == null) return StandardCharsets.UTF_8;
    return Charset.forName(this.characterEncoding);
  }

}
