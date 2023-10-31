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
    api(libs.okhttp.core)
    implementation(libs.kotlin.coroutines.core)

    api(project(":library"))

    testImplementation(libs.assertj)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(project(":extensions:google-java"))
    testImplementation(project(":conformance:google-java"))
}

mavenPublishing {
    configure(
        KotlinJvm(javadocJar = Dokka("dokkaHtml")),
    )
}

// Workaround for overriding the published library name to "connect-kotlin-okhttp".
// Otherwise, the plugin will take the library name.
extensions.getByType<PublishingExtension>().apply {
    publications
        .filterIsInstance<MavenPublication>()
        .forEach { publication ->
            publication.artifactId = "connect-kotlin-okhttp"
        }
}
