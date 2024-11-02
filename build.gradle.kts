import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.4"
}

allprojects {
    apply {
        plugin("java")
        plugin("java-library")
        plugin("maven-publish")
        plugin("com.gradleup.shadow")
    }

    group = "net.azisaba.interchat"
    version = "2.10.0"

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        withJavadocJar()
        withSourcesJar()
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.azisaba.net/repository/maven-public/") }
    }

    dependencies {
        compileOnlyApi("org.jetbrains:annotations:26.0.1")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    }

    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["sourcesElements"]) {
        skip()
    }

    publishing {
        repositories {
            maven {
                name = "repo"
                credentials(PasswordCredentials::class)
                url = uri(
                    if (project.version.toString().endsWith("SNAPSHOT"))
                        project.findProperty("deploySnapshotURL") ?: System.getProperty("deploySnapshotURL", "https://repo.azisaba.net/repository/maven-snapshots/")
                    else
                        project.findProperty("deployReleasesURL") ?: System.getProperty("deployReleasesURL", "https://repo.azisaba.net/repository/maven-releases/")
                )
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks.getByName("sourcesJar"))
            }
        }
    }

    tasks {
        compileJava {
            options.encoding = "UTF-8"
        }

        test {
            useJUnitPlatform()
        }
    }
}

subprojects {
    tasks {
        shadowJar {
            archiveFileName.set("${this@subprojects.parent!!.name}-${this@subprojects.name}-${this@subprojects.version}.jar")
        }

        processResources {
            doNotTrackState("Some file should be updated every time")
            duplicatesStrategy = DuplicatesStrategy.INCLUDE

            from(sourceSets.main.get().resources.srcDirs) {
                filter(ReplaceTokens::class, mapOf("tokens" to mapOf("version" to project.version.toString())))
                filteringCharset = "UTF-8"
            }
        }
    }
}
