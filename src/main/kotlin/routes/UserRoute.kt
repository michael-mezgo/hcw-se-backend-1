package at.ac.hcw.se.routes

import at.ac.hcw.se.database.UserService
import at.ac.hcw.se.dto.LoginResponse
import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserRegistration
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.JwtPrincipal
import at.ac.hcw.se.dto.UserUpdate
import at.ac.hcw.se.generateToken
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.patch
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException

// Note: auth routes (register/login/logout) are kept in the same file as user management
// routes because they are tightly coupled to the User domain. Extract to AuthRoute.kt if
// the project grows and separation of concerns becomes more important.

fun Application.configureUserRoutes(userService: UserService) {
    routing {

        // ── Authentication ──────────────────────────────────────────────────────

        route("/auth") {

            post("/register", {
                tags("Auth")
                summary = "Register a new user"
                request { body<UserRegistration> { description = "Registration data" } }
                response {
                    HttpStatusCode.Created to { description = "User created"; body<Map<String, Int>>() }
                    HttpStatusCode.Conflict to { description = "Username or email already taken" }
                }
            }) {
                val registration = call.receive<UserRegistration>()
                try {
                    val id = userService.create(registration)
                    call.respond(HttpStatusCode.Created, mapOf("id" to id))
                } catch (e: ExposedSQLException) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Username or email already taken"))
                }
            }

            post("/login", {
                tags("Auth")
                summary = "Login with username and password"
                request { body<UserLoginRequest> { description = "Login credentials" } }
                response {
                    HttpStatusCode.OK to { description = "Login successful, JWT token returned"; body<LoginResponse>() }
                    HttpStatusCode.Unauthorized to { description = "Invalid credentials" }
                }
            }) {
                val credentials = call.receive<UserLoginRequest>()
                val user = userService.findByCredentials(credentials.username, credentials.password)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid username or password"))
                    return@post
                }
                val token = generateToken(user.id, user.username, user.isAdmin)
                call.respond(HttpStatusCode.OK, LoginResponse(userId = user.id, isAdmin = user.isAdmin, token = token))
            }

        }

        // ── User management ─────────────────────────────────────────────────────

        authenticate("user-jwt") {
            route("/users/me") {

                get({
                    tags("Users")
                    summary = "Get own user profile"
                    description = "Returns the profile of the authenticated user."
                    response {
                        HttpStatusCode.OK to { description = "User profile"; body<UserResponse>() }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val principal = call.principal<JwtPrincipal>()!!
                    val user = userService.read(principal.userId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    call.respond(HttpStatusCode.OK, user)
                }

                patch({
                    tags("Users")
                    summary = "Update own user profile"
                    description = "Updates email, password, or other profile fields. All fields are optional."
                    request { body<UserUpdate> { description = "Fields to update (all optional)" } }
                    response {
                        HttpStatusCode.OK to { description = "User updated successfully" }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val principal = call.principal<JwtPrincipal>()!!
                    val update = call.receive<UserUpdate>()
                    val updated = userService.update(principal.userId, update)
                    if (!updated)
                        return@patch call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    call.respond(HttpStatusCode.OK, mapOf("message" to "User updated successfully"))
                }

                delete({
                    tags("Users")
                    summary = "Delete own user account"
                    description = "Deletes the authenticated user's account. Client should discard the JWT token."
                    response {
                        HttpStatusCode.NoContent to { description = "Account deleted" }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val principal = call.principal<JwtPrincipal>()!!
                    val deleted = userService.delete(principal.userId)
                    if (!deleted)
                        return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
