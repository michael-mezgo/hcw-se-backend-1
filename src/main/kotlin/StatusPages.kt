package at.ac.hcw.se

import at.ac.hcw.se.service.ServiceException
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ServiceException> { call, cause ->
            call.respond(cause.status, mapOf("error" to cause.message))
        }
    }
}
