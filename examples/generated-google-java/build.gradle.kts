plugins {
    kotlin("jvm")
    java
}

tasks {
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/bufgen")
        }
    }
}

dependencies {
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.java.util)
    api(project(":extensions:google-java"))

    implementation(project(":okhttp"))
}
