package org.pageseeder.berlioz.content;

import org.junit.Test;
import org.pageseeder.berlioz.BerliozException;

public class ServiceStatusRuleTest {


  @Test(expected = IllegalArgumentException.class)
  public void testValidate_empty() throws BerliozException {
    ServiceStatusRule.validate("");
  }

  @Test
  public void testValidate_valid() throws BerliozException {
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
  public void testValidate_invalid() throws BerliozException {
    ServiceStatusRule.validate("&");
  }

}
