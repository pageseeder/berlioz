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
package org.pageseeder.berlioz.error;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ProblemsTest {

  // --- forInvalidParameter() ---

  @Test
  void testForInvalidParameter_required() {
    InvalidParameterException ex = InvalidParameterException.required("username");
    ProblemDetails p = Problems.forInvalidParameter(ex);
    Assertions.assertEquals(400, p.status());
    Assertions.assertEquals("urn:berlioz:problem:invalid-parameter", p.type());
    Assertions.assertEquals("Invalid Request Parameter", p.title());
    Assertions.assertEquals("username", p.extensions().get("parameter"));
    Assertions.assertEquals("required", p.extensions().get("reason"));
  }

  @Test
  void testForInvalidParameter_invalidFormat() {
    InvalidParameterException ex = InvalidParameterException.invalidFormat("age", "abc", "integer");
    ProblemDetails p = Problems.forInvalidParameter(ex);
    Assertions.assertEquals("invalid-format", p.extensions().get("reason"));
  }

  @Test
  void testForInvalidParameter_outOfRange() {
    InvalidParameterException ex = InvalidParameterException.outOfRange("page", "-1", "must be >= 1");
    ProblemDetails p = Problems.forInvalidParameter(ex);
    Assertions.assertEquals("out-of-range", p.extensions().get("reason"));
  }

  @Test
  void testForInvalidParameter_notAllowed() {
    InvalidParameterException ex = InvalidParameterException.notAllowed("sort", "random", "asc", "desc");
    ProblemDetails p = Problems.forInvalidParameter(ex);
    Assertions.assertEquals("not-allowed", p.extensions().get("reason"));
  }

  @Test
  void testForInvalidParameter_withDetailAddsException() {
    InvalidParameterException ex = InvalidParameterException.required("username");
    ProblemDetails p = Problems.forInvalidParameter(ex, DetailLevel.STANDARD);
    Assertions.assertTrue(p.extensions().containsKey("exception"));
  }

  // --- forUpstreamException() ---

  @Test
  void testForUpstreamException_withMessage() {
    UpstreamException ex = new UpstreamException("connection refused");
    ProblemDetails p = Problems.forUpstreamException(ex);
    Assertions.assertEquals(502, p.status());
    Assertions.assertEquals("urn:berlioz:problem:upstream-error", p.type());
    Assertions.assertEquals("Upstream Service Error", p.title());
    Assertions.assertEquals("connection refused", p.detail());
    Assertions.assertNull(p.extensions().get("upstream-service"));
  }

  @Test
  void testForUpstreamException_withNamedService() {
    UpstreamException ex = new UpstreamException("timeout", "search-api");
    ProblemDetails p = Problems.forUpstreamException(ex);
    Assertions.assertEquals("search-api", p.extensions().get("upstream-service"));
  }

  @Test
  void testForUpstreamException_withDetailAddsException() {
    UpstreamException ex = new UpstreamException("timeout", "search-api");
    ProblemDetails p = Problems.forUpstreamException(ex, DetailLevel.FULL);
    Assertions.assertTrue(p.extensions().containsKey("exception"));
  }

  // --- forHttpException() ---

  @Test
  void testForHttpException() {
    HttpException ex = new HttpException("legal hold", 451) {};
    ProblemDetails p = Problems.forHttpException(ex);
    Assertions.assertEquals(451, p.status());
    Assertions.assertEquals("urn:berlioz:problem:http-signal", p.type());
    Assertions.assertEquals("Unavailable For Legal Reasons", p.title());
    Assertions.assertEquals("legal hold", p.detail());
  }

  @Test
  void testForHttpException_withDetailAddsException() {
    HttpException ex = new HttpException("legal hold", 451) {};
    ProblemDetails p = Problems.forHttpException(ex, DetailLevel.STANDARD);
    Assertions.assertTrue(p.extensions().containsKey("exception"));
  }

  // --- forGeneratorError() ---

  @Test
  void testForGeneratorError() {
    ProblemDetails p = Problems.forGeneratorError();
    Assertions.assertEquals(500, p.status());
    Assertions.assertEquals("urn:berlioz:problem:generator-error", p.type());
    Assertions.assertEquals("Internal Server Error", p.title());
    Assertions.assertNull(p.detail());
  }

  // --- forHttpError() ---

  @Test
  void testForHttpError_notFound() {
    ProblemDetails p = Problems.forHttpError(404, "Resource not found");
    Assertions.assertNotNull(p);
    Assertions.assertEquals(404, p.status());
    Assertions.assertEquals("urn:berlioz:problem:not-found", p.type());
    Assertions.assertEquals("Not Found", p.title());
    Assertions.assertEquals("Resource not found", p.detail());
  }

  @Test
  void testForHttpError_methodNotAllowed() {
    ProblemDetails p = Problems.forHttpError(405, "Only GET is allowed");
    Assertions.assertNotNull(p);
    Assertions.assertEquals(405, p.status());
    Assertions.assertEquals("urn:berlioz:problem:method-not-allowed", p.type());
    Assertions.assertEquals("Method Not Allowed", p.title());
  }

  @Test
  void testForHttpError_badRequest() {
    ProblemDetails p = Problems.forHttpError(400, "bad input");
    Assertions.assertNotNull(p);
    Assertions.assertEquals("urn:berlioz:problem:bad-request", p.type());
  }

  @Test
  void testForHttpError_serviceUnavailable() {
    ProblemDetails p = Problems.forHttpError(503, "down for maintenance");
    Assertions.assertNotNull(p);
    Assertions.assertEquals("urn:berlioz:problem:service-unavailable", p.type());
  }

  @Test
  void testForHttpError_validStatusOutsideContentStatus() {
    ProblemDetails p = Problems.forHttpError(412, "precondition failed");
    Assertions.assertNotNull(p);
    Assertions.assertEquals(412, p.status());
    Assertions.assertEquals("Precondition Failed", p.title());
    Assertions.assertEquals("urn:berlioz:problem:error", p.type());
  }

  @Test
  void testForHttpError_usesStandardHttpTitles() {
    ProblemDetails nonAuthoritative = Problems.forHttpError(203, "transformed");
    ProblemDetails uriTooLong = Problems.forHttpError(414, "too long");
    Assertions.assertEquals("Non-Authoritative Information", nonAuthoritative.title());
    Assertions.assertEquals("URI Too Long", uriTooLong.title());
  }

  @Test
  void testForHttpError_validUnknownStatusUsesHttpTitle() {
    ProblemDetails p = Problems.forHttpError(599, "network timeout");
    Assertions.assertNotNull(p);
    Assertions.assertEquals(599, p.status());
    Assertions.assertEquals("HTTP 599", p.title());
  }

  @Test
  void testForHttpError_unknownCode() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> Problems.forHttpError(999, "unknown"));
  }

  // --- forHttpError() with berliozErrorId ---

  @Test
  void testForHttpError_withErrorId_usesIdSlug() {
    ProblemDetails p = Problems.forHttpError(500, "stylesheet missing", "berlioz-transform-not-found");
    Assertions.assertNotNull(p);
    Assertions.assertEquals(500, p.status());
    Assertions.assertEquals("urn:berlioz:problem:transform-not-found", p.type());
  }

  @Test
  void testForHttpError_withErrorId_servicesNotFound() {
    ProblemDetails p = Problems.forHttpError(500, "config missing", "berlioz-services-not-found");
    Assertions.assertNotNull(p);
    Assertions.assertEquals("urn:berlioz:problem:services-not-found", p.type());
  }

  @Test
  void testForHttpError_withNullErrorId_fallsBackToCodeSlug() {
    ProblemDetails p = Problems.forHttpError(404, "not found", (String) null);
    Assertions.assertNotNull(p);
    Assertions.assertEquals("urn:berlioz:problem:not-found", p.type());
  }

  @Test
  void testForHttpError_withUnrecognisedErrorId_fallsBackToCodeSlug() {
    ProblemDetails p = Problems.forHttpError(500, "oops", "app-custom-error");
    Assertions.assertNotNull(p);
    Assertions.assertEquals("urn:berlioz:problem:error", p.type());
  }

  @Test
  void testForHttpError_withInvalidBerliozSlug_fallsBackToCodeSlug() {
    ProblemDetails p = Problems.forHttpError(404, "oops", "berlioz-Not_Valid");
    Assertions.assertNotNull(p);
    Assertions.assertEquals("urn:berlioz:problem:not-found", p.type());
  }

  @Test
  void testForHttpError_withThrowableAndStandardDetailAddsException() {
    ProblemDetails p = Problems.forHttpError(500, "oops", new IllegalStateException("boom"), DetailLevel.STANDARD);
    Assertions.assertTrue(p.extensions().containsKey("exception"));
  }

  @Test
  void testForHttpError_withErrorIdThrowableAndMinimalDetailOmitsException() {
    ProblemDetails p = Problems.forHttpError(500, "oops", "berlioz-unexpected",
        new IllegalStateException("boom"), DetailLevel.MINIMAL);
    Assertions.assertEquals("urn:berlioz:problem:unexpected", p.type());
    Assertions.assertFalse(p.extensions().containsKey("exception"));
  }

}
