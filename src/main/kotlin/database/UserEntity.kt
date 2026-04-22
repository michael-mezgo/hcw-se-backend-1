package at.ac.hcw.se.database

import at.ac.hcw.se.business.User
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(UserTable)

    var username          by UserTable.username
    var email             by UserTable.email
    var passwordHash      by UserTable.passwordHash
    var firstName         by UserTable.firstName
    var lastName          by UserTable.lastName
    var licenseNumber     by UserTable.licenseNumber
    var licenseValidUntil by UserTable.licenseValidUntil
    var isAdmin           by UserTable.isAdmin
    var isLocked          by UserTable.isLocked
    var preferredCurrency by UserTable.preferredCurrency

    fun toDomain() = User(
        id                = id.value,
        username          = username,
        email             = email,
        firstName         = firstName,
        lastName          = lastName,
        licenseNumber     = licenseNumber,
        licenseValidUntil = licenseValidUntil,
        isAdmin           = isAdmin,
        isLocked          = isLocked,
        preferredCurrency = preferredCurrency,
    )
}
