package at.ac.hcw.se

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
        client.get("/session/increment").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().contains("Counter is 0"))
        }
    }
}
