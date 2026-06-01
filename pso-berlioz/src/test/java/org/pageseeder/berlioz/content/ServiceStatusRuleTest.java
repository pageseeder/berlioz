/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.content;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.content.ServiceStatusRule.CodeRule;
import org.pageseeder.berlioz.content.ServiceStatusRule.SelectType;

public class ServiceStatusRuleTest {

  // --- validate ---

  @Test(expected = IllegalArgumentException.class)
  public void testValidate_empty() {
    ServiceStatusRule.validate("");
  }

  @Test
  public void testValidate_valid() {
    ServiceStatusRule.validate("1");
    ServiceStatusRule.validate("123");
    ServiceStatusRule.validate("abc");
    ServiceStatusRule.validate("123abc");
    ServiceStatusRule.validate("123-abc");
    ServiceStatusRule.validate("123_abc");
    ServiceStatusRule.validate("123.abc");
    ServiceStatusRule.validate("123:abc");
    ServiceStatusRule.validate("a:bc-d_1.23");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidate_invalid() {
    ServiceStatusRule.validate("&");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidate_space() {
    ServiceStatusRule.validate("a b");
  }

  // --- newInstance ---

  @Test
  public void testNewInstance_wildcardName() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", null);
    Assert.assertEquals(SelectType.NAME, rule.use());
    Assert.assertTrue(rule.items().isEmpty());
    Assert.assertEquals(CodeRule.HIGHEST, rule.rule());
  }

  @Test
  public void testNewInstance_wildcardTarget() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("target:*", null);
    Assert.assertEquals(SelectType.TARGET, rule.use());
    Assert.assertTrue(rule.items().isEmpty());
  }

  @Test
  public void testNewInstance_namedGenerators() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1,gen2", null);
    Assert.assertEquals(SelectType.NAME, rule.use());
    Assert.assertEquals(2, rule.items().size());
    Assert.assertTrue(rule.items().contains("gen1"));
    Assert.assertTrue(rule.items().contains("gen2"));
  }

  @Test
  public void testNewInstance_targetGenerators() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("target:main", null);
    Assert.assertEquals(SelectType.TARGET, rule.use());
    Assert.assertEquals(1, rule.items().size());
    Assert.assertEquals("main", rule.items().get(0));
  }

  @Test
  public void testNewInstance_codeRuleFirst() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", "FIRST");
    Assert.assertEquals(CodeRule.FIRST, rule.rule());
  }

  @Test
  public void testNewInstance_codeRuleLowest() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", "LOWEST");
    Assert.assertEquals(CodeRule.LOWEST, rule.rule());
  }

  @Test
  public void testNewInstance_codeRuleCaseInsensitive() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", "highest");
    Assert.assertEquals(CodeRule.HIGHEST, rule.rule());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNewInstance_invalidItem() {
    ServiceStatusRule.newInstance("name:bad item", null);
  }

  // --- appliesTo / appliesToAll ---

  @Test
  public void testAppliesToAll_emptyItems() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", null);
    Assert.assertTrue(rule.appliesToAll());
    Assert.assertTrue(rule.appliesTo("anything"));
    Assert.assertTrue(rule.appliesTo(null));
  }

  @Test
  public void testAppliesToAll_specificItems() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1", null);
    Assert.assertFalse(rule.appliesToAll());
  }

  @Test
  public void testAppliesTo_match() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1,gen2", null);
    Assert.assertTrue(rule.appliesTo("gen1"));
    Assert.assertTrue(rule.appliesTo("gen2"));
  }

  @Test
  public void testAppliesTo_noMatch() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1", null);
    Assert.assertFalse(rule.appliesTo("gen2"));
  }

  @Test
  public void testAppliesTo_null() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1", null);
    Assert.assertFalse(rule.appliesTo(null));
  }

  // --- DEFAULT_RULE ---

  @Test
  public void testDefaultRule() {
    ServiceStatusRule rule = ServiceStatusRule.DEFAULT_RULE;
    Assert.assertEquals(SelectType.NAME, rule.use());
    Assert.assertTrue(rule.items().isEmpty());
    Assert.assertEquals(CodeRule.HIGHEST, rule.rule());
    Assert.assertTrue(rule.appliesToAll());
  }

  // --- toString ---

  @Test
  public void testToString_wildcard() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", "HIGHEST");
    String s = rule.toString();
    Assert.assertNotNull(s);
    Assert.assertFalse(s.isEmpty());
  }
}
