package at.ac.hcw.se

import at.ac.hcw.se.database.DatabaseSessionStorage
import at.ac.hcw.se.database.SessionTable
import at.ac.hcw.se.database.UserService
import at.ac.hcw.se.database.UserTable
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases(): Pair<UserService, DatabaseSessionStorage> {
    val database = connectToDatabase(embedded = true)
    transaction(database) {
        SchemaUtils.create(UserTable, SessionTable)
    }
    val sessionStorage = DatabaseSessionStorage(database)
    val userService = UserService(database, sessionStorage)
    runBlocking { userService.ensureAdminExists() }
    return userService to sessionStorage
}

/**
 * Creates a database connection for Exposed ORM.
 *
 * Set [embedded] to false and provide postgres.url / postgres.user / postgres.password
 * in application.yaml to use a real PostgreSQL instance.
 */
fun Application.connectToDatabase(embedded: Boolean): Database {
    return if (embedded) {
        log.info("Using embedded H2 database; set embedded=false to use PostgreSQL")
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "root",
            password = ""
        )
    } else {
        val url = environment.config.property("postgres.url").getString()
        log.info("Connecting to PostgreSQL at $url")
        Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = environment.config.property("postgres.user").getString(),
            password = environment.config.property("postgres.password").getString()
        )
    }
}
