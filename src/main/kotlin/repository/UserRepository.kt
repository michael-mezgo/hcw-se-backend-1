package at.ac.hcw.se.repository

interface UserRepository {
    suspend fun existsById(id: Int): Boolean
}
