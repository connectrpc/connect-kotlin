import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish.base")
    alias(libs.plugins.ksp)
}

dependencies {
    testImplementation(libs.assertj)
    testImplementation(libs.junit)
    testImplementation(libs.mockito)

    // Part of API contract
    api(libs.kotlin.coroutines.core)
    api(libs.okio.core)

    implementation(libs.moshiKotlin)

    ksp(libs.moshiKotlinCodegen)
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
            publication.artifactId = "connect-kotlin"
        }
}
