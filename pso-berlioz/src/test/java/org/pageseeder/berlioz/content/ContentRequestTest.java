package org.pageseeder.berlioz.content;

import org.junit.jupiter.api.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the default bridge methods on the {@link ContentRequest} interface.
 *
 * <p>The focus is on verifying that {@code parameterNames()}, {@code parameterValues(String)},
 * and {@code cookies()} correctly delegate to the deprecated legacy methods when the
 * implementation does not override them.
 */
final class ContentRequestTest {

  // --- parameterNames() ---

  @Test
  void testParameterNames_delegatesToGetParameterNames() {
    ContentRequest req = stub(
        new String[]{"foo", "bar"},
        null,
        null
    );
    var names = req.parameterNames();
    assertTrue(names.contains("foo"));
    assertTrue(names.contains("bar"));
    assertEquals(2, names.size());
  }

  @Test
  void testParameterNames_emptyEnumeration_returnsEmptyCollection() {
    ContentRequest req = stub(new String[0], null, null);
    assertTrue(req.parameterNames().isEmpty());
  }

  // --- parameterValues(name) ---

  @Test
  void testParameterValues_delegatesToGetParameterValues() {
    ContentRequest req = stub(
        new String[0],
        new String[]{"v1", "v2"},
        null
    );
    List<String> values = req.parameterValues("p");
    assertEquals(List.of("v1", "v2"), values);
  }

  @Test
  void testParameterValues_nullFromDeprecated_returnsEmptyList() {
    ContentRequest req = stub(new String[0], null, null);
    List<String> values = req.parameterValues("missing");
    assertNotNull(values);
    assertTrue(values.isEmpty());
  }

  // --- cookies() ---

  @Test
  void testCookies_delegatesToGetCookies() {
    Cookie c1 = new Cookie("session", "abc");
    Cookie c2 = new Cookie("prefs", "dark");
    ContentRequest req = stub(new String[0], null, new Cookie[]{c1, c2});
    List<Cookie> cookies = req.cookies();
    assertEquals(2, cookies.size());
    assertSame(c1, cookies.get(0));
    assertSame(c2, cookies.get(1));
  }

  @Test
  void testCookies_nullFromDeprecated_returnsEmptyList() {
    ContentRequest req = stub(new String[0], null, null);
    List<Cookie> cookies = req.cookies();
    assertNotNull(cookies);
    assertTrue(cookies.isEmpty());
  }

  // --- helper ---

  /**
   * Builds a minimal stub ContentRequest where:
   * <ul>
   *   <li>{@code parameterNameArr} is the array returned by {@link ContentRequest#getParameterNames()}</li>
   *   <li>{@code valuesArr} is the array returned by {@link ContentRequest#getParameterValues(String)}</li>
   *   <li>{@code cookiesArr} is the array returned by {@link ContentRequest#getCookies()}</li>
   * </ul>
   * The default {@code parameterNames()}, {@code parameterValues()}, and {@code cookies()} methods
   * are NOT overridden so they exercise the default delegation logic defined in {@link ContentRequest}.
   */
  @SuppressWarnings("deprecation")
  private static ContentRequest stub(String[] parameterNameArr, String[] valuesArr, Cookie[] cookiesArr) {
    return new ContentRequest() {

      // Request abstract methods
      @Override public String getBerliozPath() { return "/test"; }
      @Override public String getParameter(String name) { return null; }
      @Override public String getParameter(String name, String def) { return def; }
      @Override public Object getAttribute(String name) { return null; }
      @Override public void setAttribute(String name, Object o) { throw new UnsupportedOperationException(); }
      @Override public HttpSession getSession() { return null; }
      @Override public Environment getEnvironment() { return null; }
      @Override public Location getLocation() { return null; }

      // ContentRequest abstract methods
      @Override public int getIntParameter(String name, int def) { return def; }
      @Override public long getLongParameter(String name, long def) { return def; }
      @Override public Enumeration<String> getParameterNames() {
        return Collections.enumeration(List.of(parameterNameArr));
      }
      @Override public String[] getParameterValues(String name) { return valuesArr; }
      @Override public Cookie[] getCookies() { return cookiesArr; }
      @Override public Date getDateParameter(String name) { return null; }
      @Override public void setStatus(ContentStatus code) { throw new UnsupportedOperationException(); }
      @Override public void setRedirect(String url, ContentStatus code) { throw new UnsupportedOperationException(); }
    };
  }
}
