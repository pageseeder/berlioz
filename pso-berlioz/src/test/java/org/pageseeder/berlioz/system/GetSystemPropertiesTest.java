package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.util.Redaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetSystemPropertiesTest {

  private static final String SECRET_NAME = "fixture.password";
  private static final String SECRET_VALUE = "s3cr3t-fixture-value";
  private static final String PLAIN_NAME = "fixture.plain";
  private static final String PLAIN_VALUE = "plain-fixture-value";
  private static final String URL_NAME = "fixture.url";
  private static final String URL_VALUE = "jdbc:postgresql://app:s3cr3t-url@db/app?ssl=true&password=s3cr3t-param";

  @BeforeEach
  void setUp() {
    System.setProperty(SECRET_NAME, SECRET_VALUE);
    System.setProperty(PLAIN_NAME, PLAIN_VALUE);
    System.setProperty(URL_NAME, URL_VALUE);
  }

  @AfterEach
  void tearDown() {
    System.clearProperty(SECRET_NAME);
    System.clearProperty(PLAIN_NAME);
    System.clearProperty(URL_NAME);
  }

  // process() tests — XML
  // ---------------------------------------------------------------------------

  @Test
  void testProcess_containsPropertyElements() throws Exception {
    Element root = parseXml(process()).getDocumentElement();
    assertEquals("system", root.getTagName());
    NodeList props = root.getElementsByTagName("property");
    assertTrue(props.getLength() > 0, "Should have at least one <property> element");
    assertEquals(0, root.getElementsByTagName("properties").getLength(), "XML should not wrap properties");
  }

  @Test
  void testProcess_includesJavaVersion() throws Exception {
    Element p = findProperty(parseXml(process()), "java.version");
    assertNotNull(p, "Should contain java.version property");
    assertFalse(p.getAttribute("value").isEmpty(), "java.version should not be empty");
  }

  @Test
  void testProcess_redactsSensitiveValues() throws Exception {
    String out = process();
    Document doc = parseXml(out);
    assertEquals(Redaction.REDACTED, findProperty(doc, SECRET_NAME).getAttribute("value"));
    assertEquals(PLAIN_VALUE, findProperty(doc, PLAIN_NAME).getAttribute("value"));
    assertFalse(out.contains(SECRET_VALUE), "Secret value must not appear in output");
  }

  @Test
  void testProcess_redactsCredentialsInValues() throws Exception {
    String out = process();
    assertEquals("jdbc:postgresql://app:[REDACTED]@db/app?ssl=true&password=[REDACTED]",
        findProperty(parseXml(out), URL_NAME).getAttribute("value"));
    assertFalse(out.contains("s3cr3t"), "Credentials must not appear in output");
  }

  @Test
  void testProcessJson_redactsCredentialsInValues() {
    String out = processJson();
    assertTrue(out.contains("{\"name\":\"" + URL_NAME + "\",\"value\":"
        + "\"jdbc:postgresql://app:[REDACTED]@db/app?ssl=true&password=[REDACTED]\"}"), out);
    assertFalse(out.contains("s3cr3t"), "Credentials must not appear in output");
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "com.example.Main                          | com.example.Main",
      "com.example.Main --password s3cret        | com.example.Main [REDACTED]",
      "/opt/app/My App.jar --token=abc           | /opt/app/My App.jar [REDACTED]",
      "/opt/app/app.JAR                          | /opt/app/app.JAR",
      "'  com.example.Main   '                   | com.example.Main",
      // A JAR in the arguments of a main class is not the executable
      "com.example.Main --password s3cr3t input.jar        | com.example.Main [REDACTED]",
      "com.example.Main input.jar --password s3cr3t        | com.example.Main [REDACTED]",
      "com.example.Main --password s3cr3t in.jar --key=k2  | com.example.Main [REDACTED]",
      "com.example.Main /opt/My Data/in.jar s3cr3t         | com.example.Main [REDACTED]",
      "Main s3cr3t                                         | Main [REDACTED]",
      "com.example.$Main s3cr3t                            | com.example.$Main [REDACTED]",
      // Module and main class (java -m)
      "com.example.app/com.example.Main s3cr3t x.jar       | com.example.app/com.example.Main [REDACTED]",
      // JAR launches: the first .jar ends the executable, whatever follows
      "/opt/app/app.jar --password s3cr3t input.jar        | /opt/app/app.jar [REDACTED]",
      "/opt/My Apps/app.jar s3cr3t /data/My Data/in.jar    | /opt/My Apps/app.jar [REDACTED]",
      "lib/app.jar s3cr3t                                  | lib/app.jar [REDACTED]",
      "'C:\\Program Files\\App\\app.jar s3cr3t in.jar'     | 'C:\\Program Files\\App\\app.jar [REDACTED]'",
      // Ambiguous: a relative path with spaces is truncated rather than showing arguments
      "My App.jar s3cr3t                                   | My [REDACTED]",
      "/opt/app/run s3cr3t                                 | /opt/app/run [REDACTED]"})
  void testRedactCommand(String command, String expected) {
    assertEquals(expected, GetSystemProperties.redactCommand(command));
  }

  @Test
  void testProcess_sortedByName() throws Exception {
    NodeList props = parseXml(process()).getElementsByTagName("property");
    List<String> names = new ArrayList<>();
    for (int i = 0; i < props.getLength(); i++) {
      names.add(((Element) props.item(i)).getAttribute("name"));
    }
    List<String> sorted = new ArrayList<>(names);
    sorted.sort(null);
    assertEquals(sorted, names, "Properties should be sorted by name");
  }

  // process() tests — JSON
  // ---------------------------------------------------------------------------

  @Test
  void testProcessJson_propertiesArray() {
    String out = processJson();
    assertTrue(out.startsWith("{\"properties\":[{\"name\":"), "Should start with properties array: " + out.substring(0, 40));
  }

  @Test
  void testProcessJson_redactsSensitiveValues() {
    String out = processJson();
    assertTrue(out.contains("{\"name\":\"" + SECRET_NAME + "\",\"value\":\"" + Redaction.REDACTED + "\"}"));
    assertTrue(out.contains("{\"name\":\"" + PLAIN_NAME + "\",\"value\":\"" + PLAIN_VALUE + "\"}"));
    assertFalse(out.contains(SECRET_VALUE), "Secret value must not appear in output");
  }

  @Test
  void testProcessJson_sortedByName() {
    String out = processJson();
    int secret = out.indexOf("\"" + SECRET_NAME + "\"");
    int plain = out.indexOf("\"" + PLAIN_NAME + "\"");
    int java = out.indexOf("\"java.version\"");
    assertTrue(secret > 0 && plain > secret && java > plain, "Properties should be sorted by name");
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process() {
    OutputWriter out = new XmlOutputAdapter();
    new GetSystemProperties().generate(emptyRequest(), out);
    return out.toString();
  }

  private static String processJson() {
    OutputWriter out = new JsonOutputAdapter();
    new GetSystemProperties().generate(emptyRequest(), out);
    return out.toString();
  }

  private static Element findProperty(Document doc, String name) {
    NodeList props = doc.getElementsByTagName("property");
    for (int i = 0; i < props.getLength(); i++) {
      Element p = (Element) props.item(i);
      if (name.equals(p.getAttribute("name"))) return p;
    }
    return null;
  }

  private static Request emptyRequest() {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, m, args) -> null);
  }

  private static Document parseXml(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
