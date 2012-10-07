/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release. A copy of this licence can also be
 * found at http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.weborganic.berlioz.Beta;

/**
 * Defines the business logic to calculating the status code of a service.
 *
 * <p>All class attributes are immutable and have a value (they are never <code>null</code>).
 *
 * @author Christophe Lauret
 * @version 1 July 2011
 */
@Beta public final class ServiceStatusRule {

  /**
   * The "name:" selector prefix
   */
  private static final String NAME_SELECTOR_PREFIX = "name:";

  /**
   * The "target:" selector prefix
   */
  private static final String TARGET_SELECTOR_PREFIX = "target:";

  /**
   * How is the status code for the determined.
   */
  @Beta
  public enum CodeRule {

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
  public enum SelectType {

    /** By target. */
    TARGET,

    /** By name. */
    NAME
  };

  /**
   * The default rule to use when none is specified.
   */
  protected static final ServiceStatusRule DEFAULT_RULE;
  static {
    // Required for type safety
    List<String> empty = Collections.emptyList();
    DEFAULT_RULE = new ServiceStatusRule(SelectType.NAME, empty, CodeRule.HIGHEST);
  }

  /**
   * How the generator should be selected.
   */
  private final SelectType _use;

  /**
   * The list of generator names or targets (depending on type)
   */
  private final List<String> _items;

  /**
   * The code rule.
   */
  private final CodeRule _rule;

  /**
   * Create a new rule.
   *
   * @param use   How the generator should be selected.
   * @param items The names or targets of the generators to select.
   * @param rule  How is the status code for the determined.
   */
  protected ServiceStatusRule(SelectType use, List<String> items, CodeRule rule) {
    if (use == null)   throw new NullPointerException("Argument use is null.");
    if (items == null) throw new NullPointerException("Argument items is null.");
    if (rule == null)  throw new NullPointerException("Argument rule is null.");
    this._use = use;
    this._items = items;
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
  public List<String> items() {
    return this._items;
  }

  /**
   * Indicates whether this rule applies to the generator name or target.
   * @param  nameOrTarget The name or target of the generator.
   * @return <code>true</code> if this rule applies to all the generators;
   *         <code>false</code> otherwise.
   */
  public boolean appliesTo(String nameOrTarget) {
    if (this._items.isEmpty()) return true;
    if (nameOrTarget == null) return false;
    return this._items.contains(nameOrTarget);
  }

  /**
   * Indicates whether this rule applies to all the generators.
   * @return <code>true</code> if this rule applies to all the generators;
   *         <code>false</code> otherwise.
   */
  public boolean appliesToAll() {
    return this._items.isEmpty();
  }

  /**
   * @return How is the status code for the determined.
   */
  public CodeRule rule() {
    return this._rule;
  }

  @Override
  public String toString() {
    return this._use+":"+(this._items.isEmpty()? "*" : this._items)+" "+this._rule;
  }

  /**
   * Create a new rule instance.
   *
   * @param use  the use definition
   * @param rule the code rule.
   *
   * @return the corresponding rule.
   *
   * @throws NullPointerException     If the use parameter is <code>null</code>.
   * @throws IllegalArgumentException If either argument is invalid.
   */
  public static ServiceStatusRule newInstance(String use, String rule) {
    if (use == null) throw new NullPointerException("Argument use is null.");
    // Default rule to HIGHEST (if unspecified)
    CodeRule r = rule != null? CodeRule.valueOf(rule.toUpperCase()) : CodeRule.HIGHEST;
    // Select type default to NAME
    SelectType t = use.startsWith(TARGET_SELECTOR_PREFIX)? SelectType.TARGET : SelectType.NAME;
    // Now get the list of items if any
    String items = use;
    if (items.startsWith(NAME_SELECTOR_PREFIX)) {
      items = items.substring(NAME_SELECTOR_PREFIX.length());
    } else if (items.startsWith(TARGET_SELECTOR_PREFIX)) {
      items = items.substring(TARGET_SELECTOR_PREFIX.length());
    }
    List<String> list = null;
    if ("*".equals(items)) {
      list = Collections.emptyList();
    } else {
      list = Arrays.asList(items.split(","));
      for (String i : list) { validate(i); }
    }
    return new ServiceStatusRule(t, Collections.unmodifiableList(list), r);
  }

  /**
   * Validates the item (throws an exception if invalid)
   *
   * @param item the use definition
   *
   * @throws IllegalArgumentException If either argument is invalid.
   */
  private static void validate(String item) {
    if (item.isEmpty()) throw new IllegalArgumentException("Named item is empty");
    char c;
    for (int i = 0; i < item.length(); i++) {
      c = item.charAt(i);
      if (Character.isLetter(c)) {
        continue;
      } else if (Character.isDigit(c)) {
        continue;
      } else if (c == '-') {
        continue;
      } else if (c == '_') {
        continue;
      } else if (c == '.') {
        continue;
      } else if (c == ':') {
        continue;
      } else throw new IllegalArgumentException("Item \""+item+"\" contains an illegal character '"+c+"'");
    }
  }

}
