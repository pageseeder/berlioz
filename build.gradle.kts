plugins {
  id("java-library")
  id("maven-publish")
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.versions)
}

val title: String by project
val gitName: String by project
val website: String by project
val globalVersion = file("version.txt").readText().trim()

allprojects {
  group   = "org.pageseeder.berlioz"
  version = globalVersion

  apply(plugin = "java-library")
  apply(plugin = "maven-publish")

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

  tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
      addStringOption("Xdoclint:none", "-quiet")
      if (JavaVersion.current().isJava9Compatible) {
        addBooleanOption("html5", true)
      }
    }
  }

  publishing {
    publications {
      create<MavenPublication>("maven") {
        from(components["java"])
        artifactId = project.name
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

dependencies {
  api(libs.xmlwriter)
  implementation(libs.slf4j.api)

  compileOnly(libs.servlet.api) {
    because("This is provided by the Servlet container")
  }
  compileOnly(libs.jackson.core) {
    because("Optional dependency for JSON output using Jackson")
  }
  compileOnly(libs.gson) {
    because("Optional dependency for JSON output using Google JSON library")
  }
  compileOnly(libs.javax.json.api) {
    because("Optional dependency for JSON output using JSR 374")
  }
  compileOnly(libs.jakarta.json.api) {
    because("Optional dependency for JSON output using JSR 374")
  }
  compileOnly(libs.jdt.annotations) {
    because("Used for null safety and better Kotlin interop")
  }

  testImplementation(libs.junit)
  testImplementation(libs.slf4j.simple)
  testImplementation(libs.glassfish.javax.json)
  testImplementation(libs.glassfish.jakarta.json)
  testImplementation(libs.parsson)
  testImplementation(libs.jackson.core)
  testImplementation(libs.gson)
}

tasks.wrapper {
  gradleVersion = "8.14.4"
  distributionType = Wrapper.DistributionType.ALL
}

jreleaser {
  configFile.set(file("jreleaser.toml"))
}
