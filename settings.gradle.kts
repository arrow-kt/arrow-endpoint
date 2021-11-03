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
include("clients", ":clients:ktor", ":clients:http4k", ":clients:spring-web", ":clients:spring-web-flux")

// servers
include("servers", ":servers:ktor", ":servers:spring-web")

// docs
include("docs", ":docs:openapi")
