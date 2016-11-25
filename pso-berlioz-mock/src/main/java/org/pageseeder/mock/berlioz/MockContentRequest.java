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
package org.pageseeder.mock.berlioz;

import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.Location;
import org.pageseeder.berlioz.servlet.HttpLocation;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.mock.servlet.MockHttpServletRequest;

public class MockContentRequest implements ContentRequest{

  private MockHttpServletRequest _http;

  private String redirectURL;

  private ContentStatus status;

  public MockContentRequest() {
    this(new MockHttpServletRequest());
  }

  public MockContentRequest(MockHttpServletRequest http) {
    this._http = http;
  }

  @Override
  public Object getAttribute(String name) {
    return this._http.getAttribute(name);
  }

  @Override
  public String getBerliozPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Cookie[] getCookies() {
    return this._http.getCookies();
  }

  @Override
  public Date getDateParameter(String name) {
    try {
      return ISO8601.parseAuto(this.getParameter(name));
    } catch (ParseException ex) {
      return null;
    }
  }

  @Override
  public Environment getEnvironment() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getIntParameter(String name, int fallback) {
    String value = getParameter(name);
    if (value == null || "".equals(value)) return fallback;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  @Override
  public long getLongParameter(String name, long fallback) {
    String value = getParameter(name);
    if (value == null || "".equals(value)) return fallback;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  @Override
  public Location getLocation() {
    return HttpLocation.build(this._http);
  }

  @Override
  public String getParameter(String name) {
    return this._http.getParameter(name);
  }

  @Override
  public String getParameter(String name, String fallback) {
    String value = getParameter(name);
    return (value == null || "".equals(value))? fallback : value;
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return this._http.getParameterNames();
  }

  @Override
  public String[] getParameterValues(String name) {
    return this._http.getParameterValues(name);
  }

  @Override
  public HttpSession getSession() {
    return this._http.getSession();
  }

  @Override
  public void setAttribute(String name, Object value) {
    this._http.setAttribute(name, value);
  }

  @Override
  public void setRedirect(String url, ContentStatus status) {
    this.redirectURL = url;
    this.status = status;
  }

  @Override
  public void setStatus(ContentStatus status) {
    this.status = status;
  }

  public ContentStatus getStatus() {
    return this.status;
  }

  public void setParameter(String name, String value) {
    this._http.setParameter(name, value);
  }

}
