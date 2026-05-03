package at.ac.hcw.se.routes

import at.ac.hcw.se.service.CurrencyService
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCurrencyRoutes() {
    routing {
        route("/currencies") {
            get({}){
                val result = CurrencyService.getSupportedCurrencies()
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }
}