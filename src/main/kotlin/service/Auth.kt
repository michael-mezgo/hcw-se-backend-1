package at.ac.hcw.se.service

import at.ac.hcw.se.dto.LoginResponse
import at.ac.hcw.se.dto.UserLoginRequest
import at.ac.hcw.se.dto.UserRegistration
import at.ac.hcw.se.generateToken
import org.jetbrains.exposed.exceptions.ExposedSQLException

object Auth {

    suspend fun register(dto: UserRegistration): Int {
        try {
            return UserService.create(dto)
        } catch (e: ExposedSQLException) {
            throw ServiceException.Conflict("Username or email already taken")
        }
    }

    suspend fun login(dto: UserLoginRequest): LoginResponse {
        val credentials = UserService.findByCredentials(dto.username, dto.password)
            ?: throw ServiceException.Unauthorized("Invalid username or password")
        val token = generateToken(credentials.id, credentials.username, credentials.isAdmin)
        return LoginResponse(userId = credentials.id, isAdmin = credentials.isAdmin, token = token)
    }
}
