package at.ac.hcw.se.dto

import kotlinx.serialization.Serializable

/** Sent by the client when creating a new account. */
@Serializable
data class UserRegistration(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val licenseNumber: String,
    /** ISO 8601 date string, e.g. "2030-12-31" */
    val licenseValidUntil: String,
)

/** Sent by the client when authenticating. */
@Serializable
data class UserLoginRequest(val username: String, val password: String)

/** Returned after a successful login. */
@Serializable
data class LoginResponse(val userId: Int, val isAdmin: Boolean)

/** Sent by the client when updating an existing account (all fields optional). */
@Serializable
data class UserUpdate(
    val email: String? = null,
    val password: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val licenseNumber: String? = null,
    /** ISO 8601 date string, e.g. "2030-12-31" */
    val licenseValidUntil: String? = null,
)

/** Returned to the client — never includes the password hash. */
@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val licenseNumber: String,
    val licenseValidUntil: String,
    val isAdmin: Boolean = false,
    val isLocked: Boolean = false,
)

/** Stored in the session cookie after a successful login. */
@Serializable
data class UserSession(val userId: Int, val username: String, val isAdmin: Boolean = false)

/** Sent by an admin when creating a new user account (optionally with admin privileges). */
@Serializable
data class AdminUserCreate(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val licenseNumber: String,
    val licenseValidUntil: String,
    val isAdmin: Boolean = false,
)

/** Sent by an admin when updating any user account (all fields optional). */
@Serializable
data class AdminUserUpdate(
    val email: String? = null,
    val password: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val licenseNumber: String? = null,
    val licenseValidUntil: String? = null,
    val isAdmin: Boolean? = null,
    val isLocked: Boolean? = null,
)
