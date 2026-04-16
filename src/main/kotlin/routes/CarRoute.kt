package at.ac.hcw.se.routes

import at.ac.hcw.se.service.CarService
import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.service.ServiceException
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.application.*
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
        }
    }
}
