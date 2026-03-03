package at.ac.hcw.se

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.util.Base64
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
    }

    private fun basicAuthHeader(user: String, password: String): String =
        "Basic ${Base64.getEncoder().encodeToString("$user:$password".toByteArray())}"

    // ── Routing ──────────────────────────────────────────────────────────────

    @Test
    fun testRoot() = testApp {
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    // ── Serialization ────────────────────────────────────────────────────────

    @Test
    fun testJsonEndpoint() = testApp {
        val client = jsonClient()
        client.get("/json/kotlinx-serialization").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = body<Map<String, String>>()
            assertEquals("world", body["hello"])
        }
    }

    // ── Cities CRUD ───────────────────────────────────────────────────────────

    @Test
    fun testCreateCity() = testApp {
        val client = jsonClient()
        client.post("/cities") {
            contentType(ContentType.Application.Json)
            setBody(City("Vienna", 1_900_000))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }
    }

    @Test
    fun testReadCity() = testApp {
        val client = jsonClient()

        val id = client.post("/cities") {
            contentType(ContentType.Application.Json)
            setBody(City("Berlin", 3_700_000))
        }.body<Int>()

        client.get("/cities/$id").apply {
            assertEquals(HttpStatusCode.OK, status)
            val city = body<City>()
            assertEquals("Berlin", city.name)
            assertEquals(3_700_000, city.population)
        }
    }

    @Test
    fun testUpdateCity() = testApp {
        val client = jsonClient()

        val id = client.post("/cities") {
            contentType(ContentType.Application.Json)
            setBody(City("Paris", 2_100_000))
        }.body<Int>()

        client.put("/cities/$id") {
            contentType(ContentType.Application.Json)
            setBody(City("Paris Updated", 2_200_000))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        val updated = client.get("/cities/$id").body<City>()
        assertEquals("Paris Updated", updated.name)
        assertEquals(2_200_000, updated.population)
    }

    @Test
    fun testDeleteCity() = testApp {
        val client = jsonClient()

        val id = client.post("/cities") {
            contentType(ContentType.Application.Json)
            setBody(City("Rome", 2_800_000))
        }.body<Int>()

        client.delete("/cities/$id").apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/cities/$id").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testReadCityNotFound() = testApp {
        client.get("/cities/999999").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    // ── Security ─────────────────────────────────────────────────────────────

    @Test
    fun testBasicAuthSuccess() = testApp {
        client.get("/protected/route/basic") {
            headers.append(HttpHeaders.Authorization, basicAuthHeader("admin", "admin"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello admin", bodyAsText())
        }
    }

    @Test
    fun testBasicAuthWrongPassword() = testApp {
        client.get("/protected/route/basic") {
            headers.append(HttpHeaders.Authorization, basicAuthHeader("user", "wrongpassword"))
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testBasicAuthNoCredentials() = testApp {
        client.get("/protected/route/basic").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testSessionIncrement() = testApp {
        client.get("/session/increment").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().contains("Counter is"))
        }
    }

    @Test
    fun testSessionIncrementCounterStartsAtZero() = testApp {
        // First request: session is new, counter is 0 before incrementing
        client.get("/session/increment").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().contains("Counter is 0"))
        }
    }
}
