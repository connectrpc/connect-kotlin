plugins {
    kotlin("jvm")
    java
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
        srcDir("src/main/java/")
    }
}

dependencies {
    implementation(libs.protobuf.javalite)
    api(project(":extensions:google-javalite"))

    implementation(project(":okhttp"))
}
