plugins {
    id(Plugins.kotlinSerialization)
}

dependencies {
    implementation(project(Libs.core))
    implementation(project(Libs.graphQL))
    implementation(project(Libs.ktor))
    implementation(Libs.arrowCore)
    implementation(Libs.graphQlServer)
    implementation(Libs.ktorServerCore)
    implementation(Libs.ktorServerNetty)
    implementation(Libs.logbackClassic)
    implementation(Libs.kotlinxSerializationJson)
}
