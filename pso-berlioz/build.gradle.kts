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

  testImplementation(libs.junit)
  testImplementation(libs.servlet.api)
  testImplementation(libs.slf4j.simple)
  testImplementation(libs.glassfish.javax.json)
  testImplementation(libs.glassfish.jakarta.json)
  testImplementation(libs.parsson)
  testImplementation(libs.jackson.core)
  testImplementation(libs.gson)
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
