/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.error;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.Errors;
import org.pageseeder.berlioz.xslt.XsltExceptionWrapper;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A structured, XSLT-specific extension member for an RFC 9457 problem details response.
 *
 * <p>Unlike {@link ExceptionDetail} — which is built around the Java stack trace as the primary
 * diagnostic — this type is tailored to what's actionable when debugging a stylesheet failure:
 * where in the source it happened (stylesheet URI, line, column), whether it's a static
 * (stylesheet won't compile) or dynamic (this document triggered a runtime error) failure, the
 * engine's own error code when available, and the trail of recoverable warnings/errors that
 * preceded the fatal one. It never carries a JVM stack trace or Java cause chain — for a
 * {@link TransformerException} those bottom out in transformer-engine internals rather than
 * anything a stylesheet author can act on.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
@Beta
public final class XsltErrorDetail implements ProblemExtension {

  private static final String NAME = "xslt-error";

  /**
   * Whether the failure was detected while compiling the stylesheet ({@link #STATIC}) or while
   * running it against a specific source document ({@link #DYNAMIC}).
   */
  public enum Kind {

    /** The stylesheet itself could not be compiled — a deployment/authoring bug. */
    STATIC,

    /** The stylesheet compiled but failed at run time against this specific input. */
    DYNAMIC;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  /**
   * Best-effort lookup for Saxon's {@code XPathException.getErrorCodeQName()} and
   * {@code StructuredQName.getLocalPart()}, probed once. {@code pso-berlioz} does not depend on
   * Saxon at compile time (it's {@code testRuntimeOnly}), so this is resolved reflectively,
   * exactly like {@code XsltTransformer}'s XSLT 2.0 probe.
   */
  private static final @Nullable Method SAXON_ERROR_CODE_QNAME =
      probeMethod("net.sf.saxon.trans.XPathException", "getErrorCodeQName");
  private static final @Nullable Method SAXON_QNAME_LOCAL_PART =
      probeMethod("net.sf.saxon.om.StructuredQName", "getLocalPart");

  private final String className;
  private final Kind kind;
  private final @Nullable String code;
  private final String message;
  private final @Nullable SourceLocation location;
  private final List<CollectedItem> collected;

  private XsltErrorDetail(String className, Kind kind, @Nullable String code, String message,
      @Nullable SourceLocation location, List<CollectedItem> collected) {
    this.className = className;
    this.kind = kind;
    this.code = code;
    this.message = message;
    this.location = location;
    this.collected = collected;
  }

  /**
   * Creates an {@code XsltErrorDetail} from a transformer exception, with no collected errors.
   *
   * @param ex the transformer exception; if an {@link XsltExceptionWrapper}, it is unwrapped and
   *           its collected errors are included automatically
   * @return a new {@code XsltErrorDetail}
   */
  public static XsltErrorDetail of(TransformerException ex) {
    if (ex instanceof XsltExceptionWrapper) return of((XsltExceptionWrapper) ex);
    return of(ex, List.of());
  }

  /**
   * Creates an {@code XsltErrorDetail} from a transformer exception and the warnings/errors
   * collected before it was thrown.
   *
   * @param ex        the transformer exception that terminated the transform
   * @param collected the warnings/errors collected before {@code ex}, in report order
   * @return a new {@code XsltErrorDetail}
   */
  public static XsltErrorDetail of(TransformerException ex, List<CollectedError<TransformerException>> collected) {
    Objects.requireNonNull(ex, "ex");
    Objects.requireNonNull(collected, "collected");
    Kind kind = ex instanceof TransformerConfigurationException ? Kind.STATIC : Kind.DYNAMIC;
    List<CollectedItem> items = new ArrayList<>(collected.size());
    for (CollectedError<TransformerException> item : collected) {
      items.add(CollectedItem.of(item));
    }
    return new XsltErrorDetail(ex.getClass().getName(), kind, errorCode(ex),
        Errors.cleanMessage(ex), SourceLocation.of(ex.getLocator()), List.copyOf(items));
  }

  /**
   * Creates an {@code XsltErrorDetail} from a wrapped transformer exception, using the wrapper's
   * {@link org.pageseeder.berlioz.xslt.XsltErrorCollector} for the collected warnings/errors.
   *
   * @param wrapper the wrapped exception produced by {@code XsltTransformer}
   * @return a new {@code XsltErrorDetail}
   */
  public static XsltErrorDetail of(XsltExceptionWrapper wrapper) {
    Objects.requireNonNull(wrapper, "wrapper");
    Throwable wrapped = wrapper.getException();
    TransformerException actual = wrapped instanceof TransformerException
        ? (TransformerException) wrapped : wrapper;
    return of(actual, wrapper.collector().getErrors());
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public OutputWriter writeTo(OutputWriter out) {
    out.startObject(NAME);
    out.field("class", this.className);
    out.field("kind", this.kind.toString());
    out.optionalField("code", this.code);
    out.field("message", this.message, OutputWriter.FieldOption.XML_ELEMENT);
    if (this.location != null) this.location.writeTo(out);
    if (!this.collected.isEmpty()) {
      out.startArray("collected");
      for (CollectedItem item : this.collected) {
        item.writeTo(out);
      }
      out.endArray();
    }
    return out.endObject();
  }

  // --- Private helpers -------------------------------------------------------------------------

  private static @Nullable String errorCode(TransformerException ex) {
    if (SAXON_ERROR_CODE_QNAME == null || SAXON_QNAME_LOCAL_PART == null
        || !SAXON_ERROR_CODE_QNAME.getDeclaringClass().isInstance(ex)) return null;
    try {
      Object qname = SAXON_ERROR_CODE_QNAME.invoke(ex);
      if (qname == null) return null;
      Object localPart = SAXON_QNAME_LOCAL_PART.invoke(qname);
      return localPart != null ? localPart.toString() : null;
    } catch (IllegalAccessException | InvocationTargetException ex2) {
      return null;
    }
  }

  private static @Nullable Method probeMethod(String className, String methodName) {
    try {
      return Class.forName(className).getMethod(methodName);
    } catch (ReflectiveOperationException ex) {
      return null;
    }
  }

  /**
   * One collected warning/error that preceded the fatal exception: level, clean message, and
   * source location when available — never a stack trace.
   */
  private static final class CollectedItem {

    private final String level;
    private final String message;
    private final @Nullable SourceLocation location;

    private CollectedItem(String level, String message, @Nullable SourceLocation location) {
      this.level = level;
      this.message = message;
      this.location = location;
    }

    private static CollectedItem of(CollectedError<TransformerException> item) {
      TransformerException ex = item.error();
      return new CollectedItem(item.level().toString(), Errors.cleanMessage(ex),
          SourceLocation.of(ex.getLocator()));
    }

    private void writeTo(OutputWriter out) {
      out.startObject("error");
      out.field("level", this.level);
      out.field("message", this.message, OutputWriter.FieldOption.XML_ELEMENT);
      if (this.location != null) this.location.writeTo(out);
      out.endObject();
    }
  }

}
