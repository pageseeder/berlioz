package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BerliozConfigTest {

  @AfterEach
  void resetControlSettings() throws ReflectiveOperationException {
    removeOption(BerliozOption.CONTROL_NETWORK);
    removeOption(BerliozOption.CONTROL_KEY);
  }

  // hasControl(req) — default (network off, no key, no delegated attribute)

  @Test
  void testHasControl_defaultSettings_returnsFalse() {
    HttpServletRequest req = ServletTestSupport.request().build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_networkOff_ignoresMatchingAuthorizationHeaderWithoutKeyConfigured() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "off");
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz secret123")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_unknownNetworkValue_fallsBackToOff() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "bogus");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("127.0.0.1").build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  // hasControl(req) — key channel (independent of network)

  @Test
  void testHasControl_key_matchingAuthorizationHeader_returnsTrue() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_KEY, "secret123");
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz secret123")
        .build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_key_matchingAuthorizationHeader_networkOff_stillReturnsTrue() throws ReflectiveOperationException {
    // The key channel authorizes independently of the network channel's value.
    setOption(BerliozOption.CONTROL_NETWORK, "off");
    setOption(BerliozOption.CONTROL_KEY, "secret123");
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz secret123")
        .build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_key_wrongAuthorizationHeader_returnsFalse() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_KEY, "secret123");
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz wrongkey")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_key_noHeader_returnsFalse() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_KEY, "secret123");
    HttpServletRequest req = ServletTestSupport.request().build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_key_partialKeySuffix_returnsFalse() throws ReflectiveOperationException {
    // Ensure "Berlioz xyzSECRET" doesn't match key "SECRET"
    setOption(BerliozOption.CONTROL_KEY, "SECRET");
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz xyzSECRET")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_key_queryParameterIgnored_returnsFalse() throws ReflectiveOperationException {
    // The berlioz-control query parameter is no longer read — only the Authorization header.
    setOption(BerliozOption.CONTROL_KEY, "secret123");
    HttpServletRequest req = ServletTestSupport.request()
        .parameter("berlioz-control", "secret123")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_key_noKeyConfigured_returnsFalse() throws ReflectiveOperationException {
    // No berlioz.control.key set must fail closed, not match a bare "Berlioz " header.
    HttpServletRequest req = ServletTestSupport.request()
        .header("Authorization", "Berlioz ")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  // hasControl(req) — network=loopback

  @Test
  void testHasControl_loopback_loopbackIPv4_returnsTrue() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("127.0.0.1").build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_loopback_loopbackIPv6_returnsTrue() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("::1").build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_loopback_nonLoopbackAddress_returnsFalse() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("203.0.113.10").build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_loopback_privateAddress_returnsFalse() throws ReflectiveOperationException {
    // "loopback" must not also authorize the wider LAN range — that's what "lan" is for.
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("192.168.1.10").build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  // hasControl(req) — network=lan

  @Test
  void testHasControl_lan_privateAddress_returnsTrue() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "lan");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("192.168.1.10").build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_lan_loopbackAddress_returnsTrue() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "lan");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("127.0.0.1").build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_lan_publicAddress_returnsFalse() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "lan");
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("203.0.113.10").build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  // hasControl(req) — network=loopback/lan, X-Forwarded-For safety net
  //
  // This only ever tightens the plain req.getRemoteAddr() check above: a matching remote address
  // is still required, and a forwarded hop that also matches adds nothing new. It exists to catch
  // a loopback/lan config mistakenly left on behind a same-host or private reverse proxy that
  // forwards the header — see the BerliozOption#CONTROL_NETWORK javadoc.

  @Test
  void testHasControl_loopback_forwardedForAlsoLoopback_returnsTrue() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("X-Forwarded-For", "127.0.0.1")
        .build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_loopback_forwardedForPublicAddress_returnsFalse() throws ReflectiveOperationException {
    // The gap being closed: remoteAddr is the (loopback) proxy, but the real caller is external.
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("X-Forwarded-For", "203.0.113.10")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_loopback_forwardedForChainWithPublicHop_returnsFalse() throws ReflectiveOperationException {
    // Every hop must match — not just the first (attacker-controlled) or last one.
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("X-Forwarded-For", "127.0.0.1, 203.0.113.10")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_lan_forwardedForAlsoPrivate_returnsTrue() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "lan");
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("X-Forwarded-For", "192.168.1.10")
        .build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_lan_forwardedForPublicAddress_returnsFalse() throws ReflectiveOperationException {
    setOption(BerliozOption.CONTROL_NETWORK, "lan");
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("192.168.1.10")
        .header("X-Forwarded-For", "203.0.113.10")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_loopback_forwardedForNonIpToken_returnsFalse() throws ReflectiveOperationException {
    // A malformed/non-IP hop must fail closed, not be silently ignored — and must never be
    // resolved as a hostname (no DNS lookup on attacker-supplied header content).
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("X-Forwarded-For", "not-an-ip-address")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_loopback_blankForwardedFor_returnsTrue() throws ReflectiveOperationException {
    // An empty/blank header is treated the same as no header — falls back to remoteAddr only.
    setOption(BerliozOption.CONTROL_NETWORK, "loopback");
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("X-Forwarded-For", "  ")
        .build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  // hasControl(req) — delegated (fixed attribute) channel

  @Test
  void testHasControl_authorizedAttribute_setToTrue_returnsTrue() {
    // network left at the default (off), no key configured, to prove the delegated channel is
    // independent of the other two.
    HttpServletRequest req = ServletTestSupport.request()
        .attribute(BerliozConfig.CONTROL_AUTHORIZED_ATTRIBUTE, Boolean.TRUE)
        .build();
    assertTrue(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_authorizedAttribute_notSet_returnsFalse() {
    HttpServletRequest req = ServletTestSupport.request().build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_authorizedAttribute_setToFalse_returnsFalse() {
    HttpServletRequest req = ServletTestSupport.request()
        .attribute(BerliozConfig.CONTROL_AUTHORIZED_ATTRIBUTE, Boolean.FALSE)
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  @Test
  void testHasControl_authorizedAttribute_wrongType_returnsFalse() {
    HttpServletRequest req = ServletTestSupport.request()
        .attribute(BerliozConfig.CONTROL_AUTHORIZED_ATTRIBUTE, "true")
        .build();
    assertFalse(BerliozConfig.hasControl(req));
  }

  // Listener static methods (just verify they don't throw)

  @Test
  void testGetListener_initiallyNull() {
    // Reset to null before checking
    BerliozConfig.setListener(null);
    assertNull(BerliozConfig.getListener());
  }

  @Test
  void resetETagSeed_generatesAndPersistsSeed(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    BerliozConfig config = BerliozConfig.newConfig(servletConfig(contextRoot));

    config.resetETagSeed();

    assertAll(
        () -> assertNotEquals(0L, config.getETagSeed()),
        () -> assertTrue(Files.exists(contextRoot.resolve("WEB-INF/berlioz.etag")))
    );
  }

  private static ServletConfig servletConfig(Path contextRoot) {
    ServletContext context = (ServletContext) Proxy.newProxyInstance(
        ServletContext.class.getClassLoader(),
        new Class<?>[]{ServletContext.class},
        (proxy, m, args) -> {
          if ("getRealPath".equals(m.getName())) return contextRoot.toString();
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
    return (ServletConfig) Proxy.newProxyInstance(
        ServletConfig.class.getClassLoader(),
        new Class<?>[]{ServletConfig.class},
        (proxy, m, args) -> {
          if ("getServletContext".equals(m.getName())) return context;
          if ("getServletName".equals(m.getName())) return "test-config";
          if ("getInitParameter".equals(m.getName())) return null;
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
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
