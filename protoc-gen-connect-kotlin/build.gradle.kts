import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    application
    java
    kotlin("jvm")

    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish.base")
}

tasks {
    jar {
        manifest {
            attributes(mapOf("Main-Class" to "build.buf.protocgen.connect.Main"))
        }
        duplicatesStrategy = DuplicatesStrategy.WARN
        // This line of code recursively collects and copies all of a project's files
        // and adds them to the JAR itself. One can extend this task, to skip certain
        // files or particular types at will
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
        from(contents)
    }
}

dependencies {
    implementation(project(":okhttp"))
    implementation(libs.protobuf.java)
    implementation(libs.kotlinpoet)

    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.kotlin.coroutines.core)
}

configure<MavenPublishBaseExtension> {
    configure(
        KotlinJvm(javadocJar = Dokka("dokkaGfm"))
    )
}

// Workaround for overriding the published library name to "connect-kotlin-okhttp".
// Otherwise, the plugin will take the library name.
extensions.getByType<PublishingExtension>().apply {
    publications
        .filterIsInstance<MavenPublication>()
        .forEach { publication ->
            publication.artifactId = "protoc-gen-connect-kotlin"
        }
}
