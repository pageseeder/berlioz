package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

class XsltTransformerTest {

  @TempDir
  Path temporary;

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
}
