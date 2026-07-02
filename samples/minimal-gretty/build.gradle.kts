import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.kotlin.dsl.delegateClosureOf

plugins {
  war
  id("org.gretty") version "3.1.9"
}

description = "Minimal Berlioz Gretty sample application"

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

dependencies {
  implementation(project(":pso-berlioz-kickstart"))
  runtimeOnly(libs.jackson.core)
  runtimeOnly(libs.saxon.he)
  runtimeOnly(libs.slf4j.simple)
}

// Local Jetty for development.
gretty {
  enableNaming = true
  jvmArgs = listOf(
    "-Dberlioz.mode=local",
    "-Dberlioz.appdata=local/appdata"
  )
  httpPort = 8999
  httpsEnabled = true
  httpsPort = 8444
  contextPath = "/"
  servletContainer = "jetty10"
  webappCopy = delegateClosureOf<CopySpec> {
    filesMatching("WEB-INF/config/config.xml") {
      filter<ReplaceTokens>(
        "tokens" to mapOf(
          "APP_VERSION" to project.version.toString(),
          "APP_DATE" to LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        )
      )
    }
  }
}
