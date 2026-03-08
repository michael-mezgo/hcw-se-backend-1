package at.ac.hcw.se.routes

import at.ac.hcw.se.database.UserService
import at.ac.hcw.se.dto.AdminUserCreate
import at.ac.hcw.se.dto.AdminUserUpdate
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.UserSession
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAdminRoutes(userService: UserService) {
    routing {
        authenticate("admin-session") {
            route("/admin/users") {

                get({
                    tags("Admin")
                    summary = "List all users"
                    description = "Returns a list of all registered users. Requires admin privileges."
                    response {
                        HttpStatusCode.OK to { description = "List of all users"; body<List<UserResponse>>() }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                    }
                }) {
                    call.respond(HttpStatusCode.OK, userService.listAll())
                }

                post({
                    tags("Admin")
                    summary = "Create a user"
                    description = "Creates a new user account. Admins can optionally grant admin privileges. Requires admin privileges."
                    request { body<AdminUserCreate> { description = "User data" } }
                    response {
                        HttpStatusCode.Created to { description = "User created"; body<Map<String, Int>>() }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.Conflict to { description = "Username or email already taken" }
                    }
                }) {
                    val dto = call.receive<AdminUserCreate>()
                    val id = userService.adminCreate(dto)
                    call.respond(HttpStatusCode.Created, mapOf("id" to id))
                }

                get("/{id}", {
                    tags("Admin")
                    summary = "Get any user profile"
                    description = "Returns the profile of the specified user. Requires admin privileges."
                    request { pathParameter<Int>("id") { description = "User ID" } }
                    response {
                        HttpStatusCode.OK to { description = "User profile"; body<UserResponse>() }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                    val user = userService.read(id)
                        ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))

                    call.respond(HttpStatusCode.OK, user)
                }

                put("/{id}", {
                    tags("Admin")
                    summary = "Update any user"
                    description = "Updates profile fields, password, admin status, or lock status of any user. All fields are optional. Requires admin privileges."
                    request {
                        pathParameter<Int>("id") { description = "User ID" }
                        body<AdminUserUpdate> { description = "Fields to update (all optional)" }
                    }
                    response {
                        HttpStatusCode.OK to { description = "User updated successfully" }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                    val dto = call.receive<AdminUserUpdate>()
                    userService.adminUpdate(id, dto)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "User updated successfully"))
                }

                delete("/{id}", {
                    tags("Admin")
                    summary = "Delete any user"
                    description = "Permanently deletes the specified user account. Requires admin privileges."
                    request { pathParameter<Int>("id") { description = "User ID" } }
                    response {
                        HttpStatusCode.NoContent to { description = "User deleted" }
                        HttpStatusCode.Unauthorized to { description = "Not authenticated" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required or self-deletion attempted" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val session = call.principal<UserSession>()!!

                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                    if (session.userId == id)
                        return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admins cannot delete their own account"))

                    userService.delete(id)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
