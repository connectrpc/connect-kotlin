import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    application
    java
    kotlin("jvm")
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
}

sourceSets {
    java.sourceSets["main"].java {
        srcDir("src/main/java/generated")
    }
}

dependencies {
    implementation(libs.okhttp.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.assertj)
    implementation(libs.protobuf.kotlin)
    implementation(project(":crosstests:common"))
    implementation(project(":extensions:google-java"))
    implementation(project(":okhttp"))

    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.kotlin.coroutines.core)
}

configure<SpotlessExtension> {
    kotlin {
        targetExclude("**/generated/**/*.kt")
    }
}
