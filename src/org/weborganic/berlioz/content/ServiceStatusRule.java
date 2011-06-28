/*
 * This file is part of the Berlioz library.
 * 
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import org.weborganic.berlioz.Beta;

/**
 * Defines the business logic to calculating the status code of a service.
 * 
 * @author Christophe Lauret
 * @version 28 June 2011
 */
@Beta public final class ServiceStatusRule {

  /**
   * How is the status code for the determined.
   */
  @Beta
  protected enum CodeRule {

    /** The first selected generator. */
    FIRST,

    /** The highest code by a selected generator. */
    HIGHEST,

    /** The highest code by a selected generator. */
    LOWEST
  };

  /**
   * How the generator should be selected.
   */
  @Beta
  protected enum SelectType {

    /** Any generator. */
    ANY,

    /** By target. */
    TARGET,

    /** By name. */
    NAME
  };

  /**
   * The default rule to use when none is specified.
   */
  protected static final ServiceStatusRule DEFAULT_RULE = 
    new ServiceStatusRule(SelectType.ANY, new String[]{}, CodeRule.HIGHEST);

  /**
   * 
   */
  private final SelectType _use;

  /**
   * The list of generator names or targets.
   */
  private final String[] _select;

  /**
   * The code rule.
   */
  private final CodeRule _rule;

  /**
   * Create a new rule.
   * 
   * @param use    How the generator should be selected.
   * @param select The names or targets of the generators to select.
   * @param rule   How is the status code for the determined.
   */
  public ServiceStatusRule(SelectType use, String[] select, CodeRule rule) {
    this._use = use;
    this._select = select;
    this._rule = rule;
  }

  /**
   * @return How the generator should be selected.
   */
  public SelectType use() {
    return this._use;
  }

  /**
   * @return The names or targets of the generators to select.
   */
  public String[] select() {
    return this._select;
  }

  /**
   * @return How is the status code for the determined.
   */
  public CodeRule rule() {
    return this._rule;
  }

  @Override
  public String toString() {
    return this._use+" "+this._select+" "+_rule;
  }

  /**
   * Create a new rule instance.
   * 
   * @param use  the use definition
   * @param rule the code rule.
   * @return the corresponding rule.
   */
  public static ServiceStatusRule newInstance(String use, String rule) {
    CodeRule r = rule != null? CodeRule.valueOf(rule.toUpperCase()) : CodeRule.HIGHEST;
    if ("*".equals(use)) {
      return new ServiceStatusRule(SelectType.ANY, new String[]{}, r);
    } else if (use.startsWith("name:")) {
      String[] select = use.substring(5).split(",");
      return new ServiceStatusRule(SelectType.NAME, select, r);
    } else if (use.startsWith("target:")) {
      String[] select = use.substring(7).split(",");
      return new ServiceStatusRule(SelectType.TARGET, select, r);
    } else {
      return new ServiceStatusRule(SelectType.ANY, new String[]{}, r);
    }
  }

}
