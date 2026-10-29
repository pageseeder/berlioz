# Contributing to Berlioz

## Building

Berlioz builds with Gradle and targets Java 11.

```bash
# Build all modules
./gradlew build

# Run the tests of all modules, or of one module
./gradlew test
./gradlew :pso-berlioz:test

# Run a single test class or method
./gradlew :pso-berlioz:test --tests "org.pageseeder.berlioz.furi.URITemplateTest"
./gradlew :pso-berlioz:test --tests "org.pageseeder.berlioz.furi.URITemplateTest.testSomeMethod"

# Generate the Javadoc
./gradlew javadoc

# Check for dependency updates
./gradlew dependencyUpdates
```

## Modules

- `pso-berlioz`: the core library.
- `pso-berlioz-mock`: mock implementations of the core interfaces, for application tests.
- `pso-berlioz-kickstart`: a web fragment with the standard servlet mappings and fallback stylesheets.
- `samples/minimal-gretty`: a minimal application; run it with `./gradlew :samples:minimal-gretty:appRun`.

## Packaged overlay verification

`./gradlew overlayIntegrationTest` deploys a test overlay JAR in a real Jetty WAR and verifies discovery, precedence, XSLT, static resources, web fragments, diagnostics, and reload retention over HTTP. It also runs as part of `check` and `build`. See [the deployment test documentation](tests/overlay-deployment/README.md) for setup and coverage.

## Code conventions

- Every package declares `@NullMarked` in its `package-info.java` (JSpecify); annotate with `@Nullable` only where `null` is allowed.
- The core stays annotation-free and its only runtime dependencies are `pso-xmlwriter` and the SLF4J API; see [ADR-0002](docs/decisions/0002-keep-the-core-annotation-free.md) and [ADR-0003](docs/decisions/0003-keep-integrations-optional.md).
- Utility classes are named after their topic in the plural (`HttpResponses`, `ETags`), without a `Utils` suffix.
- New code should pass SonarCloud analysis.

## Documentation

Documentation lives in the repository and changes with the code:

| Change | Update |
|---|---|
| Any user-visible change | [`CHANGELOG.md`](CHANGELOG.md), under Unreleased |
| A change applications must act on | [`docs/upgrading.md`](docs/upgrading.md) |
| New or changed behavior | The relevant guide in [`docs/guide/`](docs/guide/) or page in [`docs/reference/`](docs/reference/) |
| A roadmap theme's design or status | Its document in [`docs/design/`](docs/design/) and [`ROADMAP.md`](ROADMAP.md) |
| A decision that applies across themes | A new record in [`docs/decisions/`](docs/decisions/) |
