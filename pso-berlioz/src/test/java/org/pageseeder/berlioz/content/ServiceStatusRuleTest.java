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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ServiceStatusRule.CodeRule;
import org.pageseeder.berlioz.content.ServiceStatusRule.SelectType;

public class ServiceStatusRuleTest {

  // --- validate ---

  @Test
  public void testValidate_empty() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceStatusRule.validate(""));
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

  @Test
  public void testValidate_invalid() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceStatusRule.validate("&"));
  }

  @Test
  public void testValidate_space() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceStatusRule.validate("a b"));
  }

  // --- newInstance ---

  @Test
  public void testNewInstance_wildcardName() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", null);
    Assertions.assertEquals(SelectType.NAME, rule.use());
    Assertions.assertTrue(rule.items().isEmpty());
    Assertions.assertEquals(CodeRule.HIGHEST, rule.rule());
  }

  @Test
  public void testNewInstance_wildcardTarget() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("target:*", null);
    Assertions.assertEquals(SelectType.TARGET, rule.use());
    Assertions.assertTrue(rule.items().isEmpty());
  }

  @Test
  public void testNewInstance_namedGenerators() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1,gen2", null);
    Assertions.assertEquals(SelectType.NAME, rule.use());
    Assertions.assertEquals(2, rule.items().size());
    Assertions.assertTrue(rule.items().contains("gen1"));
    Assertions.assertTrue(rule.items().contains("gen2"));
  }

  @Test
  public void testNewInstance_targetGenerators() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("target:main", null);
    Assertions.assertEquals(SelectType.TARGET, rule.use());
    Assertions.assertEquals(1, rule.items().size());
    Assertions.assertEquals(rule.items().get(0), "main");
  }

  @Test
  public void testNewInstance_codeRuleFirst() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", "FIRST");
    Assertions.assertEquals(CodeRule.FIRST, rule.rule());
  }

  @Test
  public void testNewInstance_codeRuleLowest() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", "LOWEST");
    Assertions.assertEquals(CodeRule.LOWEST, rule.rule());
  }

  @Test
  public void testNewInstance_codeRuleCaseInsensitive() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", "highest");
    Assertions.assertEquals(CodeRule.HIGHEST, rule.rule());
  }

  @Test
  public void testNewInstance_invalidItem() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceStatusRule.newInstance("name:bad item", null));
  }

  // --- appliesTo / appliesToAll ---

  @Test
  public void testAppliesToAll_emptyItems() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", null);
    Assertions.assertTrue(rule.appliesToAll());
    Assertions.assertTrue(rule.appliesTo("anything"));
    Assertions.assertTrue(rule.appliesTo(null));
  }

  @Test
  public void testAppliesToAll_specificItems() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1", null);
    Assertions.assertFalse(rule.appliesToAll());
  }

  @Test
  public void testAppliesTo_match() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1,gen2", null);
    Assertions.assertTrue(rule.appliesTo("gen1"));
    Assertions.assertTrue(rule.appliesTo("gen2"));
  }

  @Test
  public void testAppliesTo_noMatch() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1", null);
    Assertions.assertFalse(rule.appliesTo("gen2"));
  }

  @Test
  public void testAppliesTo_null() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:gen1", null);
    Assertions.assertFalse(rule.appliesTo(null));
  }

  // --- DEFAULT_RULE ---

  @Test
  public void testDefaultRule() {
    ServiceStatusRule rule = ServiceStatusRule.DEFAULT_RULE;
    Assertions.assertEquals(SelectType.NAME, rule.use());
    Assertions.assertTrue(rule.items().isEmpty());
    Assertions.assertEquals(CodeRule.HIGHEST, rule.rule());
    Assertions.assertTrue(rule.appliesToAll());
  }

  // --- toString ---

  @Test
  public void testToString_wildcard() {
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:*", "HIGHEST");
    String s = rule.toString();
    Assertions.assertNotNull(s);
    Assertions.assertFalse(s.isEmpty());
  }
}
