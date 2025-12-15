import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    kotlin("jvm")
    java
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish.base")
}

kotlin {
    compilerOptions.allWarningsAsErrors.set(true)
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/bufgen")
        }
    }
}

dependencies {
    testImplementation(libs.assertj)
    testImplementation(libs.junit)

    implementation(project(":okhttp"))
    api(libs.okio.core)
    api(libs.protobuf.java)
    api(libs.protobuf.kotlin)
    api(libs.protobuf.java.util)
    implementation(libs.kotlin.reflect)
}

mavenPublishing {
    configure(
        KotlinJvm(javadocJar = Dokka("dokkaGeneratePublicationHtml")),
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
