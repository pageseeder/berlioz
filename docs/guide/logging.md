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

If none is found, Berlioz looks for Log4J configuration in the same order: `log4j-<mode>.properties`, then `log4j.properties`. Other logging frameworks must be configured independently, for example through their own default configuration file. See [configuration](configuration.md#modes) for modes.

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
