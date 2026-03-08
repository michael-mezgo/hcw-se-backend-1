package at.ac.hcw.se.database

import at.ac.hcw.se.dto.UserSession
import io.ktor.server.sessions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object SessionTable : Table("sessions") {
    val sessionId = varchar("session_id", 128)
    val userId    = integer("user_id")
    val data      = text("data")
    override val primaryKey = PrimaryKey(sessionId)
}

class DatabaseSessionStorage(private val database: Database) : SessionStorage {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun write(id: String, value: String): Unit =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val userId = runCatching { json.decodeFromString<UserSession>(value).userId }.getOrDefault(-1)
            SessionTable.deleteWhere { sessionId eq id }
            SessionTable.insert {
                it[sessionId] = id
                it[SessionTable.userId] = userId
                it[data] = value
            }
        }

    override suspend fun read(id: String): String =
        newSuspendedTransaction(Dispatchers.IO, database) {
            SessionTable.selectAll().where { SessionTable.sessionId eq id }
                .firstOrNull()?.get(SessionTable.data)
        } ?: throw NoSuchElementException("Session $id not found")

    override suspend fun invalidate(id: String): Unit =
        newSuspendedTransaction(Dispatchers.IO, database) {
            SessionTable.deleteWhere { sessionId eq id }
        }

    suspend fun invalidateForUser(userId: Int): Unit =
        newSuspendedTransaction(Dispatchers.IO, database) {
            SessionTable.deleteWhere { SessionTable.userId eq userId }
        }
}

/** Session serializer that uses kotlinx.serialization JSON so the stored format is predictable. */
class KotlinxJsonSessionSerializer<T>(private val serializer: KSerializer<T>) : SessionSerializer<T> {
    private val json = Json { ignoreUnknownKeys = true }
    override fun serialize(session: T): String = json.encodeToString(serializer, session)
    override fun deserialize(text: String): T = json.decodeFromString(serializer, text)
}
