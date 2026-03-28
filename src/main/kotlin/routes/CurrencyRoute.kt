package at.ac.hcw.se.routes

import at.ac.hcw.se.CurrencyService
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCurrencyRoutes() {
    routing {
        //TODO: Delete file in production

        /*route("/exchange-rate") {
            get( {
                tags("ExchangeRate")
                summary = "Currency Exchange"
                description = "Currency Exchange"
                response {
                }
            } )
            {
                // Implement logic to fetch and return exchange rates
                val result = CurrencyService.convert("USD", "EUR", 1.0, "secret123") // Example conversion

                call.respond(HttpStatusCode.OK, result.toString())
            }
        }*/
    }
}