/*
 * Copyright (c) 1999-2023. Allette Systems Pty Ltd
 */
package org.pageseeder.berlioz.security;

/**
 * A Content-Security-Policy directive.
 *
 * @author Christophe Lauret
 * @version 0.12.6
 * @version 0.12.6
 */
public enum Directive {

  /**
   * The default policy for fetching resources such as JavaScript, Images, CSS, Fonts, AJAX requests,
   * Frames, HTML5 Media.
   *
   * <p>Used as fallback for other directives, except for the following
   * (failing to set them is the same as allowing anything):
   * <ul>
   *  <li>base-uri</li>
   *  <li>form-action</li>
   *  <li>frame-ancestors</li>
   *  <li>plugin-types</li>
   *  <li>report-uri</li>
   *  <li>sandbox</li>
   * </ul>
   */
  DEFAULT_SRC("default-src"),

  /**
   * {@code child-src} lists the URLs for workers and embedded frame contents.
   *
   * <p>Falls back to {@code default-src} directive.
   */
  CHILD_SRC("child-src"),

  /**
   * {@code connect-src} limits the origins that you can connect to via XMLHttpRequest (AJAX), WebSocket,
   * fetch(), <a ping> or EventSource).
   *
   * <p>Falls back to {@code default-src} directive.
   */
  CONNECT_SRC("connect-src"),

  /**
   * {@code font-src} specifies the origins that can serve web fonts loaded via @font-face.
   *
   * <p>Falls back to {@code default-src} directive.
   */
  FONT_SRC("font-src"),

  /**
   * {@code frame-ancestors} specifies the sources that can embed the current page.
   *
   * <p>Defines valid sources for embedding the resource using {@code <frame> <iframe> <object> <embed> <applet>}.
   *
   * <p>Setting this directive to 'none' should be roughly equivalent to <code>X-Frame-Options: DENY</code>
   *
   * <p>No fallback.</p>
   */
  FRAME_ANCESTORS("frame-ancestors"),

  /**
   * {@code frame-src} was deprecated in level 2, but is restored in level 3.
   *
   * <p>Falls back to {@code child-src} directive, then falls back to {@code default-src} directive.
   */
  FRAME_SRC("frame-src"),

  /**
   * {@code img-src} defines the origins from which images and favicons can be loaded.
   *
   * <p>Falls back to {@code default-src} directive.
   */
  IMG_SRC("img-src"),

  /**
   * {@code manifest-src} restricts the URLs that application manifests can be loaded.
   *
   * <p>Falls back to {@code default-src} directive.
   */
  MANIFEST_SRC("manifest-src"),

  /**
   * {@code media-src} restricts the origins allowed to deliver video and audio
   * via {@code <audio>} and {@code <video>} elements.
   *
   * <p>Falls back to {@code default-src} directive.
   */
  MEDIA_SRC("media-src"),

  /**
   * {@code object-src} allows control over Flash and other plugins in {@code <object>, <embed> or <applet>}.
   *
   * <p>Use the {@code plugin-types} directive to set allowed types.
   *
   * <p>Falls back to {@code default-src} directive.
   *
   * <p>Recommended setting is 'none'.
   */
  OBJECT_SRC("object-src"),

  /**
   * {@code script-src} specifies valid sources for JavaScript.
   *
   * <p>Falls back to {@code default-src} directive
   */
  SCRIPT_SRC("script-src"),

  /**
   * {@code script-src-attr} specifies valid sources for JavaScript inline event handlers.
   *
   * <p>Falls back to <code>script-src</code>, then <code>default-src</code> directive.
   */
  SCRIPT_SRC_ATTR("script-src-attr"),

  /**
   * {@code script-src-elem} specifies valid sources for JavaScript {@code <script>} elements.
   *
   * <p>Falls back to <code>script-src</code>, then <code>default-src</code> directive.
   */
  SCRIPT_SRC_ELEM("script-src-elem"),

  /**
   * <code>style-src</code> defines valid sources for stylesheets and CSS.
   */
  STYLE_SRC("style-src"),

  /**
   * {@code style-src-attr} specifies valid sources for inline styles applied to individual DOM elements.
   *
   * <p>Falls back to <code>style-src</code>, then <code>default-src</code> directive.
   */
  STYLE_SRC_ATTR("style-src-attr"),

  /**
   * {@code style-src-elem} specifies valid sources for stylesheet <style> elements and <link> elements
   * with rel="stylesheet".
   *
   * <p>Falls back to <code>style-src</code>, then <code>default-src</code> directive.
   */
  STYLE_SRC_ELEM("style-src-elem"),

  /**
   * {@code worker-src} restricts the URLs that may be loaded as a worker,
   * shared worker, or service worker.
   *
   * <p>Falls back to <code>child-src</code>, then <code>script-src</code>, then <code>default-src</code>.
   */
  WORKER_SRC("worker-src"),

  /**
   * {@code base-uri} restricts the URLs that can appear in a page's {@code <base>} element.
   *
   * <p>Recommended setting is 'none'. No falls back source.
   */
  BASE_URI("base-uri"),

  /**
   * {@code form-action} lists valid endpoints for submission from {@code <form>} tags.
   *
   * <p>No fallback source.
   */
  FORM_ACTION("form-action"),

  /**
   * {@code plugin-types} defines valid MIME types for plugins invoked via {@code <object>} and
   * {@code <embed>}.
   *
   * <p>To load an <applet> you must specify application/x-java-applet.
   */
  @Deprecated
  PLUGIN_TYPES("plugin-types"),

  /**
   * Enables a sandbox for the requested resource similar to the iframe sandbox attribute.
   *
   * <p>The sandbox applies a same origin policy, prevents popups, plugins and script execution is blocked.
   *
   * <p>You can keep the sandbox value empty to keep all restrictions in place, or add values:
   * allow-forms allow-same-origin allow-scripts allow-popups, allow-modals, allow-orientation-lock, allow-pointer-lock, allow-presentation, allow-popups-to-escape-sandbox, and allow-top-navigation
   */
  SANDBOX("sandbox"),

  /**
   * {@code upgrade-insecure-requests} directive instructs user agents to treat all of a site's insecure URLs
   * (HTTP) as though they have been replaced with secure URLs (HTTPS).
   *
   * <p>Affects third-party non-navigational resource request such as {@code <img>} but not navigational
   * like {@code <a>}
   *
   * <p>NB. Not a replacement for HSTS.
   */
  UPGRADE_INSECURE_REQUESTS("upgrade-insecure-requests"),

  /**
   * {@code report-to} instructs the user agent to store reporting endpoints for an origin.
   *
   * <p>The directive has no effect unless the <code>Report-To</code> or <code>Reporting-Endpoints</code> HTTP
   * headers are specified.
   *
   * <p>This directive can't be used in {@code <meta>} tags.
   *
   * @see <a href="https://caniuse.com/mdn-http_headers_content-security-policy_report-to">Can I use: CSP report-to</a>
   */
  REPORT_TO("report-to"),

  /**
   * {@code report-uri} specifies a URL where a browser will send reports when a content security policy is violated.
   *
   * <p>This directive is deprecated in CSP Level 3 in favor of the {@code report-to} directive.
   *
   * <p>This directive can't be used in {@code <meta>} tags.
   *
   * @see <a href="https://caniuse.com/mdn-http_headers_content-security-policy_report-uri">Can I use: CSP report-uri</a>
   */
  REPORT_URI("report-uri");

  private final String s;

  Directive(String s){
    this.s = s;
  }

  @Override
  public String toString() {
    return s;
  }
}
