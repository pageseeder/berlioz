/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Enumeration;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.content.Environment;

/**
 * Global servlet.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 9 February 2010
 */
public final class GlobalServlet extends HttpServlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 2006100926180001L;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalServlet.class);

// class attributes -------------------------------------------------------------------------------

  /**
   * The servlet configuration.
   */
  private ServletConfig servletConfig;

  /**
   * The default stylesheet used for the transformation.
   */
  private File styleSheet;

  /**
   * Set the content type.
   */
  private String contentType;

  /**
   * Set to true to use the caching mechanism.
   */
  private boolean enableCache;

  /**
   * The environment. 
   */
  private transient Environment env;

  /**
   * Cache for the XSL stylesheet.
   */
  private transient Templates cache;

  /**
   * The request dispatcher to forward to the error handler. 
   */
  private transient RequestDispatcher errorHandler;

  /**
   * The transformer factory to generate the templates
   */
  private static TransformerFactory factory = TransformerFactory.newInstance();

// servlet methods --------------------------------------------------------------------------------

  /**
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   * 
   * @param config The servlet configuration.
   * 
   * @throws ServletException Should an exception occur.
   */
  public void init(ServletConfig config) throws ServletException {
    this.servletConfig = config;
    // get the WEB-INF directory
    ServletContext context = config.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    boolean defCache = !"true".equals(System.getProperty("xsltfilter.caching.disable"));
    this.enableCache = this.getInitParameter("enable-caching", defCache);
    this.contentType = this.getInitParameter("content-type", "text/html;charset=utf-8");
    String stylePath = this.getInitParameter("stylesheet", "/xslt/html/global.xsl");
    this.styleSheet = new File(webinfPath, stylePath);
    // used to dispatch 
    this.errorHandler = context.getNamedDispatcher("ErrorHandlerServlet");
    if (this.errorHandler == null)
      throw new ServletException("The error handler must be configured and named ErrorHandlerServlet.");
    this.env = new HttpEnvironment(contextPath, webinfPath);
  }

  /**
   * Handles a GET request.
   * 
   * {@inheritDoc}
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res);
  }

  /**
   * Handles a POST request.
   * 
   * {@inheritDoc}
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res);
  }

  /**
   * Handles requests.
   * 
   * {@inheritDoc}
   */
  protected void process(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    // setup and ensure that we use UTF-8
    req.setCharacterEncoding("utf-8");
    res.setContentType(this.contentType);

    // prevents caching
    res.setDateHeader("Expires", 0);

    // clear the cache is requested
    boolean clearCache = "true".equals(req.getParameter("clear-xsl-cache"));
    if (clearCache) {
      this.cache = null;
      LOGGER.info("Clearing XSL cache");
    }

    // Generate the XML content
    long t0 = System.currentTimeMillis();
    String content = new XMLResponse().generate(req, res, this.env);
    long t1 = System.currentTimeMillis();
    LOGGER.debug("Content generated in "+(t1 - t0)+" ms");

    // Redirect if required
    String url = req.getParameter("redirect-url");
    if (url == null)
      url = (String)req.getAttribute("redirect-url");
    if (url != null && !"".equals(url)) {
      res.sendRedirect(url);

    // produce the output
    } else {

      // setup the output
      PrintWriter out = res.getWriter();
      StreamResult result = new StreamResult(out);
      ByteArrayOutputStream errors = new ByteArrayOutputStream();

      try {
        // Creates a transformer from the templates
        setupListener(factory.getErrorListener(), errors);
        Templates templates = this.getTemplates();
        long t2 = System.currentTimeMillis();
        LOGGER.debug("Templates loaded in "+(t2 - t1)+"ms");
        Transformer transformer = templates.newTransformer();
        setupListener(transformer.getErrorListener(), errors);
        setXSLTParameters(transformer, req);
        setOutputProperties(transformer, res);

        // get response from filter chain and do XSL transform only if there is some data
        StreamSource source = new StreamSource();
        source.setReader(new StringReader(content));

        // process, write directly to the result
        transformer.transform(source, result);
        long t3 = System.currentTimeMillis();
        LOGGER.debug("Transformation in "+(t3 - t2)+"ms");
        LOGGER.debug("Total request in "+(t3 - t0)+"ms");

      // very likely to be an error in the XML or a dynamic error
      } catch (Exception ex) {
        if (!res.isCommitted()) {
          res.resetBuffer();
          req.setAttribute("exception", ex);
          if (errors.size() > 0)
            req.setAttribute("extra", errors.toString("utf-8"));
          req.setAttribute("xml", content);
          this.errorHandler.forward(req, res);
        } else {
          LOGGER.error("A dynamic XSLT error was caught", ex);
//TODO          res.sendRedirect(req.getServletPath()+"/error/500");
        }
      } finally {
        if (errors.size() > 0)
          LOGGER.debug(errors.toString("utf-8"));
      }
    }
  }

// private helpers --------------------------------------------------------------------------------

  /**
   * Returns the XSLT templates to use.
   * 
   * @return the XSLT templates to use.
   * 
   * @throws TransformerConfigurationException Should an error occur whilst loading the templates
   */
  private Templates getTemplates() throws TransformerConfigurationException {
    // caching mechanism, storing templates using the style name
    if (this.enableCache) {
      if (this.cache == null) {
        LOGGER.info("Loading and caching style '"+this.styleSheet+'\'');
        this.cache = toTemplates(this.styleSheet);
      } else LOGGER.debug("Getting style '"+this.styleSheet+"' from cache");
      return this.cache;

    // caching mechanism disabled
    } else {
      LOGGER.debug("Loading XSLT template '"+this.styleSheet+"' [caching disabled]");
      return toTemplates(this.styleSheet);
    }
  }

  /**
   * Return the XSLT templates from the given style.
   *
   * @param stylepath The path to the XSLT style sheet
   *
   * @return the corresponding XSLT templates object
   * 
   * @throws TransformerConfigurationException Si the loading fails
   */
  private static Templates toTemplates(File stylepath) throws TransformerConfigurationException {
    // load the templates from the source file
    Source source = new StreamSource(stylepath);
    return factory.newTemplates(source);
  }

  /**
   * Returns the value for the specified init parameter name.
   * 
   * <p>If <code>null</code> returns the default value.
   * 
   * @param name The name of the init parameter.
   * @param def  The default value if the parameter value is <code>null</code> 
   * 
   * @return The values for the specified init parameter name.
   */
  private String getInitParameter(String name, String def) {
    String value = this.servletConfig.getInitParameter(name);
    return (value != null)? value : def;
  }

  /**
   * Returns the value for the specified init parameter name.
   * 
   * <p>If <code>null</code> returns the default value.
   *
   * @param name The name of the init parameter.
   * @param def  The default value if the parameter value is <code>null</code> 
   * 
   * @return The values for the specified init parameter name.
   */
  private boolean getInitParameter(String name, boolean def) {
    String value = this.servletConfig.getInitParameter(name);
    return (value != null)? Boolean.valueOf(value).booleanValue() : def;
  }

  /**
   * Sets the XSLT parameters for the transformer from the parameter and attributes
   * starting with 'xsl-'.
   * 
   * @param transformer The XSLT transformer.
   * @param req         The servlet request.
   */
  private static void setXSLTParameters(Transformer transformer, ServletRequest req) {
    // adding parameters from HTTP parameters
    for (Enumeration names = req.getParameterNames(); names.hasMoreElements();) {
      String param = (String)names.nextElement();
      if (param.startsWith("xsl-")) {
        transformer.setParameter(param.substring(4), req.getParameter(param));
      }
    }
    // adding parameters from request attributes
    for (Enumeration names = req.getAttributeNames(); names.hasMoreElements();) {
      String param = (String)names.nextElement();
      if (param.startsWith("xsl-")) {
        transformer.setParameter(param.substring(4), (String)req.getAttribute(param));
      }
    }
  }

  /**
   * Sets the output properties for the XSLT transformer.
   * 
   * @param transformer The XSLT transformer.
   * @param res         The servlet response.
   */
  private static void setOutputProperties(Transformer transformer, HttpServletResponse res) {
//    transformer.setOutputProperty("indent", logger.isDebugEnabled()? "yes" : "no");
  }

  /**
   * Sets up the listener so that its output can be captured.
   * 
   * @param listener The listener to setup
   * @param out      The output
   */
  private static void setupListener(ErrorListener listener, ByteArrayOutputStream out) {
    Class<? extends ErrorListener> elClass = listener.getClass();
    Method[] methods = elClass.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getName().equals("setErrorOutput")) {
        Class<?>[] params = methods[i].getParameterTypes();
        if (params.length == 1 && params[0].equals(PrintStream.class)) {
          try {
            PrintStream st = new PrintStream(out, true, "utf-8");
            methods[i].invoke(listener, new Object[]{st});
          } catch (Exception ex) {
            ex.printStackTrace();
            return;
          }
        }
      }
    }
  }

}
