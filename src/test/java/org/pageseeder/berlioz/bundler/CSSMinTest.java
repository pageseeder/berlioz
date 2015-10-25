package org.pageseeder.berlioz.bundler;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public class CSSMinTest {

  @Test public void testEmptyRule() {
    Assert.assertEquals("a{}", min("a { }"));
  }

  @Test public void testTrailingSemicolon() {
    Assert.assertEquals("a{color:#000}", min("a { color: #000;}"));
  }

  @Test public void testSingleProperty() {
    Assert.assertEquals("a{font-size:10px}", min("a { font-size: 10px}"));
  }

  @Test public void testMultipleProperties() {
    Assert.assertEquals("a{color:#000;font-size:10px}", min("a { color: #000; font-size: 10px }"));
  }

  @Test public void testSimplifyParameters() {
    Assert.assertEquals("a{border:1px 2px 3px}", min("a { border: 1px 2px 3px 2px}"));
    Assert.assertEquals("a{border:1px 2px}",     min("a { border: 1px 2px 1px 2px}"));
    Assert.assertEquals("a{border:1px 2px}",     min("a { border: 1px 2px 1px}"));
    Assert.assertEquals("a{border:1px}",         min("a { border: 1px 1px 1px 1px}"));
    Assert.assertEquals("a{border:1px}",         min("a { border: 1px 1px 1px}"));
    Assert.assertEquals("a{border:1px}",         min("a { border: 1px 1px}"));
  }
  
  @Test public void testValueZero() {
    Assert.assertEquals("div{height:0}", min("div { height: 0px }"));
  }

  @Test public void testNamedColors() {
    Assert.assertEquals("a{color:#fff}", min("a { color: white}"));
    Assert.assertEquals("a{color:#000}", min("a { color: black}"));
    Assert.assertEquals("a{color:#777}", min("a { color: #777777}"));
  }
  
  @Test public void testContentProperty() {
    Assert.assertEquals("i::before{content:\" \"}", min("i::before { content: \" \" }"));
  }
  
  
  
  
  private final static String min(String css) {
    StringReader r = new StringReader(css);
    StringWriter w = new StringWriter();
    PrintWriter out = new PrintWriter(w);
    CSSMin.minimize(r, out);
    return w.toString().trim();
  }
}
