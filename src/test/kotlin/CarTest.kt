package at.ac.hcw.se

import at.ac.hcw.se.dto.CarCreateRequest
import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.dto.CarUpdate
import at.ac.hcw.se.dto.CoordinateDto
import at.ac.hcw.se.dto.LoginResponse
import at.ac.hcw.se.dto.UserLoginRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class CarTest : BaseTest() {

    private fun sampleCarJson(): String = Json.encodeToString(
        CarCreateRequest(
            manufacturer = "BMW",
            model = "3 Series",
            year = 2022,
            pricePerDayInUSD = 89.99,
            description = "A sporty sedan",
            transmission = "AUTOMATIC",
            power = 184,
            fuelType = "GASOLINE",
            location = CoordinateDto(48.2082, 16.3738),
        )
    )

    private suspend fun ApplicationTestBuilder.createCar(adminClient: HttpClient): Int {
        return adminClient.post("/cars") {
            setBody(MultiPartFormDataContent(formData { append("data", sampleCarJson()) }))
        }.body<Map<String, Int>>()["id"]!!
    }

    private suspend fun ApplicationTestBuilder.loginAsUser(): HttpClient {
        val user = uniqueUser()
        val anon = jsonClient()
        anon.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
        val token = anon.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(user.username, user.password))
        }.body<LoginResponse>().token
        return jsonClient(token)
    }

    // ── List cars ─────────────────────────────────────────────────────────────

    @Test
    fun testListCarsAsAdmin() = testApp {
        val client = loginAsAdmin()
        client.get("/cars").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testListCarsAsUser() = testApp {
        val client = loginAsUser()
        client.get("/cars").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testListCarsReturnsCreatedCar() = testApp {
        val client = loginAsAdmin()
        createCar(client)
        client.get("/cars").apply {
            assertEquals(HttpStatusCode.OK, status)
            val cars = body<List<CarResponse>>()
            assertTrue(cars.any { it.manufacturer == "BMW" && it.model == "3 Series" })
        }
    }

    // ── Get car by ID ─────────────────────────────────────────────────────────

    @Test
    fun testGetCarById() = testApp {
        val client = loginAsAdmin()
        val id = createCar(client)
        client.get("/cars/$id").apply {
            assertEquals(HttpStatusCode.OK, status)
            val car = body<CarResponse>()
            assertEquals("BMW", car.manufacturer)
            assertEquals("3 Series", car.model)
            assertEquals(2022, car.year)
            assertEquals(89.99, car.pricePerDay.amount)
        }
    }

    @Test
    fun testGetCarByIdNotFound() = testApp {
        val client = loginAsAdmin()
        client.get("/cars/99999").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testGetCarByIdBadRequest() = testApp {
        val client = loginAsAdmin()
        client.get("/cars/abc").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    // ── Admin create car ──────────────────────────────────────────────────────

    @Test
    fun testAdminCreateCar() = testApp {
        val client = loginAsAdmin()
        client.post("/cars") {
            setBody(MultiPartFormDataContent(formData { append("data", sampleCarJson()) }))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            val id = body<Map<String, Int>>()["id"]
            assertNotNull(id)
        }
    }

    @Test
    fun testAdminCreateCarMissingData() = testApp {
        val client = loginAsAdmin()
        client.post("/cars") {
            setBody(MultiPartFormDataContent(formData {}))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testAdminCreateCarForbiddenForUser() = testApp {
        val client = loginAsUser()
        client.post("/cars") {
            setBody(MultiPartFormDataContent(formData { append("data", sampleCarJson()) }))
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    // ── Admin update car ──────────────────────────────────────────────────────

    @Test
    fun testAdminUpdateCar() = testApp {
        val client = loginAsAdmin()
        val id = createCar(client)
        client.patch("/cars/$id") {
            setBody(MultiPartFormDataContent(formData {
                append("data", Json.encodeToString(CarUpdate(manufacturer = "Audi", pricePerDayInUSD = 99.0)))
            }))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get("/cars/$id").apply {
            val car = body<CarResponse>()
            assertEquals("Audi", car.manufacturer)
            assertEquals(99.0, car.pricePerDay.amount)
        }
    }

    @Test
    fun testAdminUpdateCarNotFound() = testApp {
        val client = loginAsAdmin()
        client.patch("/cars/99999") {
            setBody(MultiPartFormDataContent(formData {
                append("data", Json.encodeToString(CarUpdate(manufacturer = "Audi")))
            }))
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testAdminUpdateCarBadRequest() = testApp {
        val client = loginAsAdmin()
        client.patch("/cars/abc") {
            contentType(ContentType.Application.Json)
            setBody(CarUpdate(manufacturer = "Audi"))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testAdminUpdateCarForbiddenForUser() = testApp {
        val adminClient = loginAsAdmin()
        val id = createCar(adminClient)
        val userClient = loginAsUser()
        userClient.patch("/cars/$id") {
            contentType(ContentType.Application.Json)
            setBody(CarUpdate(manufacturer = "Audi"))
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    // ── Admin delete car ──────────────────────────────────────────────────────

    @Test
    fun testAdminDeleteCar() = testApp {
        val client = loginAsAdmin()
        val id = createCar(client)
        client.delete("/cars/$id").apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
        client.get("/cars/$id").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testAdminDeleteCarNotFound() = testApp {
        val client = loginAsAdmin()
        client.delete("/cars/99999").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testAdminDeleteCarBadRequest() = testApp {
        val client = loginAsAdmin()
        client.delete("/cars/abc").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    // ── Book / Unbook car ─────────────────────────────────────────────────────

    @Test
    fun testBookCar() = testApp {
        val adminClient = loginAsAdmin()
        val id = createCar(adminClient)
        val userClient = loginAsUser()
        userClient.post("/cars/$id/book").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testBookCarNotFound() = testApp {
        val userClient = loginAsUser()
        userClient.post("/cars/99999/book").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testBookCarAlreadyBooked() = testApp {
        val adminClient = loginAsAdmin()
        val id = createCar(adminClient)
        val userClient = loginAsUser()
        userClient.post("/cars/$id/book").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        userClient.post("/cars/$id/book").apply {
            assertEquals(HttpStatusCode.Conflict, status)
        }
    }

    @Test
    fun testBookCarRequiresAuth() = testApp {
        val adminClient = loginAsAdmin()
        val id = createCar(adminClient)
        val anonClient = jsonClient()
        anonClient.post("/cars/$id/book").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testUnbookCar() = testApp {
        val adminClient = loginAsAdmin()
        val id = createCar(adminClient)
        val userClient = loginAsUser()
        userClient.post("/cars/$id/book").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        userClient.post("/cars/$id/unbook").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testUnbookCarNotFound() = testApp {
        val userClient = loginAsUser()
        userClient.post("/cars/99999/unbook").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testUnbookCarNotBooked() = testApp {
        val adminClient = loginAsAdmin()
        val id = createCar(adminClient)
        val userClient = loginAsUser()
        userClient.post("/cars/$id/unbook").apply {
            assertEquals(HttpStatusCode.Conflict, status)
        }
    }

    @Test
    fun testBookedCarNotListedAsAvailable() = testApp {
        val adminClient = loginAsAdmin()
        val id = createCar(adminClient)
        val userClient = loginAsUser()
        userClient.post("/cars/$id/book")
        adminClient.get("/cars").apply {
            assertEquals(HttpStatusCode.OK, status)
            val cars = body<List<CarResponse>>()
            assertTrue(cars.none { it.id == id })
        }
    }
}

