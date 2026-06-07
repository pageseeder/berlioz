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
package org.pageseeder.berlioz.servlet;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceStatusRule.CodeRule;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accumulates generator-level outcomes (status, redirect, errors) across all generators in a
 * single service invocation. Shared by {@link XmlResponse} and {@link JsonResponse}.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.2
 */
final class GeneratorOutcome {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorOutcome.class);

  private @Nullable ContentStatus status = null;
  private @Nullable String redirect = null;
  private @Nullable BerliozException exception = null;

  ContentStatus getStatus() {
    ContentStatus s = this.status;
    return s == null ? ContentStatus.OK : s;
  }

  @Nullable BerliozException getError() {
    return this.exception;
  }

  @Nullable String getRedirectURL() {
    return this.redirect;
  }

  /**
   * Wraps {@code ex} in a {@link BerliozException}, accumulates it, logs a warning, and returns it.
   */
  BerliozException handleError(Exception ex, BerliozGenerator generator) {
    LOGGER.warn("Handling {} thrown by {}", ex.getClass().getName(), generator.getClass().getName());
    BerliozException bex = GeneratorDispatch.toBerliozException(ex);
    accumulateError(bex);
    return bex;
  }

  /**
   * Applies this generator's {@link Response} to the service-level outcome using the service's
   * {@link org.pageseeder.berlioz.content.ServiceStatusRule}. If the generator's status wins,
   * the redirect URL (if any) is also captured.
   */
  void handleStatus(Response response, BerliozGenerator generator, Service service) {
    if (!service.affectStatus(generator)) return;
    ContentStatus incoming = response.status();
    CodeRule rule = service.rule().rule();
    ContentStatus current = this.status;
    boolean update = current == null
        || (rule == CodeRule.HIGHEST && incoming.code() > current.code())
        || (rule == CodeRule.LOWEST  && incoming.code() < current.code());
    if (update) {
      this.status = incoming;
      if (response.isRedirect()) this.redirect = response.redirectLocation();
    }
  }

  private void accumulateError(BerliozException bex) {
    if (this.exception == null) {
      this.exception = bex;
    } else if (this.exception instanceof CompoundBerliozException) {
      collectCause(((CompoundBerliozException) this.exception).getCollector(), bex);
    } else {
      ErrorCollector<Throwable> collector = new ErrorCollector<>();
      BerliozException first = this.exception;
      this.exception = new CompoundBerliozException(
          "Multiple errors thrown by generators", BerliozErrorID.GENERATOR_ERROR_MULTIPLE, collector);
      collectCause(collector, first);
      collectCause(collector, bex);
    }
  }

  private static void collectCause(ErrorCollector<Throwable> collector, BerliozException bex) {
    Throwable cause = bex.getCause();
    collector.collectQuietly(Level.ERROR, cause != null ? cause : bex);
  }

}
