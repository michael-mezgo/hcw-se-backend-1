package at.ac.hcw.se.routes

import at.ac.hcw.se.database.UserService
import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserRegistration
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.UserSession
import at.ac.hcw.se.dto.UserUpdate
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

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
                val id = userService.create(registration)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            post("/login", {
                tags("Auth")
                summary = "Login with username and password"
                request { body<UserLoginRequest> { description = "Login credentials" } }
                response {
                    HttpStatusCode.OK to { description = "Login successful, session cookie set"; body<Map<String, Int>>() }
                    HttpStatusCode.Unauthorized to { description = "Invalid credentials" }
                }
            }) {
                val credentials = call.receive<UserLoginRequest>()
                val user = userService.findByCredentials(credentials.username, credentials.password)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid username or password"))
                    return@post
                }
                call.sessions.set(UserSession(userId = user.id, username = user.username))
                call.respond(HttpStatusCode.OK, mapOf("userId" to user.id))
            }

            post("/logout", {
                tags("Auth")
                summary = "Logout and clear session"
                response { HttpStatusCode.NoContent to { description = "Logged out successfully" } }
            }) {
                call.sessions.clear<UserSession>()
                call.respond(HttpStatusCode.NoContent)
            }
        }

        // ── User management ─────────────────────────────────────────────────────

        route("/users") {

            get("/{id}", {
                tags("Users")
                summary = "Get user profile"
                description = "Returns the profile of the authenticated user. Users can only access their own profile."
                request { pathParameter<Int>("id") { description = "User ID" } }
                response {
                    HttpStatusCode.OK to { description = "User profile"; body<UserResponse>() }
                    HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                    HttpStatusCode.Forbidden to { description = "Access to another user's profile denied" }
                    HttpStatusCode.NotFound to { description = "User not found" }
                }
            }) {
                val session = call.sessions.get<UserSession>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                if (session.userId != id) {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                val user = userService.read(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))

                call.respond(HttpStatusCode.OK, user)
            }

            put("/{id}", {
                tags("Users")
                summary = "Update user profile"
                description = "Updates email, password, or other profile fields. All fields are optional."
                request {
                    pathParameter<Int>("id") { description = "User ID" }
                    body<UserUpdate> { description = "Fields to update (all optional)" }
                }
                response {
                    HttpStatusCode.OK to { description = "User updated successfully" }
                    HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                    HttpStatusCode.Forbidden to { description = "Access to another user's profile denied" }
                }
            }) {
                val session = call.sessions.get<UserSession>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                if (session.userId != id) {
                    return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                val update = call.receive<UserUpdate>()
                userService.update(id, update)
                call.respond(HttpStatusCode.OK, mapOf("message" to "User updated successfully"))
            }

            delete("/{id}", {
                tags("Users")
                summary = "Delete user account"
                description = "Deletes the user account and invalidates the current session."
                request { pathParameter<Int>("id") { description = "User ID" } }
                response {
                    HttpStatusCode.NoContent to { description = "Account deleted, session cleared" }
                    HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                    HttpStatusCode.Forbidden to { description = "Access to another user's account denied" }
                }
            }) {
                val session = call.sessions.get<UserSession>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                if (session.userId != id) {
                    return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                userService.delete(id)
                call.sessions.clear<UserSession>()
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
