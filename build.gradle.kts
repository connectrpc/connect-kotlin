import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.git)
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
    snapshotVersion = "${major}.${minor}.${patch.toInt()+1}-SNAPSHOT"
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
    }
}

allprojects {
    version = releaseVersion
    repositories {
        mavenCentral()
        google()
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
            reportUndocumented.set(false)
            skipDeprecated.set(true)
            jdkVersion.set(8)
            perPackageOption {
                matchingRegex.set("build\\.buf.*")
                suppress.set(true)
            }
        }
        if (name == "dokkaGfm") {
            outputDirectory.set(project.file("${project.rootDir}/docs/3.x"))
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
                url.set("https://github.com/bufbuild/connect-kotlin")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("bufbuild")
                        name.set("Buf Technologies")
                    }
                }
                scm {
                    url.set("https://github.com/bufbuild/connect-kotlin")
                    connection.set("scm:git:https://github.com/bufbuild/connect-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/bufbuild/connect-kotlin.git")
                }
            }
        }
    }
}

subprojects {
    /*
     * By default, the plugin will execute for all subdirectories as an independent project.
     * This means that /examples will be considered one project and /examples/kotlin-protoc-gen-java will
     * be considered another project. The linter ends up executing both independently which
     * circumvents the exclusion rules specified on the actual project.
     *
     * The workaround here is to check if the project has subprojects and only execute
     * when it's a node.
     */
    if (project.childProjects.isEmpty()) {
        apply(plugin = "com.diffplug.spotless")
        configure<SpotlessExtension> {
            setEnforceCheck(false) // Disables lint on gradle builds.
            kotlin {
                ktlint(libs.versions.ktlint.get())
                    .setUseExperimental(true)
                target("**/*.kt")
            }
            kotlinGradle {
                ktlint(libs.versions.ktlint.get())
                target("**/*.kts")
            }
        }
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            languageVersion = "1.6"
            apiVersion = "1.6"
        }
    }
    tasks.withType<JavaCompile> {
        val defaultArgs = listOf("-Xdoclint:none", "-Xlint:none", "-nowarn")
        if (JavaVersion.current().isJava9Compatible) doFirst {
            options.compilerArgs = listOf("--release", "8") + defaultArgs
        } else {
            options.compilerArgs = defaultArgs
        }
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        options.encoding = Charsets.UTF_8.toString()
    }
}
