package at.ac.hcw.se.routes

import at.ac.hcw.se.BlobStorageService
import at.ac.hcw.se.business.BookingResult
import at.ac.hcw.se.business.User
import at.ac.hcw.se.carService
import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.dto.JwtPrincipal
import at.ac.hcw.se.service.CurrencyService
import at.ac.hcw.se.service.ServiceException
import at.ac.hcw.se.service.UserService
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCarRoutes(blobStorage: BlobStorageService? = null) {
    routing {
        route("/cars") {
            get({
                tags("Cars")
                summary = "List cars"
                description = "Returns all cars. Use ?available=true to filter for available (unbooked) cars only."
                request {
                    queryParameter<Boolean>("available") { description = "If true, only available cars are returned"; required = false }
                    queryParameter<String>("currencyCode") { description = "Currency Code - Supported currencies: ${CurrencyService.getSupportedCurrencies()}" }
                }
                response {
                    HttpStatusCode.OK to { description = "List of cars"; body<List<CarResponse>>() }
                }
            }) {
                val currencyCode = call.request.queryParameters["currencyCode"] ?: "USD"
                val onlyAvailable = call.request.queryParameters["available"]?.toBooleanStrictOrNull()
                val cars = if (onlyAvailable == true) carService.listAllAvailable() else carService.listAll()
                call.respond(HttpStatusCode.OK, cars.map { it.toResponse(blobStorageService = blobStorage, currencyCode = currencyCode) })
            }

            get("/{id}", {
                tags("Cars")
                summary = "Get car details"
                description = "Returns details for a specific car."
                request {
                    pathParameter<Int>("id") { description = "Car ID" }
                    queryParameter<String>("currencyCode") { description = "Currency Code - Supported currencies: ${CurrencyService.getSupportedCurrencies()}" }
                }
                response {
                    HttpStatusCode.OK to { description = "Car details"; body<CarResponse>() }
                    HttpStatusCode.NotFound to { description = "Car not found" }
                }
            }) {
                val currencyCode = call.request.queryParameters["currencyCode"] ?: "USD"
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw ServiceException.BadRequest("Invalid car ID")
                val car = carService.getById(id)
                    ?: throw ServiceException.NotFound("Car not found")
                call.respond(HttpStatusCode.OK, car.toResponse(blobStorageService = blobStorage, currencyCode = currencyCode))
            }

            authenticate("user-jwt") {
                post("/{id}/book", {
                    tags("Cars")
                    summary = "Book a car"
                    description = "Books a specific car for the authenticated user."
                    request { pathParameter<Int>("id") { description = "Car ID" } }
                    response {
                        HttpStatusCode.OK to { description = "Car booked successfully" }
                        HttpStatusCode.NotFound to { description = "Car or user not found" }
                        HttpStatusCode.Conflict to { description = "Car is not available" }
                    }
                }) {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid car ID")
                    val user = call.toUser()
                    when (carService.book(id, user.id)) {
                        BookingResult.CarNotFound -> throw ServiceException.NotFound("Car not found")
                        BookingResult.UserNotFound -> throw ServiceException.NotFound("User not found")
                        BookingResult.CarUnavailable -> throw ServiceException.Conflict("Car is not available")
                        BookingResult.CarBooked -> call.respond(HttpStatusCode.OK, mapOf("message" to "Car booked successfully"))
                        BookingResult.CarUnbooked -> {} // not returned by book()
                    }
                }

                post("/{id}/unbook", {
                    tags("Cars")
                    summary = "Unbook a car"
                    description = "Releases a booking on a specific car. Users can only unbook their own bookings; admins can unbook any car."
                    request { pathParameter<Int>("id") { description = "Car ID" } }
                    response {
                        HttpStatusCode.OK to { description = "Car unbooked successfully" }
                        HttpStatusCode.Forbidden to { description = "You can only unbook your own bookings" }
                        HttpStatusCode.NotFound to { description = "Car not found" }
                        HttpStatusCode.Conflict to { description = "Car is not booked" }
                    }
                }) {
                    val principal = call.principal<JwtPrincipal>()!!
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid car ID")
                    if (!principal.isAdmin) {
                        val car = carService.getById(id)
                            ?: throw ServiceException.NotFound("Car not found")
                        val bookedBy = car.bookedBy
                        if (bookedBy != null && bookedBy.id != principal.userId)
                            throw ServiceException.Forbidden("You can only unbook your own bookings")
                    }
                    when (carService.unbook(id)) {
                        BookingResult.CarNotFound -> throw ServiceException.NotFound("Car not found")
                        BookingResult.CarUnavailable -> throw ServiceException.Conflict("Car is not booked")
                        BookingResult.CarUnbooked -> call.respond(HttpStatusCode.OK, mapOf("message" to "Car unbooked successfully"))
                        BookingResult.CarBooked -> {} // not returned by unbook()
                        BookingResult.UserNotFound -> {} // not returned by unbook()
                    }
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

