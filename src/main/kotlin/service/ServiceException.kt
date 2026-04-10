package at.ac.hcw.se.service

import io.ktor.http.*

sealed class ServiceException(val status: HttpStatusCode, override val message: String) : RuntimeException(message) {
    class NotFound(message: String = "Resource not found") : ServiceException(HttpStatusCode.NotFound, message)
    class Conflict(message: String = "Resource already exists") : ServiceException(HttpStatusCode.Conflict, message)
    class Unauthorized(message: String = "Invalid credentials") : ServiceException(HttpStatusCode.Unauthorized, message)
    class Forbidden(message: String = "Access denied") : ServiceException(HttpStatusCode.Forbidden, message)
    class BadRequest(message: String = "Invalid request") : ServiceException(HttpStatusCode.BadRequest, message)
}
