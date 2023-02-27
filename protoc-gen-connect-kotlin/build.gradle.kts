plugins {
    application
    java
    kotlin("jvm")
}

tasks {
    jar {
        manifest {
            attributes(mapOf("Main-Class" to "build.buf.protocgen.connect.Main"))
        }
        duplicatesStrategy = DuplicatesStrategy.WARN
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

dependencies {
    implementation(project(":okhttp"))
    implementation(libs.protobuf.java)
    implementation(libs.kotlinpoet)

    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.kotlin.coroutines.core)
}
