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

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.error.ProblemDetails;
import org.pageseeder.berlioz.error.Problems;

/**
 * The framework error policy for a generator invocation.
 *
 * <p>Response classes keep their output assembly logic local, while this class keeps the shared
 * exception-to-response rules in one place.</p>
 *
 * @author Christophe Lauret
 * @version 0.13.5
 * @since 0.13.5
 */
final class GeneratorFailure {

  private final BerliozException error;
  private final Response response;
  private final DetailLevel detailLevel;

  private GeneratorFailure(BerliozException error, Response response, DetailLevel detailLevel) {
    this.error = error;
    this.response = response;
    this.detailLevel = detailLevel;
  }

  static GeneratorFailure handle(Exception ex, BerliozGenerator generator, GeneratorOutcome outcome) {
    DetailLevel level = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL));
    BerliozException error = outcome.handleError(ex, generator);
    Response response = toResponse(ex, level);
    return new GeneratorFailure(error, response, level);
  }

  BerliozException error() {
    return this.error;
  }

  Response response() {
    return this.response;
  }

  DetailLevel detailLevel() {
    return this.detailLevel;
  }

  private static Response toResponse(Exception ex, DetailLevel level) {
    if (GeneratorDispatch.useProblemFormat()) {
      return Response.problem(toProblem(ex, level));
    }
    return Response.status(toLegacyStatus(ex));
  }

  private static ProblemDetails toProblem(Exception ex, DetailLevel level) {
    if (ex instanceof HttpException) {
      return Problems.forHttpException((HttpException) ex, level);
    }
    return Problems.forGeneratorError(ex, level);
  }

  private static ContentStatus toLegacyStatus(Exception ex) {
    if (ex instanceof HttpException) {
      return statusFor((HttpException) ex);
    }
    return ContentStatus.INTERNAL_SERVER_ERROR;
  }

  private static ContentStatus statusFor(HttpException ex) {
    ContentStatus status = ContentStatus.forCode(ex.getHttpCode());
    if (status != null) {
      return status;
    }
    return ContentStatus.INTERNAL_SERVER_ERROR;
  }

}
