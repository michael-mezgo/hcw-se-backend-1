package at.ac.hcw.se.routes

import at.ac.hcw.se.BlobStorageService
import at.ac.hcw.se.carService
import at.ac.hcw.se.dto.*
import at.ac.hcw.se.business.Admin
import at.ac.hcw.se.service.ServiceException
import at.ac.hcw.se.service.UserService
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.patch
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import java.util.UUID

fun Application.configureAdminRoutes(blobStorage: BlobStorageService? = null) {
    routing {
        authenticate("admin-jwt") {
            route("/users") {

                get({
                    tags("Admin")
                    summary = "List all users"
                    description = "Returns a list of all registered users. Requires admin privileges."
                    response {
                        HttpStatusCode.OK to { description = "List of all users"; body<List<UserResponse>>() }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                    }
                }) {
                    val admin = call.toAdmin()
                    call.respond(HttpStatusCode.OK, admin.listAllUsers().map { it.toResponse() })
                }

                post({
                    tags("Admin")
                    summary = "Create a user"
                    description = "Creates a new user account. Admins can optionally grant admin privileges. Requires admin privileges."
                    request { body<AdminUserCreate> { description = "User data" } }
                    response {
                        HttpStatusCode.Created to { description = "User created"; body<Map<String, Int>>() }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.Conflict to { description = "Username or email already taken" }
                    }
                }) {
                    val admin = call.toAdmin()
                    val dto = call.receive<AdminUserCreate>()
                    val id = admin.createUser(dto)
                    call.respond(HttpStatusCode.Created, mapOf("id" to id))
                }

                get("/{id}", {
                    tags("Admin")
                    summary = "Get any user profile"
                    description = "Returns the profile of the specified user. Requires admin privileges."
                    request { pathParameter<Int>("id") { description = "User ID" } }
                    response {
                        HttpStatusCode.OK to { description = "User profile"; body<UserResponse>() }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val admin = call.toAdmin()
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid user ID")
                    call.respond(HttpStatusCode.OK, admin.getUser(id).toResponse())
                }

                patch("/{id}", {
                    tags("Admin")
                    summary = "Update any user"
                    description = "Updates profile fields, password, admin status, or lock status of any user. All fields are optional. Requires admin privileges."
                    request {
                        pathParameter<Int>("id") { description = "User ID" }
                        body<AdminUserUpdate> { description = "Fields to update (all optional)" }
                    }
                    response {
                        HttpStatusCode.OK to { description = "User updated successfully" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val admin = call.toAdmin()
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid user ID")
                    val dto = call.receive<AdminUserUpdate>()
                    admin.updateUser(id, dto)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "User updated successfully"))
                }

                delete("/{id}", {
                    tags("Admin")
                    summary = "Delete any user"
                    description = "Permanently deletes the specified user account. Requires admin privileges."
                    request { pathParameter<Int>("id") { description = "User ID" } }
                    response {
                        HttpStatusCode.NoContent to { description = "User deleted" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required or self-deletion attempted" }
                        HttpStatusCode.NotFound to { description = "User not found" }
                    }
                }) {
                    val admin = call.toAdmin()
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid user ID")
                    admin.deleteUser(id)
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            // ── Admin car management ────────────────────────────────────────────

            route("/cars") {

                post({
                    tags("Admin - Cars")
                    summary = "Create a car"
                    description = "Adds a new car to the fleet. Accepts multipart/form-data with a 'data' part (JSON car fields) and an optional 'image' part (image file uploaded to Azure Blob Storage). Requires admin privileges."
                    response {
                        HttpStatusCode.Created to { description = "Car created"; body<Map<String, Int>>() }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.BadRequest to { description = "Missing or invalid car data" }
                    }
                }) {
                    val admin = call.toAdmin()
                    var carData: CarCreateRequest? = null
                    var imageUrl: String? = null

                    call.receiveMultipart().forEachPart { part ->
                        when {
                            part is PartData.FormItem && part.name == "data" -> {
                                carData = Json.decodeFromString(part.value)
                            }
                            part is PartData.FileItem && part.name == "image" && blobStorage != null -> {
                                val bytes = part.provider().readRemaining().readByteArray()
                                val ext = part.originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
                                val blobName = "cars/${UUID.randomUUID()}.$ext"
                                blobStorage.upload(blobName, bytes)
                                imageUrl = blobName
                            }
                        }
                        part.dispose()
                    }

                    val dto = carData ?: throw ServiceException.BadRequest("Missing 'data' form field with car JSON")
                    val finalDto = if (imageUrl != null) dto.copy(imageUrl = imageUrl) else dto
                    val id = admin.createCar(finalDto)
                    call.respond(HttpStatusCode.Created, mapOf("id" to id))
                }

                patch("/{id}", {
                    tags("Admin - Cars")
                    summary = "Update a car"
                    description = "Updates car details. Accepts multipart/form-data with a 'data' part (JSON car fields) and an optional 'image' part (image file uploaded to Azure Blob Storage). All fields are optional. Requires admin privileges."
                    request {
                        pathParameter<Int>("id") { description = "Car ID" }
                    }
                    response {
                        HttpStatusCode.OK to { description = "Car updated successfully" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.NotFound to { description = "Car not found" }
                        HttpStatusCode.BadRequest to { description = "Missing or invalid car data" }
                    }
                }) {
                    val admin = call.toAdmin()
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid car ID")

                    var carData: CarUpdate? = null
                    var newImageUrl: String? = null

                    call.receiveMultipart().forEachPart { part ->
                        when {
                            part is PartData.FormItem && part.name == "data" -> {
                                carData = Json.decodeFromString(part.value)
                            }
                            part is PartData.FileItem && part.name == "image" && blobStorage != null -> {
                                val bytes = part.provider().readRemaining().readByteArray()
                                val ext = part.originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
                                val blobName = "cars/${UUID.randomUUID()}.$ext"
                                blobStorage.upload(blobName, bytes)
                                newImageUrl = blobName
                            }
                        }
                        part.dispose()
                    }

                    val dto = carData ?: throw ServiceException.BadRequest("Missing 'data' form field with car JSON")
                    if (newImageUrl != null) {
                        val oldImage = carService.getById(id)?.imageName?.takeIf { it.isNotEmpty() }
                        admin.updateCar(id, dto.copy(imageUrl = newImageUrl))
                        if (oldImage != null) blobStorage?.delete(oldImage)
                    } else {
                        admin.updateCar(id, dto)
                    }
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Car updated successfully"))
                }

                delete("/{id}", {
                    tags("Admin - Cars")
                    summary = "Delete a car"
                    description = "Removes a car from the fleet. Requires admin privileges."
                    request { pathParameter<Int>("id") { description = "Car ID" } }
                    response {
                        HttpStatusCode.NoContent to { description = "Car deleted" }
                        HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                        HttpStatusCode.NotFound to { description = "Car not found" }
                    }
                }) {
                    val admin = call.toAdmin()
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid car ID")
                    admin.deleteCar(id)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.toAdmin(): Admin {
    val principal = principal<JwtPrincipal>()!!
    val user = UserService.read(principal.userId)
        ?: throw ServiceException.NotFound("User not found")
    return Admin(user)
}
