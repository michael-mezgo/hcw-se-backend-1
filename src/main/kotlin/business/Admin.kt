package at.ac.hcw.se.business

import at.ac.hcw.se.service.CarService
import at.ac.hcw.se.service.UserService
import at.ac.hcw.se.dto.AdminUserCreate
import at.ac.hcw.se.dto.AdminUserUpdate
import at.ac.hcw.se.dto.CarCreateRequest
import at.ac.hcw.se.dto.CarUpdate
import at.ac.hcw.se.service.ServiceException
import org.jetbrains.exposed.exceptions.ExposedSQLException

class Admin(
    id: Int,
    username: String,
    email: String,
    firstName: String,
    lastName: String,
    licenseNumber: String,
    licenseValidUntil: String,
    isAdmin: Boolean,
    isLocked: Boolean,
) : User(id, username, email, firstName, lastName, licenseNumber, licenseValidUntil, isAdmin, isLocked) {

    constructor(user: User) : this(
        user.id, user.username, user.email, user.firstName, user.lastName,
        user.licenseNumber, user.licenseValidUntil, user.isAdmin, user.isLocked
    )

    // ── User management ─────────────────────────────────────────────────────

    suspend fun listAllUsers(): List<User> {
        return UserService.listAll()
    }

    suspend fun createUser(dto: AdminUserCreate): Int {
        try {
            return UserService.adminCreate(dto)
        } catch (e: ExposedSQLException) {
            throw ServiceException.Conflict("Username or email already taken")
        }
    }

    suspend fun getUser(userId: Int): User {
        return UserService.read(userId)
            ?: throw ServiceException.NotFound("User not found")
    }

    suspend fun updateUser(userId: Int, dto: AdminUserUpdate) {
        if (!UserService.adminUpdate(userId, dto))
            throw ServiceException.NotFound("User not found")
    }

    suspend fun deleteUser(targetUserId: Int) {
        if (id == targetUserId)
            throw ServiceException.Forbidden("Admins cannot delete their own account")
        if (!UserService.delete(targetUserId))
            throw ServiceException.NotFound("User not found")
    }

    // ── Car management ──────────────────────────────────────────────────────

    suspend fun createCar(dto: CarCreateRequest): Int {
        return CarService.create(dto)
    }

    suspend fun updateCar(carId: Int, dto: CarUpdate) {
        if (!CarService.update(carId, dto))
            throw ServiceException.NotFound("Car not found")
    }

    suspend fun deleteCar(carId: Int) {
        if (!CarService.delete(carId))
            throw ServiceException.NotFound("Car not found")
    }
}
