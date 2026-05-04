package at.ac.hcw.se.routes

import at.ac.hcw.se.BlobStorageService
import at.ac.hcw.se.carService
import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.service.CurrencyService
import at.ac.hcw.se.service.ServiceException
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.application.*
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

        }
    }
}

