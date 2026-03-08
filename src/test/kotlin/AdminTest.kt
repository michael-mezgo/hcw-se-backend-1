package at.ac.hcw.se

import at.ac.hcw.se.dto.AdminUserCreate
import at.ac.hcw.se.dto.AdminUserUpdate
import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.util.UUID
import kotlin.test.*

class AdminTest : BaseTest() {

    @Test
    fun testAdminListUsers() = testApp {
        val client = loginAsAdmin()
        client.get("/admin/users").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testAdminListUsersUnauthenticated() = testApp {
        val client = jsonClient()
        client.get("/admin/users").apply {
            // admin-session challenge always returns 403 (no session = not admin)
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun testAdminListUsersForbidden() = testApp {
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
        client.get("/admin/users").apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun testAdminCreateUser() = testApp {
        val client = loginAsAdmin()
        val id = UUID.randomUUID().toString().take(8)
        client.post("/admin/users") {
            contentType(ContentType.Application.Json)
            setBody(AdminUserCreate(
                username = "admin_created_$id",
                email = "admincreated_$id@example.com",
                password = "password123",
                firstName = "Admin",
                lastName = "Created",
                licenseNumber = "LIC$id",
                licenseValidUntil = "2030-12-31",
            ))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }
    }

    @Test
    fun testAdminCreateUserConflict() = testApp {
        val client = loginAsAdmin()
        val user = uniqueUser()
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
        client.post("/admin/users") {
            contentType(ContentType.Application.Json)
            setBody(AdminUserCreate(
                username = user.username,
                email = user.email,
                password = "password123",
                firstName = user.firstName,
                lastName = user.lastName,
                licenseNumber = user.licenseNumber,
                licenseValidUntil = user.licenseValidUntil,
            ))
        }.apply {
            assertEquals(HttpStatusCode.Conflict, status)
        }
    }

    @Test
    fun testAdminGetUser() = testApp {
        val client = loginAsAdmin()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.get("/admin/users/$id").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(user.username, body<UserResponse>().username)
        }
    }

    @Test
    fun testAdminGetUserNotFound() = testApp {
        val client = loginAsAdmin()
        client.get("/admin/users/99999").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testAdminUpdateUser() = testApp {
        val client = loginAsAdmin()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.put("/admin/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(AdminUserUpdate(email = "adminupdated@example.com", isAdmin = true))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get("/admin/users/$id").apply {
            val response = body<UserResponse>()
            assertEquals("adminupdated@example.com", response.email)
            assertEquals(true, response.isAdmin)
        }
    }

    @Test
    fun testAdminDeleteUser() = testApp {
        val client = loginAsAdmin()
        val user = uniqueUser()
        val id = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body<Map<String, Int>>()["id"]!!
        client.delete("/admin/users/$id").apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
        client.get("/admin/users/$id").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testAdminDeleteSelfForbidden() = testApp {
        val client = loginAsAdmin()
        val adminId = client.get("/admin/users").body<List<UserResponse>>()
            .first { it.username == "Admin" }.id
        client.delete("/admin/users/$adminId").apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }
}
