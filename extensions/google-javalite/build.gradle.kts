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
    testImplementation(libs.assertj)
    testImplementation(libs.junit)

    implementation(project(":okhttp"))
    api(libs.okio.core)
    api(libs.protobuf.javalite)
    api(libs.protobuf.kotlinlite)
    implementation(libs.kotlin.reflect)
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/bufgen")
        }
    }
}

mavenPublishing {
    configure(
        KotlinJvm(javadocJar = Dokka("dokkaHtml")),
    )
}

// Workaround for overriding the published library name to "connect-kotlin".
// Otherwise, the plugin will take the library name.
extensions.getByType<PublishingExtension>().apply {
    publications
        .filterIsInstance<MavenPublication>()
        .forEach { publication ->
            publication.artifactId = "connect-kotlin-google-javalite-ext"
        }
}
