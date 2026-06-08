package org.pageseeder.berlioz.content;

/**
 * Package-private access bridge: exposes {@link Service.Builder} to tests outside
 * the {@code content} package.
 */
public final class ServiceTestHelper {

  private ServiceTestHelper() {}

  public static Service build(String id, ServiceStatusRule rule, BerliozGenerator... generators) {
    Service.Builder b = new Service.Builder().id(id).group("g").rule(rule);
    for (BerliozGenerator gen : generators) b.add(gen);
    return b.build();
  }

  public static Service buildDirect(String id, ServiceStatusRule rule, BerliozGenerator... generators) {
    Service.Builder b = new Service.Builder().id(id).group("g").rule(rule).direct(true);
    for (BerliozGenerator gen : generators) b.add(gen);
    return b.build();
  }

  public static ServiceStatusRule highestRule() {
    return ServiceStatusRule.DEFAULT_RULE;
  }

  public static ServiceStatusRule lowestRule() {
    return ServiceStatusRule.newInstance("*", "LOWEST");
  }

  public static ServiceStatusRule namedRule(String generatorName) {
    return ServiceStatusRule.newInstance("name:" + generatorName, "HIGHEST");
  }
}
