rootProject.name = "connect-kotlin"

enableFeaturePreview("VERSION_CATALOGS")

include(":apache")
include(":crosstests:common")
include(":crosstests:google-java")
include(":crosstests:google-javalite")
include(":examples:android")
include(":examples:generated-google-java")
include(":examples:generated-google-javalite")
include(":examples:kotlin-google-java")
include(":examples:kotlin-google-javalite")
include(":extensions:google-java")
include(":extensions:google-javalite")
include(":library")
include(":okhttp")
include(":protoc-gen-connect-kotlin")
