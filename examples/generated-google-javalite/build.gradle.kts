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
    implementation(libs.protobuf.javalite)
    api(project(":extensions:google-javalite"))

    implementation(project(":okhttp"))
}
