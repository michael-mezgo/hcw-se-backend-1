package at.ac.hcw.se

import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserRegistration
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.UserUpdate
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.util.UUID
import kotlin.test.*

class ApplicationTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application { module() }
            block()
        }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
        install(HttpCookies)
    }

    private fun uniqueUser(): UserRegistration {
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

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Test
    fun testRegisterUser() = testApp {
        val client = jsonClient()
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(uniqueUser())
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }
    }

    @Test
    fun testLoginSuccess() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, user.password))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testLoginInvalidCredentials() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, "wrongpassword"))
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testLogout() = testApp {
        val client = jsonClient()
        client.post("/auth/logout").apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Test
    fun testGetUserProfile() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, user.password))
        }
        client.get("/users/$id").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(user.username, body<UserResponse>().username)
        }
    }

    @Test
    fun testGetUserProfileUnauthorized() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.get("/users/$id").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testGetUserProfileForbidden() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, user.password))
        }
        client.get("/users/99999").apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun testUpdateUser() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, user.password))
        }
        client.put("/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(UserUpdate(email = "updated@example.com"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testDeleteUser() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, user.password))
        }
        client.delete("/users/$id").apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
    }
}
