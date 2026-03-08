package at.ac.hcw.se

import at.ac.hcw.se.dto.UserLoginRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.test.*

class AuthTest : BaseTest() {

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
    fun testRegisterDuplicateUsername() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.apply {
            assertEquals(HttpStatusCode.Conflict, status)
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
    fun testLoginLockedAccount() = testApp {
        val adminClient = jsonClient()
        val userClient = jsonClient()
        val user = uniqueUser()
        val id = userClient.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        adminClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest("Admin", "Admin"))
        }
        adminClient.put("/admin/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(at.ac.hcw.se.dto.AdminUserUpdate(isLocked = true))
        }
        userClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, user.password))
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
}
