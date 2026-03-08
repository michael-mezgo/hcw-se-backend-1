package at.ac.hcw.se

import at.ac.hcw.se.database.DatabaseSessionStorage
import at.ac.hcw.se.database.KotlinxJsonSessionSerializer
import at.ac.hcw.se.database.UserService
import at.ac.hcw.se.dto.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

fun Application.configureSecurity(userService: UserService, sessionStorage: DatabaseSessionStorage) {
    install(Sessions) {
        cookie<UserSession>("USER_SESSION", sessionStorage) {
            serializer = KotlinxJsonSessionSerializer(UserSession.serializer())
            cookie.extensions["SameSite"] = "lax"
        }
    }
    install(Authentication) {
        session<UserSession>("user-session") {
            validate { session -> session }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
            }
        }
        session<UserSession>("admin-session") {
            validate { session -> session.takeIf { it.isAdmin } }
            challenge {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin privileges required"))
            }
        }
    }
}
