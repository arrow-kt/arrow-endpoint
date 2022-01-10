package springboot

import arrow.core.Either
import arrow.endpoint.*
import arrow.endpoint.server.ServerEndpoint
import arrow.endpoint.spring.server.routerFunction
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse

@SpringBootApplication
open class SpringBoot

fun main(args: Array<String>) {
  runApplication<SpringBoot>(*args)
}

val helloSpring: Endpoint<Unit, String, String> =
  Endpoint
    .get()
    .errorOutput(ArrowEndpoint.stringBody())
    .input(ArrowEndpoint.fixedPath("hello"))
    .output(ArrowEndpoint.stringBody())

@Configuration
open class SpringBootConfig {

  @Bean
  open fun getHelloRoute(): RouterFunction<ServerResponse> {
    val routerFunction: RouterFunction<ServerResponse> = routerFunction(ServerEndpoint(helloSpring) {
      if ((1..2).random() == 1)
        Either.Right("Hello Spring Boot !")
      else
        Either.Left("oops, it's just not my day...")

    })
    return routerFunction
  }
}
