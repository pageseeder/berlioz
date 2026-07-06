package org.pageseeder.berlioz.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.servlet.ServletTestSupport;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ControlAuthorizationTest {

  @AfterEach
  void resetControlSettings() throws ReflectiveOperationException {
    removeOption(BerliozOption.CONTROL_NETWORK);
    removeOption(BerliozOption.CONTROL_KEY);
  }

  // hasControl(req) — default (network off, no key, no delegated attribute)

  @Test
  void testHasControl_defaultSettings_returnsFalse() {
    HttpServletRequest req = ServletTestSupport.request().build();
    assertFalse(ControlAuthorization.hasControl(req));
  }

  @Test
  void testHasControl_networkOff_ignoresMatchingAuthorizationHeaderWithoutKeyConfigured() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "off");
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz secret123")
        .build();
    assertFalse(ControlAuthorization.hasControl(req));
  }

  @Test
  void testHasControl_unknownNetworkValue_fallsBackToOff() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "bogus");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("127.0.0.1").build();
    assertFalse(ControlAuthorization.hasControl(req));
  }

  // hasControl(req) — key channel (independent of network)

  @ParameterizedTest(name = "key={0}, header=\"{1}\" => {2}")
  @CsvSource({
      "secret123, 'Berlioz secret123', true",
      "secret123, 'Berlioz wrongkey',  false",
      "SECRET,    'Berlioz xyzSECRET', false", // "Berlioz xyzSECRET" must not match key "SECRET" as a suffix
  })
  void testHasControl_key_authorizationHeader(String key, String header, boolean expected) throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_KEY, key);
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", header)
        .build();
    assertEquals(expected, ControlAuthorization.hasControl(req));
  }

  @Test
  void testHasControl_key_matchingAuthorizationHeader_networkOff_stillReturnsTrue() throws ReflectiveOperationException {
    // The key channel authorizes independently of the network channel's value.
    setOption(BerliozOption.CONTROL_NETWORK, "off");
    setOption(BerliozOption.CONTROL_KEY, "secret123");
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz secret123")
        .build();
    assertTrue(ControlAuthorization.hasControl(req));
  }

  @Test
  void testHasControl_key_noHeader_returnsFalse() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_KEY, "secret123");
    HttpServletRequest req = ServletTestSupport.request().build();
    assertFalse(ControlAuthorization.hasControl(req));
  }

  @Test
  void testHasControl_key_queryParameterIgnored_returnsFalse() throws ReflectiveOperationException {
    // The berlioz-control query parameter is no longer read — only the Authorization header.
    setOption(BerliozOption.CONTROL_KEY, "secret123");
    HttpServletRequest req = ServletTestSupport.request()
        .parameter("berlioz-control", "secret123")
        .build();
    assertFalse(ControlAuthorization.hasControl(req));
  }

  @Test
  void testHasControl_key_noKeyConfigured_returnsFalse() {
    // No berlioz.control.key set must fail closed, not match a bare "Berlioz " header.
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz ")
        .build();
    assertFalse(ControlAuthorization.hasControl(req));
  }

  // hasControl(req) — network=loopback/lan

  @ParameterizedTest(name = "network={0}, remoteAddr={1} => {2}")
  @CsvSource({
      "loopback, 127.0.0.1,    true",
      "loopback, ::1,          true",
      "loopback, 203.0.113.10, false",
      "loopback, 192.168.1.10, false", // "loopback" must not also authorize the wider LAN range — that's what "lan" is for
      "lan,      192.168.1.10, true",
      "lan,      127.0.0.1,    true",
      "lan,      203.0.113.10, false",
  })
  void testHasControl_network_remoteAddr(String network, String remoteAddr, boolean expected) throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, network);
    HttpServletRequest req = ServletTestSupport.request().remoteAddr(remoteAddr).build();
    assertEquals(expected, ControlAuthorization.hasControl(req));
  }

  // hasControl(req) — network=loopback/lan, X-Forwarded-For safety net
  //
  // This only ever tightens the plain req.getRemoteAddr() check above: a matching remote address
  // is still required, and a forwarded hop that also matches adds nothing new. It exists to catch
  // a loopback/lan config mistakenly left on behind a same-host or private reverse proxy that
  // forwards the header — see the BerliozOption#CONTROL_NETWORK javadoc.

  @ParameterizedTest(name = "network={0}, remoteAddr={1}, X-Forwarded-For=\"{2}\" => {3}")
  @CsvSource({
      "loopback, 127.0.0.1,    127.0.0.1,                 true",
      // The gap being closed: remoteAddr is the (loopback) proxy, but the real caller is external.
      "loopback, 127.0.0.1,    203.0.113.10,              false",
      // Every hop must match — not just the first (attacker-controlled) or last one.
      "loopback, 127.0.0.1,    '127.0.0.1, 203.0.113.10', false",
      "lan,      127.0.0.1,    192.168.1.10,              true",
      "lan,      192.168.1.10, 203.0.113.10,              false",
      // A malformed/non-IP hop must fail closed, not be silently ignored — and must never be
      // resolved as a hostname (no DNS lookup on attacker-supplied header content).
      "loopback, 127.0.0.1,    not-an-ip-address,         false",
      // An empty/blank header is treated the same as no header — falls back to remoteAddr only.
      "loopback, 127.0.0.1,    '  ',                      true",
  })
  void testHasControl_network_forwardedFor(String network, String remoteAddr, String forwardedFor, boolean expected)
      throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, network);
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr(remoteAddr)
        .header("X-Forwarded-For", forwardedFor)
        .build();
    assertEquals(expected, ControlAuthorization.hasControl(req));
  }

  // hasControl(req) — delegated (fixed attribute) channel
  //
  // network left at the default (off), no key configured, to prove the delegated channel is
  // independent of the other two.

  @ParameterizedTest(name = "attribute={0} => {1}")
  @MethodSource("authorizedAttributeCases")
  void testHasControl_authorizedAttribute(Object attributeValue, boolean expected) {
    ServletTestSupport.RequestBuilder builder = ServletTestSupport.request();
    if (attributeValue != null) {
      builder.attribute(ControlAuthorization.CONTROL_AUTHORIZED_ATTRIBUTE, attributeValue);
    }
    assertEquals(expected, ControlAuthorization.hasControl(builder.build()));
  }

  private static Stream<Arguments> authorizedAttributeCases() {
    return Stream.of(
        Arguments.of(Boolean.TRUE, true),
        Arguments.of(null, false),
        Arguments.of(Boolean.FALSE, false),
        Arguments.of("true", false)
    );
  }

  // GlobalSettings test helpers (direct SETTINGS map manipulation, mirroring ErrorHandlerServletTest)

  private static void setOption(BerliozOption option, String value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().put(option.property(), value);
  }

  private static void removeOption(BerliozOption option) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    if (ref.get() != null) ref.get().remove(option.property());
  }

  @SuppressWarnings("unchecked")
  private static AtomicReference<Map<String, String>> settingsRef() throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    return (AtomicReference<Map<String, String>>) f.get(null);
  }
}
