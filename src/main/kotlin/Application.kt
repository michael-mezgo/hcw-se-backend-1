package at.ac.hcw.se

import at.ac.hcw.se.routes.configureAdminRoutes
import at.ac.hcw.se.routes.configureCarRoutes
import at.ac.hcw.se.routes.configureCurrencyRoutes
import at.ac.hcw.se.routes.configureUserRoutes
import at.ac.hcw.se.service.CurrencyService
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val dotenv = dotenv { ignoreIfMissing = true }
    val azureConnectionString = dotenv["AZURE_STORAGE_CONNECTION_STRING"]
    val blobStorage = if (azureConnectionString != null) {
        BlobStorageService(
            connectionString = azureConnectionString,
            containerName = dotenv["AZURE_STORAGE_CONTAINER_NAME"] ?: "rental-documents",
        )
    } else {
        log.warn("AZURE_STORAGE_CONNECTION_STRING not found; blob storage routes will not be available")
        null
    }
    val apiKey = dotenv["API_KEY_CURRENCY"] ?: error("API key is not set")
    val wsdlServiceUrl = dotenv["WSDL_URL"] ?: "http://localhost:5125"
    CurrencyService.init(apiKey, wsdlServiceUrl)

    configureHTTP()
    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureDatabases(blobStorage)
    configureSecurity()
    configureUserRoutes(blobStorage)
    configureAdminRoutes(blobStorage)
    configureCarRoutes(blobStorage)
    configureCurrencyRoutes()
    configureRouting()
}
