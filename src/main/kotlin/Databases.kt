package at.ac.hcw.se

import at.ac.hcw.se.database.CarTable
import at.ac.hcw.se.database.UserTable
import at.ac.hcw.se.service.CarService
import at.ac.hcw.se.service.UserService
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import io.github.cdimascio.dotenv.dotenv
import java.util.UUID

val dotenv = dotenv {
    ignoreIfMissing = true
}

fun Application.configureDatabases(blobStorage: BlobStorageService? = null) {
    val embedded = environment.config.propertyOrNull("embedded")?.getString()?.toBooleanStrictOrNull() ?: true
    val database = connectToDatabase(embedded = embedded)
    transaction(database) {
        SchemaUtils.create(UserTable)
        SchemaUtils.create(CarTable)
    }
    UserService.init(database)
    CarService.init(database, blobStorage)
    runBlocking { UserService.ensureAdminExists() }
}

/**
 * Creates a database connection for Exposed ORM.
 *
 * Set [embedded] to false in application.yaml and provide POSTGRES_URL / POSTGRES_USER / POSTGRES_PASSWORD
 * as environment variables (e.g. via .env) to use a real PostgreSQL instance.
 */
fun Application.connectToDatabase(embedded: Boolean): Database {
    return if (embedded) {
        log.info("Using embedded H2 database; set embedded=false to use PostgreSQL")
        Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "root",
            password = ""
        )
    } else {
        val url = dotenv["POSTGRES_URL"]
            ?: error("POSTGRES_URL is not set in .env or environment")
        log.info("Connecting to PostgreSQL at $url")
        Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = dotenv["POSTGRES_USER"] ?: error("POSTGRES_USER is not set in .env or environment"),
            password = dotenv["POSTGRES_PASSWORD"] ?: error("POSTGRES_PASSWORD is not set in .env or environment")
        )
    }
}
