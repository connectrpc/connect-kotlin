plugins {
    kotlin("jvm")
    java
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
        srcDir("src/main/java/")
    }
}

dependencies {
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.java.util)
    api(project(":extensions:google-java"))

    implementation(project(":okhttp"))
}
