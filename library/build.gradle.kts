import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish.base")
    alias(libs.plugins.ksp)
}

kotlin {
    jvm {
        compilerOptions.allWarningsAsErrors.set(true)
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                // Part of API contract
                api(libs.kotlin.coroutines.core)
                api(libs.okio.core)
                api(libs.ktor.http)

                implementation(libs.moshiKotlin)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.assertj)
                implementation(libs.junit)
                implementation(libs.mockito)
            }
        }
    }
}

dependencies {
    add("kspJvm", libs.moshiKotlinCodegen)
}

mavenPublishing {
    configure(
        KotlinMultiplatform(javadocJar = Dokka("dokkaGeneratePublicationHtml")),
    )
}

// Override published artifact names.
// KMP generates multiple publications with different suffixes.
extensions.getByType<PublishingExtension>().apply {
    publications
        .filterIsInstance<MavenPublication>()
        .forEach { publication ->
            publication.artifactId = when (publication.name) {
                "kotlinMultiplatform" -> "connect-kotlin"
                "jvm" -> "connect-kotlin-jvm"
                else -> "connect-kotlin-${publication.name}"
            }
        }
}
