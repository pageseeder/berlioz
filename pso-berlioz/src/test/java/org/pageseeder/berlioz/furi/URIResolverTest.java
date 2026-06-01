/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.furi;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.furi.URIResolver.MatchRule;

/**
 * A test class for the <code>URIResolver</code>.
 *
 * @author Christophe Lauret
 * @version 27 May 2009
 */
class URIResolverTest {

  /**
   * Test the <code>find</code> method.
   */
  @Test
  void testFind() {
    URIResolver resolver = new URIResolver("/group/1892/home");
    List<URIPattern> patterns = new ArrayList<URIPattern>();
    patterns.add(new URIPattern("/group/{groupid}/list"));
    patterns.add(new URIPattern("/group/{groupid}/home"));
    patterns.add(new URIPattern("/group/{groupid}/add"));
    Assertions.assertEquals(new URIPattern("/group/{groupid}/home"), resolver.find(patterns));
  }

  /**
   * Test the <code>find</code> method.
   */
  @Test
  void testFind_First() {
    URIResolver resolver = new URIResolver("/document/history/dir/doc.xml");
    List<URIPattern> patterns = new ArrayList<URIPattern>();
    patterns.add(new URIPattern("/document/{+document}"));
    patterns.add(new URIPattern("/document/history/{+document}"));
    patterns.add(new URIPattern("/{+document}"));
    Assertions.assertEquals(new URIPattern("/document/{+document}"), resolver.find(patterns, MatchRule.FIRST_MATCH));
  }

  /**
   * Test the <code>find</code> method.
   */
  @Test
  void testFind_Best() {
    URIResolver resolver = new URIResolver("/document/history/dir/doc.xml");
    List<URIPattern> patterns = new ArrayList<URIPattern>();
    patterns.add(new URIPattern("/document/{+document}"));
    patterns.add(new URIPattern("/document/history/{+document}"));
    patterns.add(new URIPattern("/{+document}"));
    Assertions.assertEquals(new URIPattern("/document/history/{+document}"), resolver.find(patterns, MatchRule.BEST_MATCH));
  }

  /**
   * Test the <code>resolve</code> method with some int values.
   */
  @Test
  void testResolve_Int() {
    URIResolver resolver = new URIResolver("/group/1892/home");
    URIPattern p = new URIPattern("/group/{groupid}/home");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("1892", r.get("groupid"));
  }

  /**
   * Test the <code>resolve</code> method with some int values.
   */
  @Test
  void testResolve_IntTyped() {
    URIResolver resolver = new URIResolver("/group/1892/home");
    URIPattern p = new URIPattern("/group/{int:groupid}/home");
    Assertions.assertTrue(p.match(resolver.uri()));
    VariableBinder b = new VariableBinder();
    b.bindType("int", new VariableResolver(){
      @Override
      public boolean exists(String v) {return v.matches("\\d+");}
      @Override
      public Integer resolve(String v) {return exists(v)? Integer.valueOf(v) : null;}
    });
    URIResolveResult r = resolver.resolve(p, b);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals(1892, r.get("groupid"));
  }

  /**
   * Test the <code>resolve</code> method with some String values.
   */
  @Test
  void testResolve_String() {
    URIResolver resolver = new URIResolver("/user/~clauret/home");
    URIPattern p = new URIPattern("/user/{account}/home");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("~clauret", r.get("account"));
  }

  /**
   * Test the <code>resolve</code> method with some escaped values.
   */
  @Test
  void testResolve_Escape() {
    URIResolver resolver = new URIResolver("/tag/Caf%C3%A9");
    URIPattern p = new URIPattern("/tag/{tag}");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("Caf\u00e9", r.get("tag"));
  }

  /**
   * Test the <code>resolve</code> method with some multiple values.
   */
  @Test
  void testResolve_Multiple() {
    URIResolver resolver = new URIResolver("http://acme.com/dev/clauret");
    URIPattern p = new URIPattern("{scheme}://{domain}/{group}/{user}");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("http", r.get("scheme"));
    Assertions.assertEquals("acme.com", r.get("domain"));
    Assertions.assertEquals("dev", r.get("group"));
    Assertions.assertEquals("clauret", r.get("user"));
  }

  /**
   * Test the <code>resolve</code> method with some multiple values.
   */
  @Test
  void testResolve_Multiple2() {
    URIResolver resolver = new URIResolver("/documents;label=technical;version=1.0");
    URIPattern p = new URIPattern("/documents;label={label};version={version}");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("technical", r.get("label"));
    Assertions.assertEquals("1.0", r.get("version"));
  }

  /**
   * Test the <code>resolve</code> method with some multiple values.
   */
  @Test
  void testResolve_OperatorPathParam1Var() {
    URIResolver resolver = new URIResolver("/documents;label=technical");
    URIPattern p = new URIPattern("/documents{;label}");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("technical", r.get("label"));
  }

  /**
   * Test the <code>resolve</code> method with some multiple values.
   */
  @Test
  void testResolve_OperatorPathParamNVar() {
    URIResolver resolver = new URIResolver("/documents;label=technical;version=1.0");
    URIPattern p = new URIPattern("/documents{;label,version}");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("technical", r.get("label"));
    Assertions.assertEquals("1.0", r.get("version"));
  }

  /**
   * Test the <code>resolve</code> method with some objects values.
   */
  @Test
  void testResolve_Objects() {
    URIResolver resolver = new URIResolver("/documents;label=technical;version=1.0");
    URIPattern p = new URIPattern("/documents;label={label};version={version}");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("technical", r.get("label"));
    Assertions.assertEquals("1.0", r.get("version"));
  }

  /**
   * Test the <code>resolve</code> method with some objects values.
   */
  @Test
  void testResolve_URIInsert() {
    URIResolver resolver = new URIResolver("/path/dir/subdir/document.xml");
    URIPattern p = new URIPattern("/path/{+path}");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("dir/subdir/document.xml", r.get("path"));
  }

  /**
   * Test the <code>resolve</code> method with some objects values.
   */
  @Test
  void testResolve_URIInsert2() {
    URIResolver resolver = new URIResolver("/path/dir/subdir/document.xml/comments");
    URIPattern p = new URIPattern("/path/{+path}/comments");
    Assertions.assertTrue(p.match(resolver.uri()));
    URIResolveResult r = resolver.resolve(p);
    Assertions.assertEquals(URIResolveResult.Status.RESOLVED, r.getStatus());
    Assertions.assertEquals("dir/subdir/document.xml", r.get("path"));
  }

  @Test
  void testSample() {
    // setting up the patterns when parsing the configuration
    List<URIPattern> patterns = new ArrayList<URIPattern>();
    patterns.add(new URIPattern("/home"));
    patterns.add(new URIPattern("/path/{+path}"));
    patterns.add(new URIPattern("/documents{;label}"));
    patterns.add(new URIPattern("/document/*"));

    // test case #0
    URIResolver resolver0 = new URIResolver("/home");
    URIPattern pattern0 = resolver0.find(patterns);
    Assertions.assertEquals("/home", pattern0.toString());

    // test case #1
    URIResolver resolver1 = new URIResolver("/path/dir/subdir/doc.xml");
    URIPattern pattern1 = resolver1.find(patterns);
    ResolvedVariables result1 = resolver1.resolve(pattern1);
    String doc = (String)result1.get("path");
    Assertions.assertEquals("dir/subdir/doc.xml", doc);

    // test case #2
    URIResolver resolver2 = new URIResolver("/documents;label=important");
    URIPattern pattern2 = resolver2.find(patterns);
    ResolvedVariables result2 = resolver2.resolve(pattern2);
    String name = (String)result2.get("label");
    Assertions.assertEquals("important", name);

    // test case #3
    URIResolver resolver3 = new URIResolver("/document/doc.xml");
    URIPattern pattern3 = resolver3.find(patterns);
    ResolvedVariables result3 = resolver3.resolve(pattern3);
    String wildcard = (String)result3.get("*");
    Assertions.assertEquals("doc.xml", wildcard);

  }

}
