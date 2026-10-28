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
package org.pageseeder.berlioz.xslt;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identifies the XSLT processor Berlioz uses to render stylesheets.
 *
 * <p>The processor is created the same way as for rendering. Its vendor, XSLT version and
 * product are read from the standard XSLT system properties (<code>xsl:vendor</code>,
 * <code>xsl:version</code>, <code>xsl:product-name</code> and <code>xsl:product-version</code>),
 * so no processor-specific API is needed. The product properties are only defined from XSLT 2.0:
 * they are absent for XSLT 1.0 processors.
 *
 * <p>The identity is determined once, on first use, and then reused.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.5
 */
public final class XsltProcessorInfo {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltProcessorInfo.class);

  private static final String PROBE = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
      + "<xsl:output method='text'/>"
      + "<xsl:template match='/'>"
      + "<xsl:value-of select=\"system-property('xsl:vendor')\"/><xsl:text>&#10;</xsl:text>"
      + "<xsl:value-of select=\"system-property('xsl:version')\"/><xsl:text>&#10;</xsl:text>"
      + "<xsl:value-of select=\"system-property('xsl:product-name')\"/><xsl:text>&#10;</xsl:text>"
      + "<xsl:value-of select=\"system-property('xsl:product-version')\"/>"
      + "</xsl:template>"
      + "</xsl:stylesheet>";

  private final @Nullable String factory;

  private final @Nullable String vendor;

  private final @Nullable String xsltVersion;

  private final @Nullable String productName;

  private final @Nullable String productVersion;

  XsltProcessorInfo(@Nullable String factory, @Nullable String vendor, @Nullable String xsltVersion,
      @Nullable String productName, @Nullable String productVersion) {
    this.factory = factory;
    this.vendor = vendor;
    this.xsltVersion = xsltVersion;
    this.productName = productName;
    this.productVersion = productVersion;
  }

  /**
   * @return the identity of the XSLT processor used by Berlioz.
   */
  public static XsltProcessorInfo current() {
    return Holder.INSTANCE;
  }

  /**
   * @return the class name of the <code>TransformerFactory</code>, or <code>null</code> if no
   *         factory could be created.
   */
  public @Nullable String factory() {
    return this.factory;
  }

  /**
   * @return the value of <code>xsl:vendor</code>, for example "Saxonica".
   */
  public @Nullable String vendor() {
    return this.vendor;
  }

  /**
   * @return the value of <code>xsl:version</code>: the XSLT version the processor implements.
   */
  public @Nullable String xsltVersion() {
    return this.xsltVersion;
  }

  /**
   * @return the value of <code>xsl:product-name</code>, for example "SAXON"; <code>null</code>
   *         for XSLT 1.0 processors.
   */
  public @Nullable String productName() {
    return this.productName;
  }

  /**
   * @return the value of <code>xsl:product-version</code>, for example "HE 12.4";
   *         <code>null</code> for XSLT 1.0 processors.
   */
  public @Nullable String productVersion() {
    return this.productVersion;
  }

  /**
   * Creates a processor the same way as for rendering and reads its identity.
   *
   * @return the processor identity; only the values that could be read are set.
   */
  static XsltProcessorInfo probe() {
    TransformerFactory factory;
    try {
      factory = XsltTemplateCache.newTransformerFactory();
    } catch (TransformerException | TransformerFactoryConfigurationError ex) {
      LOGGER.warn("Unable to create the XSLT processor to identify it", ex);
      return new XsltProcessorInfo(null, null, null, null, null);
    }
    String name = factory.getClass().getName();
    try {
      StringWriter out = new StringWriter();
      factory.newTransformer(new StreamSource(new StringReader(PROBE)))
          .transform(new StreamSource(new StringReader("<probe/>")), new StreamResult(out));
      String[] values = out.toString().split("\n", -1);
      return new XsltProcessorInfo(name, value(values, 0), value(values, 1), value(values, 2), value(values, 3));
    } catch (TransformerException ex) {
      LOGGER.warn("Unable to read the XSLT processor system properties", ex);
      return new XsltProcessorInfo(name, null, null, null, null);
    }
  }

  private static @Nullable String value(String[] values, int index) {
    if (index >= values.length) return null;
    String value = values[index].trim();
    return value.isEmpty() ? null : value;
  }

  /** Lazy holder: the processor is identified on first use only. */
  private static final class Holder {
    private static final XsltProcessorInfo INSTANCE = probe();
  }

}
