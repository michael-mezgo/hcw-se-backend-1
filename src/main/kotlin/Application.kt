package at.ac.hcw.se

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureDatabases()
    configureRouting()
}
