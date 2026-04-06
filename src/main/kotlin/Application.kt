package at.ac.hcw.se

import at.ac.hcw.se.routes.configureAdminRoutes
import at.ac.hcw.se.routes.configureBlobRoutes
import at.ac.hcw.se.routes.configureCurrencyRoutes
import at.ac.hcw.se.routes.configureUserRoutes
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val azureConnectionString = dotenv()["AZURE_STORAGE_CONNECTION_STRING"]

    configureHTTP()
    configureSerialization()
    configureMonitoring()
    val userService = configureDatabases()
    configureSecurity()
    val dotenv = dotenv { ignoreIfMissing = true }

    if(azureConnectionString != null) {
        val blobStorage = BlobStorageService(
            connectionString = dotenv["AZURE_STORAGE_CONNECTION_STRING"]
                ?: error("AZURE_STORAGE_CONNECTION_STRING not set in .env or environment"),
            containerName = dotenv["AZURE_STORAGE_CONTAINER_NAME"] ?: "rental-documents",
        )
        configureBlobRoutes(blobStorage)
    } else {
        log.warn("AZURE_STORAGE_CONNECTION_STRING not found; blob storage routes will not be available")
    }

    configureUserRoutes(userService)
    configureAdminRoutes(userService)
    configureCurrencyRoutes()
    configureRouting()
}
