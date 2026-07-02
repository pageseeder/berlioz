package org.pageseeder.berlioz.sample.support;

import org.pageseeder.berlioz.content.ParameterSpec;

/**
 * Shared request parameter definitions for the sample application.
 *
 * <p>Berlioz parameter specs are useful when more than one generator needs the same validation
 * semantics, but they are also helpful in small samples because they keep validation policy away
 * from endpoint behavior. When validation fails, Berlioz converts the parameter error into a
 * client error response.</p>
 */
public final class AppParameters {

  private AppParameters() {}

  /**
   * Text accepted by the note update endpoint.
   *
   * <p>The limit is intentionally tiny so it is easy to trigger and inspect validation failures
   * while manually testing the sample API.</p>
   */
  public static final ParameterSpec<String> TEXT =
      ParameterSpec.of("text",   p -> p.asString()
          .matching(s -> s.length() < 100, "must not exceed 100 characters")
          .required());

}
