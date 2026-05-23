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

allprojects {
  group   = "org.pageseeder.berlioz"
  version = globalVersion
}

subprojects {
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

  tasks.named<Jar>("jar") {
    manifest {
      attributes(
        "Implementation-Vendor"  to "Allette Systems",
        "Implementation-Title"   to (project.findProperty("title") as? String ?: project.name),
        "Implementation-Version" to globalVersion
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

  tasks.named("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
  }

  tasks.withType<org.cyclonedx.gradle.CyclonedxDirectTask>().configureEach {
    xmlOutput.unsetConvention()
  }

  tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
      addStringOption("Xdoclint:none", "-quiet")
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
          name.set(project.findProperty("title") as? String ?: project.name)
          description.set(project.description)
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
    property("sonar.coverage.jacoco.xmlReportPaths",
      subprojects.map { "${it.layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml" }
        .joinToString(",")
    )
  }
}

tasks.wrapper {
  gradleVersion = "8.14.4"
  distributionType = Wrapper.DistributionType.ALL
}

jreleaser {
  configFile.set(file("jreleaser.toml"))
}
