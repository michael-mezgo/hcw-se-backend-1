package at.ac.hcw.se.business

import at.ac.hcw.se.service.UserService
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.UserUpdate
import at.ac.hcw.se.service.ServiceException

open class User(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val licenseNumber: String,
    val licenseValidUntil: String,
    val isAdmin: Boolean,
    val isLocked: Boolean,
    val preferredCurrency: String = "USD",
) {

    fun toResponse() = UserResponse(
        id = id,
        username = username,
        email = email,
        firstName = firstName,
        lastName = lastName,
        licenseNumber = licenseNumber,
        licenseValidUntil = licenseValidUntil,
        isAdmin = isAdmin,
        isLocked = isLocked,
        preferredCurrency = preferredCurrency,
    )

    suspend fun updateProfile(dto: UserUpdate) {
        if (!UserService.update(id, dto))
            throw ServiceException.NotFound("User not found")
    }

    suspend fun deleteAccount() {
        if (!UserService.delete(id))
            throw ServiceException.NotFound("User not found")
    }
}
