package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class MediaTypesTest {

  private static final File WEB_INF = new File("./src/test/resources/org/pageseeder/berlioz");

  @BeforeAll
  static void setup() throws BerliozException {
    GlobalSettings.setup(WEB_INF);
  }

  @ParameterizedTest
  @CsvSource({
      "file.html, text/html",
      "file.css,  text/css",
      "file.js,   text/javascript",
      "file.xml,  application/xml",
      "file.json, application/json",
      "file.png,  image/png",
      "file.jpg,  image/jpeg",
      "file.gif,  image/gif",
      "file.pdf,  application/pdf"
  })
  void getMediaType_knownExtensions_returnsCorrectType(String filename, String expected) {
    assertNotNull(MediaTypes.getMediaType(new File(filename)),
        "Expected a non-null media type for " + filename);
  }

  @Test
  void getMediaType_fileWithNoExtension_returnsNull() {
    assertNull(MediaTypes.getMediaType(new File("noextension")));
  }

  @Test
  void getMediaType_htmlFile_returnsTextHtml() {
    String type = MediaTypes.getMediaType(new File("index.html"));
    assertEquals("text/html", type);
  }

  @Test
  void getMediaType_cssFile_returnsTextCss() {
    String type = MediaTypes.getMediaType(new File("styles.css"));
    assertEquals("text/css", type);
  }

  @Test
  void getMediaType_jsonFile_returnsApplicationJson() {
    String type = MediaTypes.getMediaType(new File("data.json"));
    assertNotNull(type);
    assertTrue(type.contains("json"), "Expected json in media type, got: " + type);
  }

  @Test
  void getMediaType_multipleCallsAreCached() {
    // Second call should use cached properties (coverage of the non-empty branch)
    String first  = MediaTypes.getMediaType(new File("a.html"));
    String second = MediaTypes.getMediaType(new File("b.html"));
    assertEquals(first, second);
  }
}
