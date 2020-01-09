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
package org.pageseeder.berlioz.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.LifecycleListener;

/**
 * Initialise a Berlioz-based application.
 *
 * <p>
 * This servlet does not return any data, it is simply used to initialise some data, and therefore
 * only implements the {@link #init(ServletConfig)} method.
 *
 * <p>
 * The {@link InitServlet#init()} method performs a sanity check to inform the user about which
 * version of Berlioz is running and which configuration files are used.
 *
 * <p>
 * Typically this servlet should be configured in the Web Configuration (web.xml) as:
 *
 * <pre>
 * {@code <!-- Initialisation servlet -->
 * <servlet>
 *   <servlet-name>Initialiser</servlet-name>
 *   <servlet-class>org.pageseeder.berlioz.servlet.InitServlet</servlet-class>
 *   <load-on-startup>1</load-on-startup>
 * </servlet> }
 * </pre>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.0
 * @since Berlioz 0.7
 */
public final class InitServlet extends HttpServlet implements Servlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20161115L;

  /**
   * The lifecycle listener notified when Berlioz starts and stops.
   */
  private static final List<LifecycleListener> listeners = new ArrayList<>();

  /**
   * The application initializer.
   */
  private @Nullable AppInitializer initializer = null;

  /**
   * Initialises Berlioz.
   *
   * <p>This method displays the configuration setting and does the following in order:
   * <ol>
   *  <li>Determines the Berlioz mode;</li>
   *  <li>Checks that the <code>services.xml</code> is available;</li>
   *  <li>Configures <code>Log4j</code> if possible;</li>
   *  <li>Loads and checks the global config;</li>
   *  <li>Invokes the <code>start</code> method of the <code>LifecycleListener</code>.</li>
   * </ol>
   *
   * {@inheritDoc}
   *
   * @param config The servlet configuration.
   *
   * @throws ServletException Should an exception occur.
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    if (config != null) {
      AppInitializer init = AppInitializer.newInstance(config, listeners);
      init.init();
      this.initializer = init;
    }
  }

  /**
   * Reset the initialisation Berlioz.
   *
   * <p>This method unload the configuration setting and does the following in order:
   * <ol>
   *  <li>Invokes the <code>stop</code> method of the <code>ConfigListener</code>.</li>
   * </ol>
   *
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    super.destroy();
    AppInitializer init = this.initializer;
    if (init != null) {
      init.destroy();
      this.initializer = null;
    }
  }

  /**
   * @return "Berlioz Initialisation Servlet"
   */
  @Override
  public String getServletInfo() {
    return "Berlioz Initialisation Servlet";
  }

  /**
   * Add a listener to invoke when the Berlioz starts or stops.
   *
   * @param listener The listener to register.
   */
  @Beta
  public static void registerListener(LifecycleListener listener) {
    listeners.add(listener);
  }

}
