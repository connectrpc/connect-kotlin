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

application {
    mainClass.set("build.buf.protocgen.connect.Main")
}

tasks {
    jar {
        manifest {
            attributes(mapOf("Main-Class" to application.mainClass.get()))
        }
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/**/*")
        }
    }
}

dependencies {
    implementation(project(":library"))
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

// Workaround for overriding the published library name.
// Otherwise, the plugin will take the library name.
extensions.getByType<PublishingExtension>().apply {
    publications
        .filterIsInstance<MavenPublication>()
        .forEach { publication ->
            publication.artifactId = "protoc-gen-connect-kotlin"
        }
}
