import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    kotlin("jvm")
    java
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish.base")
}

sourceSets {
    java.sourceSets["main"].java {
        srcDir("src/main/java/generated")
    }
}

dependencies {
    testImplementation(libs.assertj)
    testImplementation(libs.junit)

    implementation(project(":okhttp"))
    api(libs.okio.core)
    api(libs.protobuf.java)
    api(libs.protobuf.java.util)
    implementation(libs.kotlin.reflect)
}

configure<MavenPublishBaseExtension> {
    configure(
        KotlinJvm(javadocJar = Dokka("dokkaGfm")),
    )
}

// Workaround for overriding the published library name to "connect-kotlin".
// Otherwise, the plugin will take the library name.
extensions.getByType<PublishingExtension>().apply {
    publications
        .filterIsInstance<MavenPublication>()
        .forEach { publication ->
            publication.artifactId = "connect-kotlin-google-java-ext"
        }
}
