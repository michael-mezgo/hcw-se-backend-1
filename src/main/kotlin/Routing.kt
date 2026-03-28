package at.ac.hcw.se

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/convert") {
            val fromCurrency = call.parameters["from"] ?: return@get call.respondText(
                "Missing 'from' parameter", status = HttpStatusCode.BadRequest
            )
            val toCurrency = call.parameters["to"] ?: return@get call.respondText(
                "Missing 'to' parameter", status = HttpStatusCode.BadRequest
            )
            val amount = call.parameters["amount"]?.toDoubleOrNull() ?: return@get call.respondText(
                "Missing or invalid 'amount' parameter", status = HttpStatusCode.BadRequest
            )
            val apiKey = call.parameters["apiKey"] ?: return@get call.respondText(
                "Missing 'apiKey' parameter", status = HttpStatusCode.BadRequest
            )
            val result = CurrencyService.convert(fromCurrency, toCurrency, amount, apiKey)
            call.respond(mapOf("from" to fromCurrency, "to" to toCurrency, "amount" to amount, "result" to result))
        }
    }
}
