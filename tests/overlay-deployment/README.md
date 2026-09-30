# Packaged overlay deployment tests

Run from the repository root with Java 11:

```sh
./gradlew overlayIntegrationTest
```

The task also runs through `check` and `build`. Reports are written to
`tests/overlay-deployment/build/reports/tests/integrationTest/index.html`.
The ordinary unit-test command (`./gradlew test`) does not start a container.

## Deployment

`tests:overlay-fixture` builds a real JAR containing:

- `META-INF/berlioz/services.xml` and an overlay-owned generator;
- primary XSLT with relative imports and includes;
- `META-INF/resources/overlay/marker.txt`;
- a `META-INF/web-fragment.xml` defining the servlets and a marker filter;
- diagnostic routes for configuration, error handling, runtime services, and source warnings.

`tests:overlay-deployment` packages the fixture and Berlioz runtime dependencies in a WAR's
`WEB-INF/lib`. The host declares a conflicting filesystem route. Neither Berlioz nor the
fixture is a dependency of the test runner; assertions verify that its classloader cannot
load them. Jetty discovers servlet registrations from the web fragment.

The suite uses Jetty 10.0.17, matching the existing Gretty setup's verified dependencies,
and the Java 11 toolchain. Each scenario gets a fresh extracted WAR, servlet classloader,
and server bound to `127.0.0.1` on an OS-assigned port. HTTP requests and tests have bounded
timeouts; teardown stops and destroys Jetty even after failures. JUnit removes temporary
files. No external server or fixed port is required.

The fixture and harness are test-only projects and have no publishing configuration.
Their administrative endpoints and loopback control configuration are intended solely for
this test server, not as production security examples.

## Coverage

The six scenarios verify all twelve packaged-deployment checks in `PLAN-0.14.3.md`:

1. JAR service discovery and generator classloading; primary XSLT with packaged imports/includes.
2. Static resources from the JAR and execution of the web-fragment filter.
3. Filesystem route precedence and an override warning identifying both declarations/origins.
4. Deployed servlet diagnostics and discovered error forwarding extensions, with equivalent
   typed XML/JSON values and conditional HTTP requests returning 304 for unchanged diagnostics.
5. A malformed reload retaining the old route behavior and warnings, followed by recovery.
6. Startup and request processing with no filesystem `services.xml`.
