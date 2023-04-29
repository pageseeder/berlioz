/*
 * Copyright (c) 1999-2023. Allette Systems Pty Ltd
 */
package org.pageseeder.berlioz.security;

import org.eclipse.jdt.annotation.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Helper class to generate a Content-Security-Policy HTTP header.
 *
 * <p>The {@link #toString()} method returns a value that can be used in the
 * <code>Content-Security-Policy</code> or <code>Content-Security-Policy-Report-Only</code>
 * HTTP header.
 *
 * <p>Implementation note: this class does not check that the source values are valid.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy">MDN Content security policy</a>
 * @see <a href="https://web.dev/csp/">Web.dev: Content security policy</a>
 * @see <a href="https://web.dev/strict-csp/">Web.dev: Mitigate cross-site scripting (XSS) with a strict Content Security Policy (CSP)</a>
 * @see <a href="https://content-security-policy.com/">Content Security Policy Reference</a>
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html">OWASP Content Security Policy Cheat Sheet</a>
 *
 * @author Christophe Lauret
 * @version 0.12.6
 * @since 0.12.6
 */
public final class ContentSecurityPolicy {

  /**
   * Holds the mapping of policy directives to the source values.
   *
   * <p>Using an <code>EnumMap</code> ensures an efficient implementation with entries
   * returned in consistent order (the order of the policy directive enum class declarations)
   */
  private final EnumMap<PolicyDirective, String> directives;

  public ContentSecurityPolicy() {
    this.directives = new EnumMap<>(PolicyDirective.class);
  }

  ContentSecurityPolicy(EnumMap<PolicyDirective, String> directives) {
    this.directives = directives.clone();
  }

  /**
   * Creates a deep copy of the object so that they can be modified without changing the
   * original.
   *
   * @return a new instance with the same policy directive mappings in a new map.
   */
  public ContentSecurityPolicy copy() {
    return new ContentSecurityPolicy(this.directives);
  }

  /**
   * Returns the current source value of the policy directive.
   *
   * @param directive The policy directive
   * @return The corresponding source value or null
   */
  public @Nullable String get(PolicyDirective directive) {
    return this.directives.get(directive);
  }

  /**
   * Set the source value of the specified policy directive.
   *
   * @param directive The policy directive
   * @param value The source value to set
   */
  public void set(PolicyDirective directive, String value) {
    this.directives.put(directive, value);
  }

  /**
   * Add the specified value to the policy directive.
   *
   * <p>If the policy has no source value, it is set to the specified value.
   *
   * @param directive The policy directive
   * @param value The source value to add or set
   */
  public void add(PolicyDirective directive, String value) {
    String current = this.directives.get(directive);
    this.directives.put(directive, current != null && current.length() > 0 ? current+" "+value : value);
  }

  /**
   * Remove the specified policy directive.
   *
   * @param directive The policy directive
   */
  public void remove(PolicyDirective directive) {
    this.directives.remove(directive);
  }

  /**
   * @return The value that can be used the header.
   */
  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    // EnumMaps return entries in Enum declaration order
    for (Map.Entry<PolicyDirective, String> e : this.directives.entrySet()) {
      if (out.length() > 0) out.append("; ");
      out.append(e.getKey());
      // `upgrade-insecure-requests` has no value
      if (e.getValue().length() > 0) {
        out.append(' ').append(e.getValue());
      }
    }
    return out.toString();
  }
}
