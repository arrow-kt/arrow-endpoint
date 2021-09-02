import io.ktor.application.Application

public fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

public fun Application.module() {
  endpointModule()
}
