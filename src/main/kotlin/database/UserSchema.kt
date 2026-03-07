package at.ac.hcw.se.database

import at.ac.hcw.se.dto.UserRegistration
import at.ac.hcw.se.dto.UserResponse
import at.ac.hcw.se.dto.UserUpdate
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.security.MessageDigest

// ── Table definition ─────────────────────────────────────────────────────────

object UserTable : IntIdTable("users") {
    val username         = varchar("username", 255).uniqueIndex()
    val email            = varchar("email", 255).uniqueIndex()
    val passwordHash     = varchar("password_hash", 255)
    val firstName        = varchar("first_name", 255)
    val lastName         = varchar("last_name", 255)
    val licenseNumber    = varchar("license_number", 255)
    val licenseValidUntil = varchar("license_valid_until", 10)  // ISO 8601: YYYY-MM-DD
}

// ── DAO entity ───────────────────────────────────────────────────────────────

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(UserTable)

    var username          by UserTable.username
    var email             by UserTable.email
    var passwordHash      by UserTable.passwordHash
    var firstName         by UserTable.firstName
    var lastName          by UserTable.lastName
    var licenseNumber     by UserTable.licenseNumber
    var licenseValidUntil by UserTable.licenseValidUntil

    fun toResponse() = UserResponse(
        id            = id.value,
        username      = username,
        email         = email,
        firstName     = firstName,
        lastName      = lastName,
        licenseNumber = licenseNumber,
        licenseValidUntil = licenseValidUntil,
    )
}

// ── Internal credential holder (not a DTO — never leaves the service layer) ──

data class UserCredentials(val id: Int, val username: String)

// ── Service ──────────────────────────────────────────────────────────────────

class UserService(private val database: Database) {

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    suspend fun create(dto: UserRegistration): Int =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity.new {
                username          = dto.username
                email             = dto.email
                passwordHash      = hashPassword(dto.password)
                firstName         = dto.firstName
                lastName          = dto.lastName
                licenseNumber     = dto.licenseNumber
                licenseValidUntil = dto.licenseValidUntil
            }.id.value
        }

    suspend fun read(id: Int): UserResponse? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity.findById(id)?.toResponse()
        }

    suspend fun findByCredentials(username: String, password: String): UserCredentials? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity
                .find { UserTable.username eq username }
                .firstOrNull()
                ?.takeIf { it.passwordHash == hashPassword(password) }
                ?.let { UserCredentials(it.id.value, it.username) }
        }

    suspend fun update(id: Int, dto: UserUpdate) =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity.findByIdAndUpdate(id) { entity ->
                dto.email?.let            { entity.email             = it }
                dto.password?.let         { entity.passwordHash      = hashPassword(it) }
                dto.firstName?.let        { entity.firstName         = it }
                dto.lastName?.let         { entity.lastName          = it }
                dto.licenseNumber?.let    { entity.licenseNumber     = it }
                dto.licenseValidUntil?.let { entity.licenseValidUntil = it }
            }
        }

    suspend fun delete(id: Int) =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity.findById(id)?.delete()
        }
}
