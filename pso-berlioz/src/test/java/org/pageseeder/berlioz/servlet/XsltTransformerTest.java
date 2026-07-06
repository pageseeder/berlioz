package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.xslt.XsltErrorCollector;
import org.pageseeder.berlioz.xslt.XsltExceptionWrapper;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class XsltTransformerTest {

  @TempDir
  Path temporary;

  @BeforeAll
  static void initSettings() throws ReflectiveOperationException {
    settingsRef().compareAndSet(null, new HashMap<>());
  }

  @Test
  void testTransformFailSafeDoesNotResolveExternalEntity() throws Exception {
    File secret = Files.createFile(this.temporary.resolve("secret.txt")).toFile();
    Files.write(secret.toPath(), "LEAKED".getBytes(StandardCharsets.UTF_8));

    File stylesheet = Files.createFile(this.temporary.resolve("copy.xsl")).toFile();
    Files.write(stylesheet.toPath(), (
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>"
      + "<xsl:template match=\"/\"><out><xsl:value-of select=\"/root\"/></out></xsl:template>"
      + "</xsl:stylesheet>").getBytes(StandardCharsets.UTF_8));

    String xml = "<!DOCTYPE root [<!ENTITY xxe SYSTEM \""+secret.toURI()+"\">]><root>&xxe;</root>";
    String result = XsltTransformer.transformFailSafe(xml, stylesheet.toURI().toURL());

    Assertions.assertTrue(result.contains("<out"), result);
    Assertions.assertFalse(result.contains("LEAKED"), result);
  }

  @Test
  void testTransformFailSafeAllowsLocalStylesheetIncludes() throws Exception {
    File included = Files.createFile(this.temporary.resolve("included.xsl")).toFile();
    Files.write(included.toPath(), (
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:template name=\"included\"><included>ok</included></xsl:template>"
      + "</xsl:stylesheet>").getBytes(StandardCharsets.UTF_8));

    File stylesheet = Files.createFile(this.temporary.resolve("main.xsl")).toFile();
    Files.write(stylesheet.toPath(), (
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:include href=\"included.xsl\"/>"
      + "<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>"
      + "<xsl:template match=\"/\"><out><xsl:call-template name=\"included\"/></out></xsl:template>"
      + "</xsl:stylesheet>").getBytes(StandardCharsets.UTF_8));

    String result = XsltTransformer.transformFailSafe("<root/>", stylesheet.toURI().toURL());

    Assertions.assertEquals("<out><included>ok</included></out>", result);
  }

  // toXML(TransformerException, Map) — private, invoked via reflection to check the raw
  // error document produced for the fail-safe stylesheet, independent of its HTML rendering.

  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; covers legacy migration path
  @BeforeEach
  @AfterEach
  void resetErrorProblemFormat() throws ReflectiveOperationException {
    removeOption(BerliozOption.ERROR_PROBLEM_FORMAT);
    removeOption(BerliozOption.ERROR_DETAIL);
  }

  @Test
  void toXML_defaultsToProblemFormat() throws Exception {
    TransformerConfigurationException ex = new TransformerConfigurationException("bad stylesheet");
    String xml = invokeToXml(ex, null);

    Assertions.assertTrue(xml.contains("<problem>"), xml);
    Assertions.assertTrue(xml.contains("<type>urn:berlioz:problem:transform-invalid</type>"), xml);
    Assertions.assertTrue(xml.contains("<status>503</status>"), xml);
  }

  @Test
  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; covers legacy migration path
  void toXml_legacyFormat_producesServerErrorDocument() throws Exception {
    setOption(BerliozOption.ERROR_PROBLEM_FORMAT, "false");
    TransformerConfigurationException ex = new TransformerConfigurationException("bad stylesheet");

    String xml = invokeToXml(ex, null);

    Assertions.assertTrue(xml.contains("<error"), xml);
    Assertions.assertTrue(xml.contains("http-class=\"server-error\""), xml);
    Assertions.assertTrue(xml.contains("http-code=\"503\""), xml);
    Assertions.assertTrue(xml.contains("id=\"berlioz-transform-invalid\""), xml);
    Assertions.assertFalse(xml.contains("<server-error"), xml);
    Assertions.assertFalse(xml.contains("<problem>"), xml);
  }

  @Test
  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; covers legacy migration path
  void toXml_legacyFormat_minimalDetail_omitsExceptionDetail() throws Exception {
    setOption(BerliozOption.ERROR_PROBLEM_FORMAT, "false");
    setOption(BerliozOption.ERROR_DETAIL, "minimal");
    TransformerConfigurationException ex = new TransformerConfigurationException("bad stylesheet");

    String xml = invokeToXml(ex, null);

    Assertions.assertFalse(xml.contains("<exception"), xml);
  }

  @Test
  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; covers legacy migration path
  void toXml_legacyFormat_standardDetail_addsExceptionSummaryOnly() throws Exception {
    setOption(BerliozOption.ERROR_PROBLEM_FORMAT, "false");
    setOption(BerliozOption.ERROR_DETAIL, "standard");
    TransformerConfigurationException ex = new TransformerConfigurationException("bad stylesheet");

    String xml = invokeToXml(ex, null);

    Assertions.assertTrue(xml.contains("<exception class=\"" + TransformerConfigurationException.class.getName() + "\">"), xml);
    Assertions.assertFalse(xml.contains("<stack-trace>"), xml);
  }

  @Test
  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; covers legacy migration path
  void toXml_legacyFormat_fullDetail_addsStackTrace() throws Exception {
    setOption(BerliozOption.ERROR_PROBLEM_FORMAT, "false");
    setOption(BerliozOption.ERROR_DETAIL, "full");
    TransformerConfigurationException ex = new TransformerConfigurationException("bad stylesheet");

    String xml = invokeToXml(ex, null);

    Assertions.assertTrue(xml.contains("<stack-trace>"), xml);
  }

  @Test
  void toXml_problemFormat_withCollectedErrors_addsCollectedErrorsExtension() throws Exception {
    XsltErrorCollector collector = new XsltErrorCollector(org.slf4j.LoggerFactory.getLogger(XsltTransformerTest.class));
    collector.collectQuietly(Level.ERROR, new TransformerException("first failure"));
    collector.collectQuietly(Level.WARNING, new TransformerException("second failure"));
    XsltExceptionWrapper wrapper = new XsltExceptionWrapper(
        new TransformerConfigurationException("bad stylesheet"), collector);

    String xml = invokeToXml(wrapper, null);

    Assertions.assertTrue(xml.contains("<collected-errors>"), xml);
    Assertions.assertTrue(xml.contains("<collected level=\"error\">"), xml);
    Assertions.assertTrue(xml.contains("<collected level=\"warning\">"), xml);
    Assertions.assertTrue(xml.contains("first failure"), xml);
    Assertions.assertTrue(xml.contains("second failure"), xml);
  }

  @Test
  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; covers legacy migration path
  void toXml_legacyFormat_withCollectedErrors_keepsFlatShape() throws Exception {
    setOption(BerliozOption.ERROR_PROBLEM_FORMAT, "false");
    XsltErrorCollector collector = new XsltErrorCollector(org.slf4j.LoggerFactory.getLogger(XsltTransformerTest.class));
    collector.collectQuietly(Level.ERROR, new TransformerException("first failure"));
    XsltExceptionWrapper wrapper = new XsltExceptionWrapper(
        new TransformerConfigurationException("bad stylesheet"), collector);

    String xml = invokeToXml(wrapper, Map.of("xsl-mode", "preview"));

    Assertions.assertTrue(xml.contains("<collected-errors>"), xml);
    Assertions.assertTrue(xml.contains("<collected level=\"error\""), xml);
    // Legacy shape keeps XSLT parameters; dropped from the Problem Details branch
    Assertions.assertTrue(xml.contains("<parameters>"), xml);
    Assertions.assertTrue(xml.contains("xsl-mode"), xml);
  }

  private static String invokeToXml(TransformerException ex, Map<String, String> parameters) throws Exception {
    Method toXML = XsltTransformer.class.getDeclaredMethod("toXml", TransformerException.class, Map.class);
    toXML.setAccessible(true);
    return (String) toXML.invoke(null, ex, parameters);
  }

  private static void setOption(BerliozOption option, String value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().put(option.property(), value);
  }

  private static void removeOption(BerliozOption option) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().remove(option.property());
  }

  @SuppressWarnings("unchecked")
  private static AtomicReference<Map<String, String>> settingsRef() throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    return (AtomicReference<Map<String, String>>) f.get(null);
  }
}
