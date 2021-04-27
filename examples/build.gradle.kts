plugins {
    id(Plugins.kotlinSerialization)
}

dependencies {
    implementation(project(Libs.core))
    implementation(project(Libs.graphQL))
    implementation(project(Libs.ktorServer))
    implementation(Libs.graphQlServer)
    implementation(Libs.ktorServerCore)
    implementation(Libs.ktorServerNetty)
    implementation(Libs.logbackClassic)
    implementation(Libs.kotlinxSerializationJson)
}
