package at.ac.hcw.se.routes

import at.ac.hcw.se.BlobStorageService
import at.ac.hcw.se.business.BookingResult
import at.ac.hcw.se.carService
import at.ac.hcw.se.dto.BookingCreateRequest
import at.ac.hcw.se.dto.BookingResponse
import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.dto.JwtPrincipal
import at.ac.hcw.se.service.CurrencyService
import at.ac.hcw.se.service.ServiceException
import at.ac.hcw.se.service.UserService
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureBookingRoutes(blobStorage: BlobStorageService? = null) {
    routing {
        authenticate("user-jwt") {
            route("/bookings") {

                post({
                    tags("Bookings")
                    summary = "Book a car"
                    description = "Creates a booking for the given car ID for the authenticated user."
                    request { body<BookingCreateRequest> { description = "Car to book" } }
                    response {
                        HttpStatusCode.Created to { description = "Booking created"; body<BookingResponse>() }
                        HttpStatusCode.NotFound to { description = "Car or user not found" }
                        HttpStatusCode.Conflict to { description = "Car is not available" }
                    }
                }) {
                    val dto = call.receive<BookingCreateRequest>()
                    val principal = call.principal<JwtPrincipal>()!!
                    val user = UserService.read(principal.userId)
                        ?: throw ServiceException.NotFound("User not found")
                    when (carService.book(dto.carId, user.id)) {
                        BookingResult.CarNotFound -> throw ServiceException.NotFound("Car not found")
                        BookingResult.UserNotFound -> throw ServiceException.NotFound("User not found")
                        BookingResult.CarUnavailable -> throw ServiceException.Conflict("Car is not available")
                        BookingResult.CarBooked -> {
                            call.respond(HttpStatusCode.Created, BookingResponse(
                                carId = dto.carId,
                                bookedBy = user.toResponse(),
                            ))
                        }
                        BookingResult.CarUnbooked -> throw ServiceException.ServerError("Unexpected unbooking result")
                    }
                }

                get({
                    tags("Bookings")
                    summary = "List my bookings"
                    description = "Returns all cars currently booked by the authenticated user."
                    request {
                        queryParameter<String>("currencyCode") {
                            description = "Currency Code - Supported currencies: ${CurrencyService.getSupportedCurrencies()}"
                        }
                    }
                    response {
                        HttpStatusCode.OK to { description = "List of booked cars"; body<List<CarResponse>>() }
                    }
                }) {
                    val currencyCode = call.request.queryParameters["currencyCode"] ?: "USD"
                    val principal = call.principal<JwtPrincipal>()!!
                    val cars = carService.listBookedByUser(principal.userId)
                    call.respond(HttpStatusCode.OK, cars.map { it.toResponse(blobStorageService = blobStorage, currencyCode = currencyCode) })
                }

                delete("/{carId}", {
                    tags("Bookings")
                    summary = "Cancel a booking"
                    description = "Cancels the booking for the given car. Users can only cancel their own bookings; admins can cancel any booking."
                    request { pathParameter<Int>("carId") { description = "Car ID" } }
                    response {
                        HttpStatusCode.NoContent to { description = "Booking cancelled" }
                        HttpStatusCode.Forbidden to { description = "You can only cancel your own bookings" }
                        HttpStatusCode.NotFound to { description = "Car not found" }
                        HttpStatusCode.Conflict to { description = "Car is not booked" }
                    }
                }) {
                    val principal = call.principal<JwtPrincipal>()!!
                    val carId = call.parameters["carId"]?.toIntOrNull()
                        ?: throw ServiceException.BadRequest("Invalid car ID")
                    if (!principal.isAdmin) {
                        val car = carService.getById(carId)
                            ?: throw ServiceException.NotFound("Car not found")
                        val bookedBy = car.bookedBy
                        if (bookedBy != null && bookedBy.id != principal.userId)
                            throw ServiceException.Forbidden("You can only cancel your own bookings")
                    }
                    when (carService.unbook(carId)) {
                        BookingResult.CarNotFound -> throw ServiceException.NotFound("Car not found")
                        BookingResult.CarUnavailable -> throw ServiceException.Conflict("Car is not booked")
                        BookingResult.CarUnbooked -> call.respond(HttpStatusCode.NoContent)
                        BookingResult.CarBooked -> throw ServiceException.ServerError("Unexpected unbooking result")
                        BookingResult.UserNotFound -> throw ServiceException.ServerError("Unexpected unbooking result")
                    }
                }
            }
        }

        authenticate("admin-jwt") {
            get("/bookings/{carId}", {
                tags("Bookings")
                summary = "Get booking for a car (Admin)"
                description = "Returns the user who booked this car, or 404 if the car is not booked. Requires admin privileges."
                request { pathParameter<Int>("carId") { description = "Car ID" } }
                response {
                    HttpStatusCode.OK to { description = "Booking details"; body<BookingResponse>() }
                    HttpStatusCode.Forbidden to { description = "Admin privileges required" }
                    HttpStatusCode.NotFound to { description = "Car not found or not booked" }
                }
            }) {
                val carId = call.parameters["carId"]?.toIntOrNull()
                    ?: throw ServiceException.BadRequest("Invalid car ID")
                val car = carService.getById(carId)
                    ?: throw ServiceException.NotFound("Car not found")
                val bookedBy = car.bookedBy
                    ?: throw ServiceException.NotFound("Car is not booked")
                call.respond(HttpStatusCode.OK, BookingResponse(
                    carId = car.id,
                    bookedBy = bookedBy.toResponse(),
                ))
            }
        }
    }
}
