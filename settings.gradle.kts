rootProject.name = "thool"
include("thool-model", "core", "graphql", "examples", "schema-reflect")

// clients
include("clients", ":clients:ktor-client", ":clients:http4k-client")

// servers
include("ktor-server")
