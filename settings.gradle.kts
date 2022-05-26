enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("libs.versions.toml"))
    }
  }

  repositories {
    mavenCentral()
  }
}

rootProject.name = "arrow-endpoint"

include(":arrow-endpoint-core", ":arrow-endpoint-examples", ":arrow-endpoint-schema-reflect", ":arrow-endpoint-test")

// clients
include(":arrow-endpoint-ktor-client", ":arrow-endpoint-http4k-client", ":arrow-endpoint-spring-web-client", ":arrow-endpoint-spring-web-flux-client")

// servers
include(":arrow-endpoint-ktor-server", ":arrow-endpoint-spring-web-server")

// docs
include(":arrow-endpoint-openapi-docs")

project(":arrow-endpoint-core").projectDir = file("core")
project(":arrow-endpoint-examples").projectDir = file("examples")
project(":arrow-endpoint-schema-reflect").projectDir = file("schema-reflect")
project(":arrow-endpoint-test").projectDir = file("test")
project(":arrow-endpoint-ktor-client").projectDir = file("clients/ktor-client")
project(":arrow-endpoint-http4k-client").projectDir = file("clients/http4k-client")
project(":arrow-endpoint-spring-web-client").projectDir = file("clients/spring-web-client")
project(":arrow-endpoint-spring-web-flux-client").projectDir = file("clients/spring-web-flux-client")
project(":arrow-endpoint-ktor-server").projectDir = file("servers/ktor-server")
project(":arrow-endpoint-spring-web-server").projectDir = file("servers/spring-web-server")
project(":arrow-endpoint-openapi-docs").projectDir = file("docs/openapi-docs")
