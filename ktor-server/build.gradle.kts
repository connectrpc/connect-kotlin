import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish.base")
}

kotlin {
    compilerOptions.allWarningsAsErrors.set(true)
}

dependencies {
    // Server core library
    api(project(":library-server"))

    // Ktor server
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Coroutines
    implementation(libs.kotlin.coroutines.core)

    // Test dependencies
    testImplementation(libs.assertj)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
}

mavenPublishing {
    configure(
        KotlinJvm(javadocJar = Dokka("dokkaGeneratePublicationHtml")),
    )
}

extensions.getByType<PublishingExtension>().apply {
    publications
        .filterIsInstance<MavenPublication>()
        .forEach { publication ->
            publication.artifactId = "connect-kotlin-ktor-server"
        }
}
