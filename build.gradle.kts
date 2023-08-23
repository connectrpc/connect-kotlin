import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "com.vanniktech.maven.publish.base")

val releaseVersion = project.findProperty("releaseVersion") as String? ?: "0.0.0-SNAPSHOT"

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
                group = "build.buf"
                version = releaseVersion
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
            if (JavaVersion.current().isJava9Compatible) {
                freeCompilerArgs = listOf("-Xjdk-release=1.8", "-opt-in=kotlin.RequiresOptIn")
            }
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
