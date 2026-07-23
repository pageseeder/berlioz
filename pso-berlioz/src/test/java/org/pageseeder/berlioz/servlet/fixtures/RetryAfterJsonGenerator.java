package org.pageseeder.berlioz.servlet.fixtures;

import org.pageseeder.berlioz.content.JsonGenerator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.json.JsonWriter;

/** Always signals 503 with a {@code Retry-After} header, for error-header propagation tests. */
public final class RetryAfterJsonGenerator implements JsonGenerator {

  @Override
  public Response generate(Request req, JsonWriter json) {
    throw new HttpException("Service busy", 503) {}.header("Retry-After", "30");
  }
}
