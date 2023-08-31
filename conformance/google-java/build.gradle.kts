import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    application
    java
    kotlin("jvm")
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }
}

sourceSets {
    java.sourceSets["main"].java {
        srcDir("src/main/java/generated")
    }
}

dependencies {

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.protobuf.kotlin)
    implementation(project(":conformance:common"))
    implementation(project(":extensions:google-java"))
    implementation(project(":okhttp"))

    testImplementation(libs.okhttp.core)
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.kotlin.coroutines.core)
    testImplementation(libs.testcontainers)
}

configure<SpotlessExtension> {
    kotlin {
        targetExclude("**/generated/**/*.kt")
    }
}
