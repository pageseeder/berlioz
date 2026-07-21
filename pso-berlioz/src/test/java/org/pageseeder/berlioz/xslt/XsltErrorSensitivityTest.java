package org.pageseeder.berlioz.xslt;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.util.CollectedError.Level;

import static org.junit.jupiter.api.Assertions.*;

class XsltErrorSensitivityTest {

  @Test
  void option_usesExpectedPropertyAndDefault() {
    assertEquals("berlioz.xslt.sensitivity", BerliozOption.XSLT_SENSITIVITY.property());
    assertEquals("error", BerliozOption.XSLT_SENSITIVITY.defaultTo());
  }

  @Test
  void from_recognizesSupportedValuesCaseInsensitively() {
    assertEquals(XsltErrorSensitivity.FATAL, XsltErrorSensitivity.from("fatal"));
    assertEquals(XsltErrorSensitivity.ERROR, XsltErrorSensitivity.from("ERROR"));
    assertEquals(XsltErrorSensitivity.WARNING, XsltErrorSensitivity.from(" Warning "));
  }

  @Test
  void from_defaultsInvalidAndNullValuesToError() {
    assertEquals(XsltErrorSensitivity.ERROR, XsltErrorSensitivity.from("unknown"));
    assertEquals(XsltErrorSensitivity.ERROR, XsltErrorSensitivity.from(null));
  }

  @Test
  void includes_usesConfiguredThreshold() {
    assertFalse(XsltErrorSensitivity.FATAL.includes(Level.ERROR));
    assertTrue(XsltErrorSensitivity.FATAL.includes(Level.FATAL));
    assertFalse(XsltErrorSensitivity.ERROR.includes(Level.WARNING));
    assertTrue(XsltErrorSensitivity.ERROR.includes(Level.ERROR));
    assertTrue(XsltErrorSensitivity.WARNING.includes(Level.WARNING));
  }
}
