package at.ac.hcw.se.database

import org.jetbrains.exposed.dao.id.IntIdTable

object UserTable : IntIdTable("users") {
    val username          = varchar("username", 255).uniqueIndex()
    val email             = varchar("email", 255).uniqueIndex()
    val passwordHash      = varchar("password_hash", 255)
    val firstName         = varchar("first_name", 255)
    val lastName          = varchar("last_name", 255)
    val licenseNumber     = varchar("license_number", 255)
    val licenseValidUntil = varchar("license_valid_until", 10)  // ISO 8601: YYYY-MM-DD
    val isAdmin           = bool("is_admin").default(false)
    val isLocked          = bool("is_locked").default(false)
}
