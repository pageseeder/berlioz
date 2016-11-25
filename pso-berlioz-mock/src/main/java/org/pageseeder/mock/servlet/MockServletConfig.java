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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class MockServletConfig implements ServletConfig {

  private String servletName;

  private Map<String, String> parameters = new HashMap<>();

  public MockServletConfig() {
    this.servletName = "org.example.UnnamedServlet";
  }

  @Override
  public String getInitParameter(String name) {
    return this.parameters.get(name);
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return Collections.enumeration(this.parameters.values());
  }

  @Override
  public ServletContext getServletContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getServletName() {
    return this.servletName;
  }


  public void setInitParameter(String name, String value) {
    this.parameters.put(name, value);
  }
}
