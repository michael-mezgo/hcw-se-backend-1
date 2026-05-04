package at.ac.hcw.se.routes

import at.ac.hcw.se.BlobStorageService
import at.ac.hcw.se.service.UserService
import at.ac.hcw.se.dto.LoginResponse
import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserRegistration
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.JwtPrincipal
import at.ac.hcw.se.dto.UserUpdate
import at.ac.hcw.se.service.Auth
import at.ac.hcw.se.business.User
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

@Suppress("UNUSED_PARAMETER")
fun Application.configureUserRoutes(blobStorage: BlobStorageService? = null) {
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
                val id = Auth.register(registration)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
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
                val loginResponse = Auth.login(credentials)
                call.respond(HttpStatusCode.OK, loginResponse)
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
                    val user = call.toUser()
                    call.respond(HttpStatusCode.OK, user.toResponse())
                }

                patch({
                    tags("Users")
                    summary = "Update own user profile"
                    description = "Updates email, password, or other profile fields. All fields are optional."
                    request { body<UserUpdate> { description = "Fields to update (all optional)" } }
                    response {
                        HttpStatusCode.OK to { description = "Updated user profile"; body<UserResponse>() }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val user = call.toUser()
                    val update = call.receive<UserUpdate>()
                    user.updateProfile(update)
                    val updated = UserService.read(user.id)
                        ?: throw at.ac.hcw.se.service.ServiceException.NotFound("User not found")
                    call.respond(HttpStatusCode.OK, updated.toResponse())
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
                    val user = call.toUser()
                    user.deleteAccount()
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.toUser(): User {
    val principal = principal<JwtPrincipal>()!!
    return UserService.read(principal.userId)
        ?: throw at.ac.hcw.se.service.ServiceException.NotFound("User not found")
}
