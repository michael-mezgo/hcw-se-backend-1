package at.ac.hcw.se

import at.ac.hcw.se.dto.LoginResponse
import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserRegistration
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.util.*

abstract class BaseTest {

    protected fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application { module() }
            block()
        }

    protected fun ApplicationTestBuilder.jsonClient(token: String? = null): HttpClient {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        if (token != null) {
            client.requestPipeline.intercept(HttpRequestPipeline.Before) {
                context.headers.remove(HttpHeaders.Authorization)
                context.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        return client
    }

    protected fun uniqueUser(): UserRegistration {
        val id = UUID.randomUUID().toString().take(8)
        return UserRegistration(
            username = "user_$id",
            email = "$id@example.com",
            password = "password123",
            firstName = "Test",
            lastName = "User",
            licenseNumber = "LIC$id",
            licenseValidUntil = "2030-12-31",
        )
    }

    protected suspend fun ApplicationTestBuilder.loginAsAdmin(): HttpClient {
        val client = jsonClient()
        val token = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest("Admin", "Admin"))
        }.body<LoginResponse>().token
        return jsonClient(token)
    }
}
