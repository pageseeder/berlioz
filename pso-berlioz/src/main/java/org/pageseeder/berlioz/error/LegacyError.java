package org.pageseeder.berlioz.error;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.*;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.http.HttpStatusCodes;
import org.pageseeder.berlioz.servlet.ErrorHandlerServlet;
import org.pageseeder.berlioz.util.*;
import org.pageseeder.berlioz.xml.XmlWritable;
import org.pageseeder.berlioz.xml.XmlWriter;
import org.pageseeder.xmlwriter.XML;
import org.pageseeder.xmlwriter.XMLStringWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LegacyError implements XmlWritable {


  private static final String REDACTED = "[REDACTED]";

  private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
      "authorization",
      "proxy-authorization",
      "cookie",
      "set-cookie"
  );

  private static final String[] SENSITIVE_HEADER_KEYWORDS = {
      "token",
      "secret",
      "credential",
      "apikey",
      "session",
      "csrf"
  };

  private static final String[] SENSITIVE_PARAMETER_KEYWORDS = {
      "password",
      "passwd",
      "pwd",
      "secret",
      "apikey",
      "token",
      "credential",
      "privatekey",
      "session",
      "auth",
      "assertion",
      "saml",
      "jwt",
      "csrf"
  };

  private final int code;

  private final @Nullable String message;

  private final @Nullable Throwable throwable;

  private final @Nullable String requestUri;

  private final String errorId;

  private final @Nullable HttpServletRequest servletRequest;

  private LegacyError(int code, @Nullable String message, @Nullable Throwable throwable, @Nullable String requestUri, String errorId, @Nullable HttpServletRequest servletRequest) {
    this.code = code;
    this.message = message;
    this.throwable = throwable;
    this.requestUri = requestUri;
    this.errorId = errorId;
    this.servletRequest = servletRequest;
  }

  public static LegacyError of(Request req) {
    try {
      String message = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
      Integer code = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
      Throwable exception = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
      String requestUri = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

      String errorId = (String) req.getAttribute(ErrorHandlerServlet.BERLIOZ_ERROR_ID);

      if (code == null) code = HttpServletResponse.SC_OK;
      errorId = resolveErrorId(exception, errorId);

      return new LegacyError(code, message, exception, requestUri, errorId, null);
    } catch (Exception ex) {
      return new LegacyError(500, "Error retrieving error information", ex, null, BerliozErrorID.UNEXPECTED.toString(), null);
    }
  }

  public static LegacyError of(HttpServletRequest req) {
    String currentUri = req.getRequestURI();
    try {
      String message = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
      Integer code = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
      Throwable exception = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
      String requestUri = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

      // Fall back values
      String errorId = (String) req.getAttribute(ErrorHandlerServlet.BERLIOZ_ERROR_ID);
      errorId = resolveErrorId(exception, errorId);
      if (requestUri == null) requestUri = currentUri;
      if (code == null) code = HttpServletResponse.SC_OK;

      return new LegacyError(code, message, exception, requestUri, errorId, req);
    } catch (Exception ex) {
      return new LegacyError(500, "Error retrieving error information", ex, null, BerliozErrorID.UNEXPECTED.toString(), req);
    }
  }

  @Override
  public XmlWriter toXml(XmlWriter xml) {

    // Generator
    String element = "error";
    if (this.servletRequest != null) element = toHttpClass(this.code);
    xml.openElement(element);
    xml.attribute("http-class", toHttpClass(this.code));
    xml.attribute("http-code", this.code);
    xml.attribute("datetime", ISO8601.format(System.currentTimeMillis(), ISO8601.DATETIME));
    xml.attribute("id", this.errorId);

    // Berlioz info
    xml.openElement("berlioz");
    xml.attribute("version", GlobalSettings.getVersion());
    xml.closeElement();

    // Other informational elements
    String title = HttpStatusCodes.getTitle(code);
    xml.element("title", title != null ? title : "Berlioz Status");
    if (message != null) {
      xml.element("message", message);
    }
    if (requestUri != null) {
      xml.element("request-uri", requestUri);
    }

    // Add servlet in request mode
    if (this.servletRequest != null) {
      String servlet = (String) servletRequest.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
      xml.element("servlet", servlet != null ? servlet : "null");
    }

    DetailLevel detail = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL));
    if (detail == DetailLevel.STANDARD) {
      writeThrowableSummary(xml, throwable);
    } else if (detail == DetailLevel.FULL) {
      writeThrowable(xml, throwable);
      if (this.servletRequest != null) {
        writeHttpHeaders(xml, this.servletRequest);
        writeHttpParameters(xml, this.servletRequest);
      }
    }

    xml.closeElement();
    return xml;
  }

  private static void writeThrowable(XmlWriter xml, @Nullable Throwable throwable) {
    if (throwable == null) return;
    xml.xml(toThrowable(throwable));
  }

  private static void writeThrowableSummary(XmlWriter xml, @Nullable Throwable throwable) {
    if (throwable == null) return;
    xml.openElement("exception");
    xml.attribute("class", throwable.getClass().getName());
    String message = throwable.getMessage();
    if (message != null) {
      xml.element("message", message);
    }
    xml.closeElement();
  }

  private static void writeHttpHeaders(XmlWriter xml, HttpServletRequest req) {
    xml.openElement("http-headers");
    Enumeration<?> names = req.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement().toString();
      Enumeration<?> values = req.getHeaders(name);
      if (values != null) {
        while (values.hasMoreElements()) {
          String value = values.nextElement().toString();
          xml.openElement("header")
              .attribute("name", name)
              .attribute("value", redactHeaderValue(name, value))
              .closeElement();
        }
      }
    }
    xml.closeElement();
  }

  private static void writeHttpParameters(XmlWriter xml, HttpServletRequest req) {
    xml.openElement("http-parameters");
    Map<?, ?> parameters = req.getParameterMap();
    for (Map.Entry<?, ?> entry : parameters.entrySet()) {
      String name = entry.getKey().toString();
      // Must be an array, according to Servlet Specifications
      String[] values = (String[]) entry.getValue();
      if (values != null) {
        for (String value : values) {
          xml.openElement("parameter")
              .attribute("name", name)
              .attribute("value", redactParameterValue(name, value))
              .closeElement();
        }
      }
    }
    xml.closeElement();
  }

  /**
   * Return the root element name based on the status code.
   *
   * @param code the HTTP status code.
   * @return the root element name based on the HTTP status code or "unknown-status";
   */
  private static String toHttpClass(int code) {
    String element = HttpStatusCodes.getClassOfStatus(code);
    return (element != null)? element.toLowerCase().replace(' ', '-') : "unknown-status";
  }

  private static String toThrowable(Throwable throwable) {
    XMLStringWriter buffer = new XMLStringWriter(XML.NamespaceAware.No);
    try {
      Errors.toXML(throwable, buffer, true);

      // If some errors were collected, let's include them
      if (throwable instanceof CompoundBerliozException) {
        buffer.openElement("collected-errors");
        ErrorCollector<? extends Throwable> collector = ((CompoundBerliozException) throwable).getCollector();
        for (CollectedError<? extends Throwable> collected : collector.getErrors()) {
          collected.toXML(buffer);
        }
        buffer.closeElement();
      }

    } catch (IOException e) {
      // Will never happen since we send a string buffer, we can safely ignore
    }
    return buffer.toString();
  }

  private static String resolveErrorId(@Nullable Throwable exception, @Nullable String errorId) {
    if (exception instanceof BerliozException) {
      ErrorID id = ((BerliozException) exception).id();
      if (id != null) return id.id();
    }
    return errorId != null ? errorId : BerliozErrorID.UNEXPECTED.toString();
  }


  private static String redactHeaderValue(String name, String value) {
    return isSensitiveHeader(name) ? REDACTED : value;
  }

  private static String redactParameterValue(String name, String value) {
    return isSensitiveParameter(name) ? REDACTED : value;
  }

  private static boolean isSensitiveHeader(String name) {
    String lowerName = name.toLowerCase(Locale.ROOT);
    return SENSITIVE_HEADER_NAMES.contains(lowerName) ||
        containsSensitiveKeyword(normalizeName(lowerName), SENSITIVE_HEADER_KEYWORDS);
  }

  private static boolean isSensitiveParameter(String name) {
    String normalizedName = normalizeName(name.toLowerCase(Locale.ROOT));
    return containsSensitiveKeyword(normalizedName, SENSITIVE_PARAMETER_KEYWORDS);
  }

  private static boolean containsSensitiveKeyword(String normalizedName, String[] keywords) {
    for (String keyword : keywords) {
      if (normalizedName.contains(keyword)) return true;
    }
    return false;
  }

  private static String normalizeName(String name) {
    return name.replace("-", "").replace("_", "").replace(".", "");
  }



}
