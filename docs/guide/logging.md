# Logging

Berlioz logs through [SLF4J](https://www.slf4j.org/). Add an SLF4J binding to the application, such as Logback or `slf4j-simple`.

## Logging from generators

Declare a logger in the generator class:

```java
private static final Logger LOGGER = LoggerFactory.getLogger(MyGenerator.class);
```

and log with placeholders rather than string concatenation:

```java
LOGGER.debug("Loading {}", id);
LOGGER.warn("Unable to read {}", path, ex);
```

## Configuration by mode

When Logback is on the classpath, Berlioz configures it at startup from the first file found among:

1. `logback-<mode>.xml` in the application data `config` folder (`-Dberlioz.appdata`);
2. `logback-<mode>.xml` in `WEB-INF/config/`;
3. `logback.xml` in `WEB-INF/config/`.

Logback 1.3 or later is supported. Berlioz uses these files only when SLF4J is bound to Logback; otherwise it says so at startup and ignores them.

Berlioz resets Logback before loading a file, and reports what happened on the console (lines starting with `[BERLIOZ_INIT]`):

- If Logback reports errors while loading a file, for example an appender class that cannot be found, Berlioz prints them and keeps the file, as Logback does. Logback may then have applied only part of the configuration, or none of it.
- If a file cannot be loaded, for example because its XML is malformed, Berlioz prints the errors and tries the next file. When a later file loads, Berlioz also logs a warning naming the file that failed.
- If no file that exists can be loaded, Berlioz logs to the console at `INFO` level and logs a warning, so that logging is not lost.

If none is found, Berlioz looks for Log4J configuration in the same order: `log4j-<mode>.properties`, then `log4j.properties`. Loading Log4j 1 files is deprecated and will be removed in 1.0: Berlioz prints a deprecation notice when it loads one, and a second notice when SLF4J uses another backend, since the file may then not configure Berlioz's logs. Other logging frameworks must be configured independently, for example through their own default configuration file. See [configuration](configuration.md#modes) for modes.

If Berlioz finds none of its files, it leaves the backend's own configuration intact.

Mode-based configuration happens while Berlioz initializes, so the container's messages and the messages logged earlier during startup use the backend's defaults.

What Berlioz did at startup, including the backend, which file is in use and which failed before it, is available to administration tools through the `GetLoggingConfig` generator (see [administration](administration.md#logging-getloggingconfig)).

## Backends

Berlioz identifies the backend from the SLF4J logger factory in use, not from the libraries on the classpath.

| Backend | SLF4J provider | What Berlioz does |
|---|---|---|
| `logback` | `logback-classic` | Loads its mode-based Logback files (above) |
| `log4j2` | `log4j-slf4j2-impl` | Nothing; reports `log4j2.configurationFile` if set |
| `jul` | `slf4j-jdk14` | Nothing; reports `java.util.logging.config.file` if set |
| `simple` | `slf4j-simple` | Nothing |
| `nop` | none found | Nothing |
| `unknown` | any other, such as reload4j | Nothing |

With any backend other than Logback, Berlioz still checks whether its Logback files exist and prints a notice for each one it ignores. The legacy Log4j 1 files are loaded whatever the backend (see above).

## Leaving logging to the backend

Set `berlioz.logging` to `native` so that Berlioz looks at none of its files and leaves logging to the backend's native configuration. The default is `auto`. Any other value prints a warning and `auto` is used.

Like the mode, the option is read before logging is configured, from the first of these sources that defines it:

1. Servlet init parameter `logging`;
2. Context parameter `berlioz.logging`;
3. System property `berlioz.logging`;
4. Environment variable `BERLIOZ_LOGGING`.

Prefer the context parameter: a system property or environment variable applies to every application in the JVM.

```xml
<context-param>
  <param-name>berlioz.logging</param-name>
  <param-value>native</param-value>
</context-param>
```

Use it, for example, when Logback is shared by the container (in Tomcat's `lib` folder): resetting Logback to load a Berlioz file would then wipe the container's configuration.

## Native configuration locations

Backends can be pointed at a configuration file with a system property, such as `-Dlogback.configurationFile`. In `auto`, Berlioz files still take precedence: these properties apply to the whole JVM, so Berlioz cannot tell whether one was set for the container, another application or this one, and deferring to it would silently skip the files the application ships. When the property is set and Berlioz loads its own file anyway, it prints a notice suggesting `berlioz.logging=native`. The property and its value are reported by `GetLoggingConfig` as `native-setting`.

## java.util.logging

With `slf4j-jdk14`, configure JUL with `-Djava.util.logging.config.file=…`. JUL is configured for the whole JVM: its configuration is shared by every web application unless the container isolates them, as Tomcat's JULI does. Berlioz neither reconfigures JUL nor installs bridges.

## SLF4J Simple

With `slf4j-simple`, put a `simplelogger.properties` file in `WEB-INF/classes`, or set `org.slf4j.simpleLogger.*` system properties. They are read once, when SLF4J is first used, so there is no mode-based configuration.

## Sample Logback configurations

### Development

Log to the console, keeping chatty libraries quiet:

```xml
<configuration scan="true" scanPeriod="30 seconds">
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>[%thread] %-5level %logger{30} - %msg%n</pattern>
    </encoder>
  </appender>

  <logger name="org.pageseeder.berlioz" level="INFO"/>

  <root level="DEBUG">
    <appender-ref ref="STDOUT"/>
  </root>
</configuration>
```

### Production

Log to files, rolled over daily or when they reach 10 MB:

```xml
<configuration>
  <appender name="GENERAL" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/myproject/berlioz.log</file>
    <encoder>
      <pattern>%d{yyyy-MM-dd'T'HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <fileNamePattern>logs/myproject/berlioz-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
      <maxFileSize>10MB</maxFileSize>
      <maxHistory>30</maxHistory>
    </rollingPolicy>
  </appender>

  <root level="INFO">
    <appender-ref ref="GENERAL"/>
  </root>
</configuration>
```

See the [Logback manual](https://logback.qos.ch/manual/configuration.html) for details.
