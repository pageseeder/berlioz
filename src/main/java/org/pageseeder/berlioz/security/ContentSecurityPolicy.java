/*
 * Copyright (c) 1999-2023. Allette Systems Pty Ltd
 */
package org.pageseeder.berlioz.security;

import org.eclipse.jdt.annotation.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Class for immutable content security policy objects to help creating a Content-Security-Policy HTTP header.
 *
 * <p>The {@link #toString()} method returns a value that can be used in the
 * <code>Content-Security-Policy</code> or <code>Content-Security-Policy-Report-Only</code>
 * HTTP header.
 *
 * <p>Instances are immutable, use the {@link Builder} to create or update policies.</p>
 *
 * <p>NB. this implementation is backed by a map and does not support multiple instances of the same directive.</p>
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
  private final EnumMap<Directive, String> directives;

  private ContentSecurityPolicy(EnumMap<Directive, String> directives) {
    this.directives = directives;
  }

  /**
   * Returns the current source value of the policy directive.
   *
   * @param directive The directive
   * @return The corresponding source value or null
   */
  public @Nullable String get(Directive directive) {
    return this.directives.get(directive);
  }

  /**
   * @return a new security policy from the current security policy.
   */
  public Builder builder() {
    return new ContentSecurityPolicy.Builder(this);
  }

  public boolean isEmpty() {
    return this.directives.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ContentSecurityPolicy that = (ContentSecurityPolicy) o;
    return this.directives.equals(that.directives);
  }

  @Override
  public int hashCode() {
    return this.directives.hashCode();
  }

  /**
   * @return The value that can be used the header.
   */
  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    // EnumMaps return entries in Enum declaration order
    for (Map.Entry<Directive, String> e : this.directives.entrySet()) {
      if (out.length() > 0) out.append("; ");
      out.append(e.getKey());
      // `upgrade-insecure-requests` has no value
      if (!e.getValue().isEmpty()) {
        out.append(' ').append(e.getValue());
      }
    }
    return out.toString();
  }

  /**
   * Shorthand method to create a new policy with the updated policy directive.
   *
   * @param directive The policy directive
   * @param value The source value to set
   *
   * @return a new ContentSecurityPolicy instance.
   */
  public ContentSecurityPolicy withValue(Directive directive, String value) {
    return this.builder().set(directive, value).buildPrivate();
  }

  /**
   * Shorthand method to create a new policy the specified value add to the existing policy directive.
   *
   * <p>If the policy has no source value, it is set to the specified value.
   *
   * @param directive The policy directive to update
   * @param source The source value to add or set to new policy
   *
   * @return a new ContentSecurityPolicy instance.
   */
  public ContentSecurityPolicy withSource(Directive directive, String source) {
    return this.builder().add(directive, source).buildPrivate();
  }

  /**
   * Add the specified nonce to the policy directive.
   *
   * <p>If the policy has no source value, it is set to the specified value.
   *
   * @param directive The policy directive to update
   * @param nonce The source value to add or set to new policy.
   *
   * @return a new ContentSecurityPolicy instance.
   */
  public ContentSecurityPolicy withNonce(Directive directive, String nonce) {
    return this.builder().nonce(directive, nonce).buildPrivate();
  }

  /**
   * Shorthand method to create a new policy without the specified policy directive.
   *
   * @param directive The policy directive to remove from new policy.
   *
   * @return a new ContentSecurityPolicy instance.
   */
  public ContentSecurityPolicy without(Directive directive) {
    return this.builder().remove(directive).buildPrivate();
  }

  /**
   * A builder for content security policies.
   *
   * <p>The setter methods are chainable.
   *
   * <pre>{@code
   *  ContentSecurityPolicy policy = new ContentSecurityPolicy.Builder()
   *  .set(Directive.DEFAULT_SRC, "'self'")
   *  .set(Directive.FRAME_ANCESTORS, "'self'")
   *  .set(Directive.OBJECT_SRC, "'none'")
   *  .set(Directive.BASE_URI, "'none'")
   *  .set(Directive.SCRIPT_SRC, "'self' 'strict-dynamic'")
   *  .set(Directive.STYLE_SRC, "'self' 'unsafe-inline'")
   *  .build();
   * }</pre>
   *
   * <p>Implementation note: this class does not check that the source values are valid.
   *
   * @author Christophe Lauret
   * @version 0.12.6
   * @since 0.12.6
   */
  public static class Builder {

    /** Modifiable policy directives */
    private final EnumMap<Directive, String> directives;

    /**
     * Creates a new CSP builder.
     */
    public Builder() {
      this.directives = new EnumMap<>(Directive.class);
    }

    /**
     * Creates a new builder from an existing policy.
     *
     * @param policy The original policy.
     */
    public Builder(ContentSecurityPolicy policy) {
      this.directives = policy.directives.clone();
    }

    /**
     * Set the value of the specified policy directive.
     *
     * @param directive The policy directive
     * @param value The source value to set
     */
    public Builder set(Directive directive, String value) {
      this.directives.put(directive, value);
      return this;
    }

    /**
     * Add the specified value to the policy directive.
     *
     * <p>If the policy has no source value, it is set to the specified value.
     *
     * @param directive The policy directive
     * @param source The source value to add or set
     */
    public Builder add(Directive directive, String source) {
      String current = this.directives.get(directive);
      set(directive, current != null && !current.isEmpty() ? current+" "+source : source);
      return this;
    }

    /**
     * Add the specified nonce to the policy directive.
     *
     * <p>If the policy has no source value, it is set to the specified value.
     *
     * @param directive The policy directive
     * @param nonce The source value to add or set
     */
    public Builder nonce(Directive directive, String nonce) {
      return this.add(directive, "'nonce-"+nonce+"'");
    }

    /**
     * Remove the specified policy directive.
     *
     * @param directive The policy directive
     */
    public Builder remove(Directive directive) {
      this.directives.remove(directive);
      return this;
    }

    /**
     * Build the content security policy.
     *
     * @return a new ContentSecurity policy
     */
    public ContentSecurityPolicy build() {
      return new ContentSecurityPolicy(this.directives.clone());
    }

    /**
     * Build the content security policy.
     *
     * <p>Implementation note: keep this method private, to avoid cloning
     * the map, this method reuses
     *
     * @return a new ContentSecurity policy
     */
    private ContentSecurityPolicy buildPrivate() {
      return new ContentSecurityPolicy(this.directives);
    }

  }

}
