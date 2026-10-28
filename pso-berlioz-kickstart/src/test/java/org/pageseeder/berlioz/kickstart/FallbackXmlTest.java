package org.pageseeder.berlioz.kickstart;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the kickstart XML fallback only exposes allowlisted parts of the Berlioz envelope.
 */
class FallbackXmlTest {

  private static Templates templates;

  private static String output;

  @BeforeAll
  static void transformEnvelope() throws TransformerException {
    URL xslt = Objects.requireNonNull(FallbackXmlTest.class.getResource("fallback-xml.xsl"));
    URL envelope = Objects.requireNonNull(FallbackXmlTest.class.getResource("envelope.xml"));
    templates = TransformerFactory.newInstance().newTemplates(new StreamSource(xslt.toString()));
    output = transform(new StreamSource(envelope.toString()));
  }

  @Test
  void dropsHeader() {
    assertFalse(output.contains("<header"), output);
    assertFalse(output.contains("s3cr3t"), output);
    assertFalse(output.contains("abc123"), output);
    assertFalse(output.contains("internal.example"), output);
    assertFalse(output.contains("reflected"), output);
  }

  @Test
  void replacesHeaderWithComment() {
    assertTrue(output.contains("<root service=\"get-item\" group=\"items\" flags=\"admin\"><!-- header removed, attributes pruned -->"), output);
  }

  @Test
  void keepsOnlyServiceGroupAndFlagsOnRoot() {
    assertTrue(output.contains("<root service=\"get-item\" group=\"items\" flags=\"admin\">"), output);
  }

  @Test
  void keepsOnlyPublicContentAttributes() {
    assertTrue(output.contains("<content name=\"item\" target=\"main\" status=\"ok\">"), output);
    assertTrue(output.contains("<content name=\"failure\" status=\"not-found\">"), output);
    assertFalse(output.contains("com.example.internal"), output);
    assertFalse(output.contains("etag="), output);
    assertFalse(output.contains("deprecated="), output);
    assertFalse(output.contains("profile"), output);
  }

  @Test
  void copiesPayloadUnchanged() {
    assertTrue(output.contains("<item xmlns:x=\"urn:example\" id=\"42\" generator=\"payload-attribute\">"
        + "<x:title>Item <b>42</b></x:title><!-- kept --></item>"), output);
    assertTrue(output.contains("<problem><status>404</status><title>Not Found</title></problem>"), output);
  }

  @Test
  void ignoresDocumentsWithoutEnvelope() throws TransformerException {
    String result = transform(new StreamSource(new StringReader("<other secret=\"x\">text</other>")));
    assertFalse(result.contains("secret"), result);
    assertFalse(result.contains("text"), result);
  }

  private static String transform(StreamSource source) throws TransformerException {
    StringWriter out = new StringWriter();
    templates.newTransformer().transform(source, new StreamResult(out));
    return out.toString();
  }
}
