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
include("core", "examples", "schema-reflect", "test")

// clients
include(":clients:ktor-client", ":clients:http4k-client", ":clients:spring-web-client", ":clients:spring-web-flux-client")

// servers
include(":servers:ktor-server", ":servers:spring-web-server")

// docs
include(":docs:openapi-docs")
