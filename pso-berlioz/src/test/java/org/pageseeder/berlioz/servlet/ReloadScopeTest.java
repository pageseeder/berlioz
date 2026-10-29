package org.pageseeder.berlioz.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

class ReloadScopeTest {

  @Test
  void parse_noScope() {
    assertTrue(ReloadScope.parse(null).isEmpty());
    assertTrue(ReloadScope.parse("").isEmpty());
    assertTrue(ReloadScope.parse("false").isEmpty());
  }

  @Test
  void parse_trueMeansEveryScope() {
    assertEquals(EnumSet.allOf(ReloadScope.class), ReloadScope.parse("true"));
  }

  @Test
  void parse_listInAnyOrder() {
    assertEquals(EnumSet.of(ReloadScope.SERVICES), ReloadScope.parse("services"));
    assertEquals(EnumSet.of(ReloadScope.CONFIG, ReloadScope.SERVICES), ReloadScope.parse("services,config"));
    assertEquals(EnumSet.of(ReloadScope.XSLT, ReloadScope.ETAGS, ReloadScope.REDIRECTS),
        ReloadScope.parse(" redirects , etags,xslt,xslt"));
  }

  @Test
  void parse_rejectsUnknownOrEmptyScopes() {
    assertThrows(IllegalArgumentException.class, () -> ReloadScope.parse("services,xsl"));
    assertThrows(IllegalArgumentException.class, () -> ReloadScope.parse("services,"));
    assertThrows(IllegalArgumentException.class, () -> ReloadScope.parse("Services"));
    assertThrows(IllegalArgumentException.class, () -> ReloadScope.parse("all"));
  }

  @Test
  void parse_unknownScopeMessageOnlyKeepsSafeCharacters() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> ReloadScope.parse("bad\r\nscope"));
    assertFalse(ex.getMessage().contains("\n"), ex.getMessage());
  }

  @Test
  void format_usesApplicationOrder() {
    assertEquals("config, services, xslt, etags, redirects", ReloadScope.format(EnumSet.allOf(ReloadScope.class)));
    assertEquals("services, etags", ReloadScope.format(ReloadScope.parse("etags,services")));
    assertEquals("", ReloadScope.format(EnumSet.noneOf(ReloadScope.class)));
  }

  @Test
  void reportAppliedPreservesScopesAcrossResponseReset() {
    HttpServletRequest req = ServletTestSupport.request().build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    HttpServletResponse res = recorder.build();
    ReloadScope.reportApplied(req, res, Set.of(ReloadScope.CONFIG));
    res.reset();
    ReloadScope.reportApplied(req, res, Set.of(ReloadScope.REDIRECTS));
    assertEquals("config, redirects", recorder.header(ReloadScope.HEADER));
  }

  @Test
  void fromRequest_combinesScopesAndDeprecatedAliases() {
    Set<ReloadScope> scopes = ReloadScope.fromRequest(ServletTestSupport.request()
        .parameter("berlioz-reload", "config")
        .parameter("reload-services", "true")
        .parameter("clear-xsl-cache", "true")
        .parameter("reset-etags", "false")
        .build());
    assertEquals(EnumSet.of(ReloadScope.CONFIG, ReloadScope.SERVICES, ReloadScope.XSLT), scopes);
  }

  @Test
  void fromRequest_unknownScopeRejectsWholeRequest() {
    Set<ReloadScope> scopes = ReloadScope.fromRequest(ServletTestSupport.request()
        .parameter("berlioz-reload", "services,bogus")
        .parameter("reset-etags", "true")
        .build());
    assertTrue(scopes.isEmpty());
  }

  @Test
  @SuppressWarnings("removal")
  void controlParameters_coverEveryScopeAndAlias() {
    assertTrue(ControlParameter.isControlParameter("berlioz-reload"));
    assertTrue(ControlParameter.isControlParameter("berlioz-profile"));
    assertTrue(ControlParameter.isControlParameter("reload-services"));
    assertTrue(ControlParameter.isControlParameter("clear-xsl-cache"));
    assertTrue(ControlParameter.isControlParameter("reset-etags"));
    assertFalse(ControlParameter.isControlParameter("berlioz-other"));
    assertFalse(ControlParameter.isControlParameter(null));
    assertEquals(ReloadScope.SERVICES, ControlParameter.RELOAD_SERVICES.alias());
    assertEquals(ReloadScope.XSLT, ControlParameter.CLEAR_XSL_CACHE.alias());
    assertEquals(ReloadScope.ETAGS, ControlParameter.RESET_ETAGS.alias());
  }
}
