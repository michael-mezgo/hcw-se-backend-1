package at.ac.hcw.se.service

import at.ac.hcw.se.business.User
import at.ac.hcw.se.database.UserEntity
import at.ac.hcw.se.database.UserTable
import at.ac.hcw.se.dto.AdminUserCreate
import at.ac.hcw.se.dto.AdminUserUpdate
import at.ac.hcw.se.dto.UserCredentials
import at.ac.hcw.se.dto.UserRegistration
import at.ac.hcw.se.dto.UserUpdate
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.mindrot.jbcrypt.BCrypt

object UserService {

    private lateinit var database: Database

    fun init(database: Database) {
        this.database = database
    }

    private fun hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

    private fun checkPassword(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)

    suspend fun create(dto: UserRegistration): Int =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity.new {
                username = dto.username
                email = dto.email
                passwordHash = hashPassword(dto.password)
                firstName = dto.firstName
                lastName = dto.lastName
                licenseNumber = dto.licenseNumber
                licenseValidUntil = dto.licenseValidUntil
                preferredCurrency = dto.preferredCurrency
            }.id.value
        }

    suspend fun adminCreate(dto: AdminUserCreate): Int =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity.new {
                username = dto.username
                email = dto.email
                passwordHash = hashPassword(dto.password)
                firstName = dto.firstName
                lastName = dto.lastName
                licenseNumber = dto.licenseNumber
                licenseValidUntil = dto.licenseValidUntil
                isAdmin = dto.isAdmin
                preferredCurrency = dto.preferredCurrency
            }.id.value
        }

    suspend fun read(id: Int): User? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity.findById(id)?.toDomain()
        }

    suspend fun listAll(): List<User> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity.all().map { it.toDomain() }
        }

    suspend fun findByCredentials(username: String, password: String): UserCredentials? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            UserEntity
                .find { UserTable.username eq username }
                .firstOrNull()
                ?.takeIf { checkPassword(password, it.passwordHash) && !it.isLocked }
                ?.let { UserCredentials(it.id.value, it.username, it.isAdmin) }
        }

    suspend fun update(id: Int, dto: UserUpdate): Boolean =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val entity = UserEntity.findById(id) ?: return@newSuspendedTransaction false
            dto.email?.let { entity.email = it }
            dto.password?.let { entity.passwordHash = hashPassword(it) }
            dto.firstName?.let { entity.firstName = it }
            dto.lastName?.let { entity.lastName = it }
            dto.licenseNumber?.let { entity.licenseNumber = it }
            dto.licenseValidUntil?.let { entity.licenseValidUntil = it }
            dto.preferredCurrency?.let { entity.preferredCurrency = it }
            true
        }

    suspend fun adminUpdate(id: Int, dto: AdminUserUpdate): Boolean =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val entity = UserEntity.findById(id) ?: return@newSuspendedTransaction false
            dto.email?.let { entity.email = it }
            dto.password?.let { entity.passwordHash = hashPassword(it) }
            dto.firstName?.let { entity.firstName = it }
            dto.lastName?.let { entity.lastName = it }
            dto.licenseNumber?.let { entity.licenseNumber = it }
            dto.licenseValidUntil?.let { entity.licenseValidUntil = it }
            dto.isAdmin?.let { entity.isAdmin = it }
            dto.isLocked?.let { entity.isLocked = it }
            dto.preferredCurrency?.let { entity.preferredCurrency = it }
            true
        }

    suspend fun delete(id: Int): Boolean =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val entity = UserEntity.findById(id) ?: return@newSuspendedTransaction false
            entity.delete()
            true
        }

    suspend fun ensureAdminExists() =
        newSuspendedTransaction(Dispatchers.IO, database) {
            if (UserEntity.find { UserTable.username eq "Admin" }.empty()) {
                UserEntity.new {
                    username = "Admin"
                    email = "admin@local"
                    passwordHash = hashPassword("Admin")
                    firstName = "Admin"
                    lastName = "Admin"
                    licenseNumber = "N/A"
                    licenseValidUntil = "9999-12-31"
                    isAdmin = true
                }
            }
        }
}