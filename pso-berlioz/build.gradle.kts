description = "Berlioz framework"

val optional by configurations.creating
configurations.compileOnly { extendsFrom(optional) }

dependencies {
  api(libs.xmlwriter)
  implementation(libs.slf4j.api)

  compileOnly(libs.servlet.api) {
    because("This is provided by the Servlet container")
  }
  compileOnly(libs.jspecify) {
    because("Used for null safety annotations")
  }

  // Not 'optional': the published POM must not mention logback
  compileOnly(libs.logback.classic.min) {
    because("LogbackSetup configures logback when SLF4J is bound to it; the oldest supported version")
  }

  optional(libs.jackson.core)
  optional(libs.gson)
  optional(libs.javax.json.api)
  optional(libs.jakarta.json.api)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.servlet.api)
  testImplementation(libs.slf4j.simple)
  testImplementation(libs.glassfish.javax.json)
  testImplementation(libs.glassfish.jakarta.json)
  testImplementation(libs.parsson)
  testImplementation(libs.jackson.core)
  testImplementation(libs.gson)
  testImplementation(libs.logback.classic.min)

  testRuntimeOnly(libs.junit.platform.launcher)
  testRuntimeOnly(libs.saxon.he)
}

// Logback
// ------------------------------------------------------------------------------------------------

val logbackLatestRuntimeClasspath by configurations.creating {
  description = "Test runtime classpath with the latest logback instead of the oldest supported"
  extendsFrom(configurations.testImplementation.get(), configurations.testRuntimeOnly.get())
  isCanBeConsumed = false
  attributes {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
  }
}

dependencies {
  logbackLatestRuntimeClasspath(libs.logback.classic.latest)
}

tasks.withType<Test>().configureEach {
  // Both slf4j-simple and logback are on the test classpath: keep slf4j-simple, logback tests use their own context
  systemProperty("slf4j.provider", "org.slf4j.simple.SimpleServiceProvider")
}

tasks.named<Test>("test") {
  systemProperty("berlioz.test.logback.version", libs.versions.logback.min.get())
}

val testLogbackLatest = tasks.register<Test>("testLogbackLatest") {
  description = "Runs the logback tests with the latest logback version"
  group = "verification"
  testClassesDirs = sourceSets.test.get().output.classesDirs
  classpath = sourceSets.test.get().output + sourceSets.main.get().output + logbackLatestRuntimeClasspath
  useJUnitPlatform()
  filter { includeTestsMatching("org.pageseeder.berlioz.servlet.LogbackSetupTest") }
  systemProperty("berlioz.test.logback.version", libs.versions.logback.latest.get())
}

val checkLogbackConfinement = tasks.register("checkLogbackConfinement") {
  description = "Checks that only LogbackSetup refers to logback and that logback is not a published dependency"
  group = "verification"
  val pomTask = tasks.named<GenerateMavenPom>("generatePomFileForMavenPublication")
  val moduleTask = tasks.named<GenerateModuleMetadata>("generateMetadataFileForMavenPublication")
  dependsOn(pomTask, moduleTask)
  val sources = fileTree("src/main/java") { include("**/*.java") }
  val published = files(pomTask.map { it.destination }, moduleTask.flatMap { it.outputFile })
  inputs.files(sources)
  inputs.files(published)
  doLast {
    val offenders = sources.filter { it.name != "LogbackSetup.java" && it.readText().contains("ch.qos.logback") }
      .map { it.name } + published.filter { it.readText().contains("logback") }.map { it.name }
    if (offenders.isNotEmpty()) {
      throw GradleException("Logback must only be used by LogbackSetup and must not be published as a dependency: $offenders")
    }
  }
}

tasks.named("check") { dependsOn(testLogbackLatest, checkLogbackConfinement) }

tasks.register<Test>("generateErrorSamples") {
  description = "Generate HTML previews of all error fixtures using the failsafe XSLT. Output: build/error-samples/"
  group = "verification"
  useJUnitPlatform {
    includeTags("error-samples")
  }
  systemProperty("berlioz.generateSamples", "true")
  outputs.dir(layout.buildDirectory.dir("error-samples"))
}

publishing {
  publications {
    named<MavenPublication>("maven") {
      pom.withXml {
        @Suppress("UNCHECKED_CAST")
        val depsNode = ((asNode().get("dependencies") as groovy.util.NodeList).firstOrNull() as? groovy.util.Node)
            ?: asNode().appendNode("dependencies")
        optional.dependencies.forEach { dep ->
          depsNode.appendNode("dependency").apply {
            appendNode("groupId", dep.group)
            appendNode("artifactId", dep.name)
            appendNode("version", dep.version)
            appendNode("scope", "compile")
            appendNode("optional", "true")
          }
        }
      }
    }
  }
}
