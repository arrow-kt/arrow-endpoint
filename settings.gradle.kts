rootProject.name = "arrow-endpoint"
include("core", "examples", "schema-reflect", "test")

// clients
include("clients", ":clients:ktor-client", ":clients:http4k-client", ":clients:spring-web-client", ":clients:spring-web-flux-client")

// servers
include("servers", ":servers:ktor-server", ":servers:spring-web-server")

// docs
include("docs", ":docs:openapi-docs")
