plugins { war }
description = "Non-published real-container overlay deployment tests"
repositories { mavenCentral() }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }

// These artifacts belong only in WEB-INF/lib, never on the test runner's classpath.
val deploymentRuntime by configurations.creating
val integrationTestSource = sourceSets.create("integrationTest")
dependencies {
  deploymentRuntime(project(":tests:overlay-fixture"))
  deploymentRuntime(libs.jackson.core)
  deploymentRuntime(libs.saxon.he)
  deploymentRuntime(libs.slf4j.simple)
  add(integrationTestSource.implementationConfigurationName, "org.eclipse.jetty:jetty-webapp:10.0.17")
  add(integrationTestSource.implementationConfigurationName, libs.junit.jupiter)
  add(integrationTestSource.implementationConfigurationName, libs.gson)
  add(integrationTestSource.runtimeOnlyConfigurationName, libs.junit.platform.launcher)
  add(integrationTestSource.runtimeOnlyConfigurationName, libs.slf4j.simple)
}
val deploymentWar = tasks.named<War>("war") {
  from(deploymentRuntime) { into("WEB-INF/lib") }
}
val integrationTest = tasks.register<Test>("integrationTest") {
  description = "Deploy a packaged overlay WAR to Jetty and verify it over HTTP"
  group = "verification"
  testClassesDirs = integrationTestSource.output.classesDirs
  classpath = integrationTestSource.runtimeClasspath
  useJUnitPlatform()
  dependsOn(deploymentWar)
  inputs.file(deploymentWar.flatMap { it.archiveFile })
  doFirst { systemProperty("overlay.war", deploymentWar.get().archiveFile.get().asFile.absolutePath) }
}
tasks.named("check") { dependsOn(integrationTest) }
