package org.pageseeder.berlioz.fixture;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

/** Marker proving web-fragment discovery and filter execution. */
public final class FragmentFilter implements Filter {
  @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    ((HttpServletResponse) response).setHeader("X-Overlay-Fragment", "active");
    chain.doFilter(request, response);
  }
}
