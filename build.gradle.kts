plugins {
  base
  alias(libs.plugins.cyclonedx) apply false
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.sonarqube)
  alias(libs.plugins.versions)
}

val title: String by project
val gitName: String by project
val website: String by project
val globalVersion = file("version.txt").readText().trim()
val publishedProjectNames = setOf("pso-berlioz", "pso-berlioz-kickstart", "pso-berlioz-mock")
val moduleNames = mapOf(
  "pso-berlioz"           to "org.pageseeder.berlioz",
  "pso-berlioz-kickstart" to "org.pageseeder.berlioz.kickstart",
  "pso-berlioz-mock"      to "org.pageseeder.berlioz.mock"
)

allprojects {
  group   = "org.pageseeder.berlioz"
  version = globalVersion
}

configure(subprojects.filter { it.name in publishedProjectNames }) {
  apply(plugin = "java-library")
  apply(plugin = "jacoco")
  apply(plugin = "maven-publish")
  apply(plugin = "org.cyclonedx.bom")

  configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
    }
  }

  repositories {
    mavenCentral()
  }

  tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENCE.md")) {
      into("META-INF")
    }
  }

  tasks.named<Jar>("jar") {
    manifest {
      attributes(
        "Automatic-Module-Name"    to moduleNames.getValue(project.name),
        "Implementation-Vendor"    to "Allette Systems",
        "Implementation-Vendor-Id" to "org.pageseeder.berlioz",
        "Implementation-Title"     to (project.findProperty("title") as? String ?: project.name),
        "Implementation-Version"   to globalVersion,
        "Implementation-URL"       to website,
        "Bundle-License"           to "Apache-2.0",
        "Build-Jdk-Spec"           to "11"
      )
    }
  }

  tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
      xml.required.set(true)
      html.required.set(false)
    }
  }

  tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
  }

  tasks.withType<org.cyclonedx.gradle.CyclonedxDirectTask>().configureEach {
    xmlOutput.unsetConvention()
  }

  tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
      if (JavaVersion.current().isJava9Compatible) {
        addBooleanOption("html5", true)
      }
    }
  }

  configure<PublishingExtension> {
    publications {
      create<MavenPublication>("maven") {
        from(components["java"])
        artifactId = project.name
        artifact(
          tasks.named<org.cyclonedx.gradle.CyclonedxDirectTask>("cyclonedxDirectBom")
            .flatMap { it.jsonOutput }
        ) {
          classifier = "cyclonedx"
          extension = "json"
        }
        pom {
          // Read lazily: subproject scripts set the title and description after this block runs
          name.set(provider { project.findProperty("title") as? String ?: project.name })
          description.set(provider { project.description })
          url.set(website)
          licenses {
            license {
              name.set("The Apache Software License, Version 2.0")
              url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }
          organization {
            name.set("Allette Systems")
            url.set("https://www.allette.com.au")
          }
          scm {
            url.set("git@github.com:pageseeder/${gitName}.git")
            connection.set("scm:git:git@github.com:pageseeder/${gitName}.git")
            developerConnection.set("scm:git:git@github.com:pageseeder/${gitName}.git")
          }
          developers {
            developer { name.set("Christophe Lauret"); email.set("clauret@weborganic.com") }
            developer { name.set("Jean-Baptiste Reure"); email.set("jbreure@weborganic.com") }
            developer { name.set("Carlos Cabral"); email.set("ccabral@allette.com.au") }
          }
        }
      }
    }

    repositories {
      maven {
        url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
      }
    }
  }
}

sonarqube {
  properties {
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.organization", "pageseeder")
    property("sonar.projectKey", "pageseeder_berlioz")
    property("sonar.token", providers.gradleProperty("sonarcloud.login").getOrElse(""))
    property("sonar.coverage.exclusions", "samples/**")
    property("sonar.coverage.jacoco.xmlReportPaths",
      subprojects.filter { it.name in publishedProjectNames }
        .map { "${it.layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml" }
        .joinToString(",")
    )
  }
}

tasks.wrapper {
  gradleVersion = "8.14.5"
  distributionType = Wrapper.DistributionType.ALL
}

jreleaser {
  configFile.set(file("jreleaser.toml"))
}

val overlayIntegrationTest = tasks.register("overlayIntegrationTest") {
  description = "Verify packaged overlay deployment in a real servlet container"
  group = "verification"
  dependsOn(":tests:overlay-deployment:integrationTest")
}
tasks.named("check") { dependsOn(overlayIntegrationTest) }
