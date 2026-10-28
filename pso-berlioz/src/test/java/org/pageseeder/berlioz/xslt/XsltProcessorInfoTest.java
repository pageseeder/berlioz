package org.pageseeder.berlioz.xslt;

import javax.xml.transform.TransformerFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XsltProcessorInfoTest {

  @Test void identifiesTheRenderingProcessor() {
    XsltProcessorInfo info = XsltProcessorInfo.current();
    assertSame(info, XsltProcessorInfo.current());
    assertEquals(TransformerFactory.newInstance().getClass().getName(), info.factory());
    assertNotNull(info.vendor());
    assertNotNull(info.xsltVersion());
  }

  @Test void productIsReportedFromXslt2() {
    XsltProcessorInfo info = XsltProcessorInfo.probe();
    if (Double.parseDouble(info.xsltVersion()) >= 2.0) {
      assertNotNull(info.productName());
      assertNotNull(info.productVersion());
    } else {
      assertNull(info.productName());
      assertNull(info.productVersion());
    }
  }

}
