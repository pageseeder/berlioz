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

  testRuntimeOnly(libs.junit.platform.launcher)
  testRuntimeOnly(libs.saxon.he)
}

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
