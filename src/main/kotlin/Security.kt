package at.ac.hcw.se

import at.ac.hcw.se.database.UserService
import at.ac.hcw.se.dto.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

fun Application.configureSecurity(userService: UserService) {
    install(Sessions) {
        cookie<UserSession>("USER_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }
    install(Authentication) {
        session<UserSession>("user-session") {
            validate { session ->
                if (userService.isSessionValid(session.userId)) session else null
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
            }
        }
        session<UserSession>("admin-session") {
            validate { session ->
                if (userService.isSessionValid(session.userId) && session.isAdmin) session else null
            }
            challenge {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin privileges required"))
            }
        }
    }
}
