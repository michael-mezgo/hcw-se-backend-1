package at.ac.hcw.se

import at.ac.hcw.se.dto.JwtPrincipal
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*

const val JWT_SECRET = "car-rental-super-secret-key-change-me"
const val JWT_ISSUER = "car-rental-service"
const val JWT_AUDIENCE = "car-rental-users"
private const val JWT_EXPIRY_MS = 86_400_000L // 24 hours

fun generateToken(userId: Int, username: String, isAdmin: Boolean): String =
    JWT.create()
        .withIssuer(JWT_ISSUER)
        .withAudience(JWT_AUDIENCE)
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withClaim("isAdmin", isAdmin)
        .withExpiresAt(Date(System.currentTimeMillis() + JWT_EXPIRY_MS))
        .sign(Algorithm.HMAC256(JWT_SECRET))

fun Application.configureSecurity() {
    val algorithm = Algorithm.HMAC256(JWT_SECRET)
    val verifier = JWT.require(algorithm).withIssuer(JWT_ISSUER).withAudience(JWT_AUDIENCE).build()

    install(Authentication) {
        jwt("user-jwt") {
            realm = "Car Rental Service"
            this.verifier(verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt() ?: return@validate null
                val username = credential.payload.getClaim("username").asString() ?: return@validate null
                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false
                JwtPrincipal(userId = userId, username = username, isAdmin = isAdmin)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
            }
        }
        jwt("admin-jwt") {
            realm = "Car Rental Service Admin"
            this.verifier(verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt() ?: return@validate null
                val username = credential.payload.getClaim("username").asString() ?: return@validate null
                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false
                if (!isAdmin) return@validate null
                JwtPrincipal(userId = userId, username = username, isAdmin = true)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin privileges required"))
            }
        }
    }
}
