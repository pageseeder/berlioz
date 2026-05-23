package org.pageseeder.berlioz.servlet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class XSLTransformerTest {

  @Rule
  public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void testTransformFailSafeDoesNotResolveExternalEntity() throws Exception {
    File secret = this.temporary.newFile("secret.txt");
    Files.write(secret.toPath(), "LEAKED".getBytes(StandardCharsets.UTF_8));

    File stylesheet = this.temporary.newFile("copy.xsl");
    Files.write(stylesheet.toPath(), (
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>"
      + "<xsl:template match=\"/\"><out><xsl:value-of select=\"/root\"/></out></xsl:template>"
      + "</xsl:stylesheet>").getBytes(StandardCharsets.UTF_8));

    String xml = "<!DOCTYPE root [<!ENTITY xxe SYSTEM \""+secret.toURI()+"\">]><root>&xxe;</root>";
    String result = XSLTransformer.transformFailSafe(xml, stylesheet.toURI().toURL());

    Assert.assertTrue(result, result.contains("<out"));
    Assert.assertFalse(result, result.contains("LEAKED"));
  }

  @Test
  public void testTransformFailSafeAllowsLocalStylesheetIncludes() throws Exception {
    File included = this.temporary.newFile("included.xsl");
    Files.write(included.toPath(), (
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:template name=\"included\"><included>ok</included></xsl:template>"
      + "</xsl:stylesheet>").getBytes(StandardCharsets.UTF_8));

    File stylesheet = this.temporary.newFile("main.xsl");
    Files.write(stylesheet.toPath(), (
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:include href=\"included.xsl\"/>"
      + "<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>"
      + "<xsl:template match=\"/\"><out><xsl:call-template name=\"included\"/></out></xsl:template>"
      + "</xsl:stylesheet>").getBytes(StandardCharsets.UTF_8));

    String result = XSLTransformer.transformFailSafe("<root/>", stylesheet.toURI().toURL());

    Assert.assertEquals("<out><included>ok</included></out>", result);
  }
}
