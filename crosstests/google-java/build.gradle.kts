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
    jar {
        manifest {
            attributes(mapOf("Main-Class" to "build.buf.connect.crosstest.Main"))
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        // This line of code recursively collects and copies all of a project's files
        // and adds them to the JAR itself. One can extend this task, to skip certain
        // files or particular types at will
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
        from(contents)
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
    implementation(project(":apache"))
}

configure<SpotlessExtension> {
    kotlin {
        targetExclude("**/generated/**/*.kt")
    }
}
