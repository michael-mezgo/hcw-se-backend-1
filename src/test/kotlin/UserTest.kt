package at.ac.hcw.se

import at.ac.hcw.se.dto.LoginResponse
import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.UserUpdate
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class UserTest : BaseTest() {

    private suspend fun ApplicationTestBuilder.loginUser(user: at.ac.hcw.se.dto.UserRegistration): String {
        val anon = jsonClient()
        anon.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
        return anon.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, user.password))
        }.body<LoginResponse>().token
    }

    @Test
    fun testGetUserProfile() = testApp {
        val user = uniqueUser()
        jsonClient(loginUser(user)).get("/users/me").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(user.username, body<UserResponse>().username)
        }
    }

    @Test
    fun testGetUserProfileUnauthorized() = testApp {
        jsonClient().get("/users/me").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testUpdateUser() = testApp {
        val user = uniqueUser()
        jsonClient(loginUser(user)).patch("/users/me") {
            contentType(ContentType.Application.Json)
            setBody(UserUpdate(email = "updated@example.com"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testUpdateUserUnauthorized() = testApp {
        jsonClient().patch("/users/me") {
            contentType(ContentType.Application.Json)
            setBody(UserUpdate(email = "noauth@example.com"))
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testDeleteUser() = testApp {
        val user = uniqueUser()
        jsonClient(loginUser(user)).delete("/users/me").apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
    }

    @Test
    fun testDeleteUserUnauthorized() = testApp {
        jsonClient().delete("/users/me").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
}
