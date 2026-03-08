package at.ac.hcw.se

import at.ac.hcw.se.routes.configureAdminRoutes
import at.ac.hcw.se.routes.configureUserRoutes
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureMonitoring()
    val (userService, sessionStorage) = configureDatabases()
    configureSecurity(userService, sessionStorage)
    configureUserRoutes(userService)
    configureAdminRoutes(userService)
    configureRouting()
}
