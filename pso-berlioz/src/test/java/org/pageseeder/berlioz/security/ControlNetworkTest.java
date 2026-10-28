package org.pageseeder.berlioz.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.pageseeder.berlioz.servlet.ServletTestSupport;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ControlNetworkTest {

  @Test
  void testMatches_off_neverMatches() {
    HttpServletRequest req = ServletTestSupport.request().remoteAddr("127.0.0.1").build();
    assertFalse(ControlNetwork.OFF.matches(req));
  }

  @Test
  void testMatches_nullRemoteAddr_failsClosed() {
    // InetAddress.getByName(null) would return the loopback address
    HttpServletRequest req = ServletTestSupport.request().remoteAddr(null).build();
    assertFalse(ControlNetwork.LOOPBACK.matches(req));
    assertFalse(ControlNetwork.LAN.matches(req));
  }

  // Forwarded (RFC 7239): every for= parameter must match, like X-Forwarded-For hops

  @ParameterizedTest(name = "network={0}, Forwarded=\"{1}\" => {2}")
  @CsvSource(delimiter = '|', value = {
      "LOOPBACK | for=127.0.0.1                                  | true",
      "LOOPBACK | for=203.0.113.1                                | false",
      // Local dev proxy: loopback hops, with port and other parameters, still match
      "LOOPBACK | for=127.0.0.1:52144;proto=http;host=localhost  | true",
      "LOOPBACK | 'for=\"[::1]:4711\"'                           | true",
      "LOOPBACK | 'for=\"[::1]\"'                                | true",
      // Lenient: unbracketed IPv6 is not RFC 7239 compliant but accepted
      "LOOPBACK | for=::1                                        | true",
      "LOOPBACK | For=127.0.0.1                                  | true",
      // Every element must match, not just the first
      "LOOPBACK | 'for=127.0.0.1, for=203.0.113.1'               | false",
      "LOOPBACK | 'for=127.0.0.1;proto=http, for=203.0.113.1'    | false",
      // Unknown, obfuscated, hostnames and malformed nodes fail closed
      "LOOPBACK | for=unknown                                    | false",
      "LOOPBACK | for=_hidden                                    | false",
      "LOOPBACK | for=localhost                                  | false",
      "LOOPBACK | 'for=\"[::1\"'                                 | false",
      "LOOPBACK | for=                                           | false",
      // No for= parameter: nothing to check
      "LOOPBACK | proto=https;host=example.org                   | true",
      "LAN      | for=192.168.1.10                               | true",
      "LAN      | for=203.0.113.1                                | false",
  })
  void testMatches_forwarded(ControlNetwork network, String forwarded, boolean expected) {
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("Forwarded", forwarded)
        .build();
    assertEquals(expected, network.matches(req));
  }

  @ParameterizedTest(name = "network={0}, X-Real-IP=\"{1}\" => {2}")
  @CsvSource({
      "LOOPBACK, 127.0.0.1,    true",
      "LOOPBACK, ::1,          true",
      "LOOPBACK, 203.0.113.1,  false",
      "LOOPBACK, localhost,    false",
      "LOOPBACK, '  ',         true",
      "LAN,      192.168.1.10, true",
      "LAN,      203.0.113.1,  false",
  })
  void testMatches_realIp(ControlNetwork network, String realIp, boolean expected) {
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("X-Real-IP", realIp)
        .build();
    assertEquals(expected, network.matches(req));
  }

  @Test
  void testMatches_allHeadersChecked() {
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .header("X-Forwarded-For", "127.0.0.1")
        .header("Forwarded", "for=127.0.0.1")
        .header("X-Real-IP", "203.0.113.1")
        .build();
    assertFalse(ControlNetwork.LOOPBACK.matches(req));
  }

  @Test
  void testMatches_hostIsNotChecked() {
    // Custom dev hostnames (e.g. myapp.local in /etc/hosts) must keep working
    HttpServletRequest req = ServletTestSupport.request()
        .remoteAddr("127.0.0.1")
        .host("myapp.local")
        .header("Host", "myapp.local")
        .build();
    assertTrue(ControlNetwork.LOOPBACK.matches(req));
  }

}
