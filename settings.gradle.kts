rootProject.name = "connect-kotlin"

include(":conformance:client")
include(":conformance:client:google-java")
include(":conformance:client:google-javalite")
if (extra.has("skipAndroid") && extra.get("skipAndroid").toString().toBoolean()) {
    println("Skipping Android build (skipAndroid=true)")
} else {
    include(":examples:android")
}
include(":examples:generated-google-java")
include(":examples:generated-google-javalite")
include(":examples:kotlin-google-java")
include(":examples:kotlin-google-javalite")
include(":extensions:google-java")
include(":extensions:google-javalite")
include(":library")
include(":library-server")
include(":ktor-server")
include(":okhttp")
include(":protoc-gen-connect-kotlin")
