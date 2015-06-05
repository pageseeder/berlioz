/*
 * This file is part of the URI Template library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.weborganic.berlioz.furi.URIResolver.MatchRule;

/**
 * A test class for the <code>URIResolver</code>.
 *
 * @author Christophe Lauret
 * @version 27 May 2009
 */
public class URIResolverTest {

  /**
   * Test the <code>find</code> method.
   */
  @Test
  public void testFind() {
    URIResolver resolver = new URIResolver("/group/1892/home");
    List<URIPattern> patterns = new ArrayList<URIPattern>();
    patterns.add(new URIPattern("/group/{groupid}/list"));
    patterns.add(new URIPattern("/group/{groupid}/home"));
    patterns.add(new URIPattern("/group/{groupid}/add"));
    Assert.assertEquals(new URIPattern("/group/{groupid}/home"), resolver.find(patterns));
  }

  /**
   * Test the <code>find</code> method.
   */
  @Test
  public void testFind_First() {
    URIResolver resolver = new URIResolver("/document/history/dir/doc.xml");
    List<URIPattern> patterns = new ArrayList<URIPattern>();
    patterns.add(new URIPattern("/document/{+document}"));
    patterns.add(new URIPattern("/document/history/{+document}"));
    patterns.add(new URIPattern("/{+document}"));
    Assert.assertEquals(new URIPattern("/document/{+document}"), resolver.find(patterns, MatchRule.FIRST_MATCH));
  }

  /**
   * Test the <code>find</code> method.
   */
  @Test
  public void testFind_Best() {
    URIResolver resolver = new URIResolver("/document/history/dir/doc.xml");
    List<URIPattern> patterns = new ArrayList<URIPattern>();
    patterns.add(new URIPattern("/document/{+document}"));
    patterns.add(new URIPattern("/document/history/{+document}"));
    patterns.add(new URIPattern("/{+document}"));
    Assert.assertEquals(new URIPattern("/document/history/{+document}"), resolver.find(patterns, MatchRule.BEST_MATCH));
  }

  /**
   * Test the <code>resolve</code> method with some int values.
   */
  @Test
  public void testResolve_Int() {
    URIResolver resolver = new URIResolver("/group/1892/home");
    URIPattern p = new URIPattern("/group/{groupid}/home");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("1892", r.get("groupid"));
  }

  /**
   * Test the <code>resolve</code> method with some int values.
   */
  @Test
  public void testResolve_IntTyped() {
    URIResolver resolver = new URIResolver("/group/1892/home");
    URIPattern p = new URIPattern("/group/{int:groupid}/home");
    Assert.assertTrue(p.match(resolver.uri()));
    VariableBinder b = new VariableBinder();
    b.bindType("int", new VariableResolver(){
      @Override
      public boolean exists(String v) {return v.matches("\\d+");}
      @Override
      public Integer resolve(String v) {return exists(v)? Integer.valueOf(v) : null;};
    });
    URIResolveResult r = resolver.resolve(p, b);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals(1892, r.get("groupid"));
  }

  /**
   * Test the <code>resolve</code> method with some String values.
   */
  @Test
  public void testResolve_String() {
    URIResolver resolver = new URIResolver("/user/~clauret/home");
    URIPattern p = new URIPattern("/user/{account}/home");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("~clauret", r.get("account"));
  }

  /**
   * Test the <code>resolve</code> method with some escaped values.
   */
  @Test
  public void testResolve_Escape() {
    URIResolver resolver = new URIResolver("/tag/Caf%C3%A9");
    URIPattern p = new URIPattern("/tag/{tag}");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("Caf\u00e9", r.get("tag"));
  }

  /**
   * Test the <code>resolve</code> method with some multiple values.
   */
  @Test
  public void testResolve_Multiple() {
    URIResolver resolver = new URIResolver("http://acme.com/dev/clauret");
    URIPattern p = new URIPattern("{scheme}://{domain}/{group}/{user}");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("http",     r.get("scheme"));
    Assert.assertEquals("acme.com", r.get("domain"));
    Assert.assertEquals("dev",      r.get("group"));
    Assert.assertEquals("clauret",  r.get("user"));
  }

  /**
   * Test the <code>resolve</code> method with some multiple values.
   */
  @Test
  public void testResolve_Multiple2() {
    URIResolver resolver = new URIResolver("/documents;label=technical;version=1.0");
    URIPattern p = new URIPattern("/documents;label={label};version={version}");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("technical", r.get("label"));
    Assert.assertEquals("1.0",       r.get("version"));
  }

  /**
   * Test the <code>resolve</code> method with some multiple values.
   */
  @Test
  public void testResolve_OperatorPathParam1Var() {
    URIResolver resolver = new URIResolver("/documents;label=technical");
    URIPattern p = new URIPattern("/documents{;label}");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("technical", r.get("label"));
  }

  /**
   * Test the <code>resolve</code> method with some multiple values.
   */
  @Test
  public void testResolve_OperatorPathParamNVar() {
    URIResolver resolver = new URIResolver("/documents;label=technical;version=1.0");
    URIPattern p = new URIPattern("/documents{;label,version}");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("technical", r.get("label"));
    Assert.assertEquals("1.0",       r.get("version"));
  }

  /**
   * Test the <code>resolve</code> method with some objects values.
   */
  @Test
  public void testResolve_Objects() {
    URIResolver resolver = new URIResolver("/documents;label=technical;version=1.0");
    URIPattern p = new URIPattern("/documents;label={label};version={version}");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("technical", r.get("label"));
    Assert.assertEquals("1.0",       r.get("version"));
  }

  /**
   * Test the <code>resolve</code> method with some objects values.
   */
  @Test
  public void testResolve_URIInsert() {
    URIResolver resolver = new URIResolver("/path/dir/subdir/document.xml");
    URIPattern p = new URIPattern("/path/{+path}");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("dir/subdir/document.xml", r.get("path"));
  }

  /**
   * Test the <code>resolve</code> method with some objects values.
   */
  @Test
  public void testResolve_URIInsert2() {
    URIResolver resolver = new URIResolver("/path/dir/subdir/document.xml/comments");
    URIPattern p = new URIPattern("/path/{+path}/comments");
    Assert.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assert.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assert.assertEquals("dir/subdir/document.xml", r.get("path"));
  }

  @Test
  public void testSample() {
    // setting up the patterns when parsing the configuration
    List<URIPattern> patterns = new ArrayList<URIPattern>();
    patterns.add(new URIPattern("/home"));
    patterns.add(new URIPattern("/path/{+path}"));
    patterns.add(new URIPattern("/documents{;label}"));
    patterns.add(new URIPattern("/document/*"));

    // test case #0
    URIResolver resolver0 = new URIResolver("/home");
    URIPattern pattern0 = resolver0.find(patterns);
    Assert.assertEquals("/home", pattern0.toString());

    // test case #1
    URIResolver resolver1 = new URIResolver("/path/dir/subdir/doc.xml");
    URIPattern pattern1 = resolver1.find(patterns);
    ResolvedVariables result1 = resolver1.resolve(pattern1);
    String doc = (String)result1.get("path");
    Assert.assertEquals("dir/subdir/doc.xml", doc);

    // test case #2
    URIResolver resolver2 = new URIResolver("/documents;label=important");
    URIPattern pattern2 = resolver2.find(patterns);
    ResolvedVariables result2 = resolver2.resolve(pattern2);
    String name = (String)result2.get("label");
    Assert.assertEquals("important", name);

    // test case #3
    URIResolver resolver3 = new URIResolver("/document/doc.xml");
    URIPattern pattern3 = resolver3.find(patterns);
    ResolvedVariables result3 = resolver3.resolve(pattern3);
    String wildcard = (String)result3.get("*");
    Assert.assertEquals("doc.xml", wildcard);

  }

}
