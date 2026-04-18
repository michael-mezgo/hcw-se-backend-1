package at.ac.hcw.se.routes

import at.ac.hcw.se.business.BookingResult
import at.ac.hcw.se.business.User
import at.ac.hcw.se.database.CarEntity
import at.ac.hcw.se.database.CarTable.description
import at.ac.hcw.se.service.CarService
import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.dto.JwtPrincipal
import at.ac.hcw.se.service.ServiceException
import at.ac.hcw.se.service.UserService
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.client.request.request
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCarRoutes() {
    routing {
        route("/cars") {

            get({
                tags("Cars")
                summary = "List all available cars"
                description = "Returns all cars that are currently available for rental."
                response {
                    HttpStatusCode.OK to { description = "List of available cars"; body<List<CarResponse>>() }
                }
            }) {
                val cars = CarService.listAllAvailable()
                call.respond(HttpStatusCode.OK, cars.map { it.toResponse() })
            }

            get("/{id}", {
                tags("Cars")
                summary = "Get car details"
                description = "Returns details for a specific car."
                request { pathParameter<Int>("id") { description = "Car ID" } }
                response {
                    HttpStatusCode.OK to { description = "Car details"; body<CarResponse>() }
                    HttpStatusCode.NotFound to { description = "Car not found" }
                }
            }) {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw ServiceException.BadRequest("Invalid car ID")
                val car = CarService.getById(id)
                    ?: throw ServiceException.NotFound("Car not found")
                call.respond(HttpStatusCode.OK, car.toResponse())
            }

            authenticate("user-jwt") {
                post("/{id}/book", {
                    tags("Cars")
                    summary = "Book car"
                    description = "Books a specific car"
                    request { pathParameter<Int>("id") { description = "Car found" } }
                    response {
                        HttpStatusCode.OK to { description = "Accessed Car" }
                        HttpStatusCode.NotFound to { description = "Car not found" }
                    }
                }) {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid car ID")
                    val user = call.toUser()

                }
            }
        }
    }
}

private suspend fun ApplicationCall.toUser(): User {
    val principal = principal<JwtPrincipal>()!!
    return UserService.read(principal.userId)
        ?: throw ServiceException.NotFound("User not found")
}
