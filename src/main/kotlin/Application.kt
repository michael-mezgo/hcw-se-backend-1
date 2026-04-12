package at.ac.hcw.se

import at.ac.hcw.se.routes.configureAdminRoutes
import at.ac.hcw.se.routes.configureCarRoutes
import at.ac.hcw.se.routes.configureBlobRoutes
import at.ac.hcw.se.routes.configureCurrencyRoutes
import at.ac.hcw.se.routes.configureUserRoutes
import at.ac.hcw.se.service.CurrencyService
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val azureConnectionString = dotenv()["AZURE_STORAGE_CONNECTION_STRING"]
    print(CurrencyService.getSupportedCurrencies())

    configureHTTP()
    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureDatabases()
    configureSecurity()
    configureUserRoutes()
    configureAdminRoutes()
    configureCarRoutes()
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
    configureCurrencyRoutes()
    configureRouting()
}
