package at.ac.hcw.se

import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.UserUpdate
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.test.*

class UserTest : BaseTest() {

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
    fun testUpdateUserUnauthorized() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.put("/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(UserUpdate(email = "noauth@example.com"))
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testUpdateUserForbidden() = testApp {
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
        client.put("/users/${id + 1}") {
            contentType(ContentType.Application.Json)
            setBody(UserUpdate(email = "forbidden@example.com"))
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, status)
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

    @Test
    fun testDeleteUserUnauthorized() = testApp {
        val client = jsonClient()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.delete("/users/$id").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testDeleteUserForbidden() = testApp {
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
        client.delete("/users/${id + 1}").apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }
}
