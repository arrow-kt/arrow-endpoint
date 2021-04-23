rootProject.name = "thool"
include("thool-model", "core", "graphql", "examples", "schema-reflect", "test")

// clients
include("clients", ":clients:ktor-client", ":clients:http4k-client")

// servers
include("servers", ":servers:ktor-server")
