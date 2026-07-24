package org.pageseeder.berlioz.xslt;

import java.nio.file.NoSuchFileException;
import java.time.Duration;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozErrorID;
import org.xml.sax.SAXParseException;

import static org.junit.jupiter.api.Assertions.*;

class XsltTransformExceptionTest {

  @Test
  void stylesheetConfigurationFailure_isStaticAndInvalid() {
    TransformerConfigurationException cause = new TransformerConfigurationException("invalid");
    XsltTransformException failure = XsltTransformException.of(
        XsltTransformException.Phase.STYLESHEET, cause);

    assertEquals(XsltTransformException.Phase.STYLESHEET, failure.phase());
    assertEquals(BerliozErrorID.TRANSFORM_INVALID, failure.id());
    assertSame(cause, failure.transformerException());
  }

  @Test
  void missingStylesheet_isIdentifiedThroughNestedCause() {
    TransformerConfigurationException cause = new TransformerConfigurationException(
        "missing", new NoSuchFileException("missing.xsl"));
    XsltTransformException failure = XsltTransformException.of(
        XsltTransformException.Phase.STYLESHEET, cause);

    assertEquals(BerliozErrorID.TRANSFORM_NOT_FOUND, failure.id());
  }

  @Test
  void malformedSource_isDetectedDuringExecution() {
    SAXParseException parse = new SAXParseException("malformed", null, "request.src", 4, 2);
    TransformerException cause = new TransformerException("source failed", parse);
    XsltTransformException failure = XsltTransformException.duringExecution(cause);

    assertEquals(XsltTransformException.Phase.SOURCE_XML, failure.phase());
    assertEquals(BerliozErrorID.TRANSFORM_MALFORMED_SOURCE_XML, failure.id());
  }

  @Test
  void ordinaryExecutionFailure_isDynamic() {
    XsltTransformException failure = XsltTransformException.duringExecution(
        new TransformerException("dynamic"));

    assertEquals(XsltTransformException.Phase.EXECUTION, failure.phase());
    assertEquals(BerliozErrorID.TRANSFORM_DYNAMIC_ERROR, failure.id());
  }

  @Test
  void cyclicCauseChain_isBounded() {
    RuntimeException first = new RuntimeException("first");
    RuntimeException second = new RuntimeException("second");
    first.initCause(second);
    second.initCause(first);
    TransformerException cause = new TransformerException("dynamic", first);

    XsltTransformException failure = assertTimeoutPreemptively(Duration.ofSeconds(1),
        () -> XsltTransformException.duringExecution(cause));

    assertEquals(XsltTransformException.Phase.EXECUTION, failure.phase());
    assertEquals(BerliozErrorID.TRANSFORM_DYNAMIC_ERROR, failure.id());
  }

  @Test
  void wrapper_isRetainedForCollectedDiagnostics() {
    XsltErrorCollector collector = new XsltErrorCollector(
        org.slf4j.LoggerFactory.getLogger(XsltTransformExceptionTest.class));
    XsltExceptionWrapper wrapper = new XsltExceptionWrapper(
        new TransformerConfigurationException("invalid"), collector);

    XsltTransformException failure = XsltTransformException.of(
        XsltTransformException.Phase.STYLESHEET, wrapper);

    assertSame(wrapper, failure.transformerException());
    assertEquals(BerliozErrorID.TRANSFORM_INVALID, failure.id());
  }

  @Test
  void factory_rejectsNullArguments() {
    TransformerException cause = new TransformerException("failure");
    assertThrows(NullPointerException.class, () -> XsltTransformException.of(null, cause));
    assertThrows(NullPointerException.class,
        () -> XsltTransformException.of(XsltTransformException.Phase.EXECUTION, null));
  }
}
