package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.*;
import org.pageseeder.berlioz.content.ServiceStatusRule.CodeRule;
import org.pageseeder.berlioz.content.ServiceTestHelper;
import org.pageseeder.berlioz.error.ProblemDetails;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.util.CompoundBerliozException;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorOutcomeTest {

  // Helpers --------------------------------------------------------------------------------------

  private static Service serviceWithRule(CodeRule rule) {
    ServiceStatusRule statusRule = ServiceStatusRule.newInstance("*", rule.name());
    return ServiceTestHelper.build("test", statusRule, new NoContent());
  }

  // getStatus ------------------------------------------------------------------------------------

  @Test
  void getStatus_initiallyReturnsOk() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    assertEquals(ContentStatus.OK, outcome.getStatus());
  }

  @Test
  void getError_initiallyNull() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    assertNull(outcome.getError());
  }

  @Test
  void getRedirectURL_initiallyNull() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    assertNull(outcome.getRedirectURL());
  }

  // handleError ----------------------------------------------------------------------------------

  @Test
  void handleError_runtimeException_wrapsToBerliozException() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    RuntimeException ex = new RuntimeException("test failure");
    BerliozGenerator generator = new NoContent();

    BerliozException result = outcome.handleError(ex, generator);

    assertNotNull(result);
    assertEquals(BerliozErrorID.GENERATOR_ERROR_UNCHECKED, result.id());
    assertSame(ex, result.getCause());
    assertSame(result, outcome.getError());
  }

  @Test
  void handleError_berliozException_storesIt() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    BerliozException bex = new BerliozException("forced", BerliozErrorID.GENERATOR_ERROR_UNFORCED);
    BerliozGenerator generator = new NoContent();

    BerliozException result = outcome.handleError(bex, generator);

    assertSame(bex, result);
    assertSame(bex, outcome.getError());
  }

  @Test
  void handleError_twoErrors_producesCompoundException() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    BerliozGenerator generator = new NoContent();

    outcome.handleError(new RuntimeException("first"), generator);
    outcome.handleError(new RuntimeException("second"), generator);

    assertTrue(outcome.getError() instanceof CompoundBerliozException);
  }

  @Test
  void handleError_threeErrors_compoundStillHolds() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    BerliozGenerator generator = new NoContent();

    outcome.handleError(new RuntimeException("1"), generator);
    outcome.handleError(new RuntimeException("2"), generator);
    outcome.handleError(new RuntimeException("3"), generator);

    assertTrue(outcome.getError() instanceof CompoundBerliozException);
  }

  // handleStatus ----------------------------------------------------------------------------------

  @Test
  void handleStatus_highestRule_higherStatusWins() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    Service service = serviceWithRule(CodeRule.HIGHEST);
    NoContent generator = new NoContent();

    // First call sets to NOT_FOUND (404)
    Response r404 = Response.status(ContentStatus.NOT_FOUND);
    outcome.handleStatus(r404, generator, service);
    assertEquals(ContentStatus.NOT_FOUND, outcome.getStatus());

    // Second call with OK (200) should NOT displace NOT_FOUND
    outcome.handleStatus(Response.ok(), generator, service);
    assertEquals(ContentStatus.NOT_FOUND, outcome.getStatus());
  }

  @Test
  void handleStatus_highestRule_validStatusOutsideContentStatusIsPreserved() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    Service service = serviceWithRule(CodeRule.HIGHEST);
    NoContent generator = new NoContent();

    outcome.handleStatus(Response.problem(ProblemDetails.of(412)), generator, service);
    outcome.handleStatus(Response.ok(), generator, service);

    assertEquals(412, outcome.getStatusCode());
    assertEquals(ContentStatus.BAD_REQUEST, outcome.getStatus());
  }

  @Test
  void handleStatus_highestRule_higherStatusDisplacesCurrent() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    Service service = serviceWithRule(CodeRule.HIGHEST);
    NoContent generator = new NoContent();

    outcome.handleStatus(Response.ok(), generator, service);
    assertEquals(ContentStatus.OK, outcome.getStatus());

    Response r500 = Response.status(ContentStatus.INTERNAL_SERVER_ERROR);
    outcome.handleStatus(r500, generator, service);
    assertEquals(ContentStatus.INTERNAL_SERVER_ERROR, outcome.getStatus());
  }

  @Test
  void handleStatus_lowestRule_lowerStatusWins() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    Service service = serviceWithRule(CodeRule.LOWEST);
    NoContent generator = new NoContent();

    outcome.handleStatus(Response.status(ContentStatus.NOT_FOUND), generator, service);
    assertEquals(ContentStatus.NOT_FOUND, outcome.getStatus());

    // OK (200) < NOT_FOUND (404), so OK should win under LOWEST
    outcome.handleStatus(Response.ok(), generator, service);
    assertEquals(ContentStatus.OK, outcome.getStatus());
  }

  @Test
  void handleStatus_lowestRule_higherStatusDoesNotDisplace() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    Service service = serviceWithRule(CodeRule.LOWEST);
    NoContent generator = new NoContent();

    outcome.handleStatus(Response.ok(), generator, service);
    outcome.handleStatus(Response.status(ContentStatus.NOT_FOUND), generator, service);

    assertEquals(ContentStatus.OK, outcome.getStatus());
  }

  @Test
  void handleStatus_redirectResponse_capturesRedirectUrl() {
    GeneratorOutcome outcome = new GeneratorOutcome();
    Service service = serviceWithRule(CodeRule.HIGHEST);
    NoContent generator = new NoContent();

    Response redirect = Response.redirect(ContentStatus.SEE_OTHER, "/new-location");
    outcome.handleStatus(redirect, generator, service);

    assertEquals("/new-location", outcome.getRedirectURL());
    assertTrue(ContentStatus.isRedirect(outcome.getStatus()));
  }

  @Test
  void handleStatus_nonAffectingGenerator_ignored() {
    // Rule that only applies to a generator named "other-generator"; NoContent maps to "no-content"
    ServiceStatusRule selectiveRule = ServiceTestHelper.namedRule("other-generator");
    Service service = ServiceTestHelper.build("selective", selectiveRule, new NoContent());
    NoContent generator = (NoContent) service.generators().get(0);

    GeneratorOutcome outcome = new GeneratorOutcome();
    outcome.handleStatus(Response.status(ContentStatus.NOT_FOUND), generator, service);

    // "no-content" is not in the name selector — status stays at default OK
    assertEquals(ContentStatus.OK, outcome.getStatus());
  }
}
