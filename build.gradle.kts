import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.git)
    alias(libs.plugins.spotless)
}

apply(plugin = "com.vanniktech.maven.publish.base")

// The releaseVersion property is set on official releases in the release.yml workflow.
// If not specified, we attempt to calculate a snapshot version based on the last tagged release.
// So if the local build's last tag was v0.1.9, this will set snapshotVersion to 0.1.10-SNAPSHOT.
// If this fails for any reason, we'll fall back to using 0.0.0-SNAPSHOT version.
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()
var snapshotVersion = "0.0.0-SNAPSHOT"
val matchResult = """^v(\d+)\.(\d+)\.(\d+)$""".toRegex().matchEntire(details.lastTag)
if (matchResult != null) {
    val (major, minor, patch) = matchResult.destructured
    snapshotVersion = "$major.$minor.${patch.toInt() + 1}-SNAPSHOT"
}
val releaseVersion = project.findProperty("releaseVersion") as String? ?: snapshotVersion

buildscript {
    dependencies {
        classpath(libs.dokka.core)
        classpath(libs.maven.plugin)
        classpath(libs.android.plugin)
        classpath(libs.dokka.plugin)
        classpath(libs.kotlin.plugin)
        classpath(libs.spotless)
    }
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

allprojects {
    version = releaseVersion
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    apply(plugin = "com.diffplug.spotless")
    spotless {
        isEnforceCheck = false // Disables lint on gradle builds.
        kotlin {
            ktlint().editorConfigOverride(
                mapOf(
                    "ktlint_experimental" to "enabled",
                    // Disabled until https://github.com/pinterest/ktlint/pull/2273 is released
                    "ktlint_standard_function-expression-body" to "disabled",
                ),
            )
            target("**/*.kt")
        }
        kotlinGradle {
            ktlint().editorConfigOverride(
                mapOf(
                    "ktlint_experimental" to "enabled",
                    // Disabled until https://github.com/pinterest/ktlint/pull/2273 is released
                    "ktlint_standard_function-expression-body" to "disabled",
                ),
            )
            target("**/*.kts")
        }
    }
    tasks.withType<Jar>().configureEach {
        if (name == "jar") {
            val parentName = project.parent?.name ?: ""
            val resolvedName = if (parentName != "connect-kotlin") {
                "${project.name}-$parentName"
            } else {
                project.name
            }
            this.archiveBaseName.set(resolvedName)
            manifest {
                attributes("Automatic-Module-Name" to resolvedName)
                attributes("Implementation-Version" to releaseVersion)
            }
        }
    }
    tasks.withType<DokkaTask>().configureEach {
        dokkaSourceSets.configureEach {
            skipDeprecated.set(true)
            jdkVersion.set(8)
        }
    }
    plugins.withId("com.vanniktech.maven.publish.base") {
        configure<MavenPublishBaseExtension> {
            val isAutoReleased = project.hasProperty("signingInMemoryKey")
            publishToMavenCentral(SonatypeHost.S01)
            if (isAutoReleased) {
                signAllPublications()
            }
            pom {
                description.set("Simple, reliable, interoperable. A better RPC.")
                name.set("connect-library") // This is overwritten in subprojects.
                group = "com.connectrpc"
                url.set("https://github.com/connectrpc/connect-kotlin")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("connectrpc")
                        name.set("The Connect Authors")
                    }
                }
                scm {
                    url.set("https://github.com/connectrpc/connect-kotlin")
                    connection.set("scm:git:https://github.com/connectrpc/connect-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/connectrpc/connect-kotlin.git")
                }
            }
        }
    }
}

subprojects {
    tasks.withType<KotlinJvmCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            languageVersion.set(KotlinVersion.KOTLIN_1_7)
            apiVersion.set(KotlinVersion.KOTLIN_1_7)
            if (JavaVersion.current().isJava9Compatible && project.name != "android") {
                freeCompilerArgs.add("-Xjdk-release=1.8")
            }
        }
    }
    tasks.withType<JavaCompile> {
        val defaultArgs = listOf("-Xdoclint:none", "-Xlint:none", "-nowarn")
        if (JavaVersion.current().isJava9Compatible) {
            doFirst {
                options.compilerArgs = listOf("--release", "8") + defaultArgs
            }
        } else {
            options.compilerArgs = defaultArgs
        }
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        options.encoding = Charsets.UTF_8.toString()
    }
}
