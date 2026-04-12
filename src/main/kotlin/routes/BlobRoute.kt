package at.ac.hcw.se.routes

import at.ac.hcw.se.BlobStorageService
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureBlobRoutes(blobStorage: BlobStorageService) {
    routing {
        route("/blobs") {

            get({
                tags("Blobs")
                summary = "List all blobs"
                description = "Returns names of all blobs in the configured Azure Storage container."
                response {
                    HttpStatusCode.OK to { description = "List of blob names"; body<List<String>>() }
                }
            }) {
                call.respond(HttpStatusCode.OK, blobStorage.list())
            }

            get("/{name}", {
                tags("Blobs")
                summary = "Download a blob"
                description = "Downloads the blob with the given name as raw bytes."
                request {
                    pathParameter<String>("name") { description = "Blob name" }
                }
                response {
                    HttpStatusCode.OK to { description = "Blob content" }
                    HttpStatusCode.NotFound to { description = "Blob not found" }
                }
            }) {
                val name = call.parameters["name"]!!
                if (!blobStorage.exists(name)) {
                    return@get call.respond(HttpStatusCode.NotFound)
                }
                call.respondBytes(blobStorage.download(name), ContentType.Application.OctetStream)
            }

            post("/{name}", {
                tags("Blobs")
                summary = "Upload a blob"
                description = "Uploads raw bytes as a blob with the given name. Overwrites existing blobs."
                request {
                    pathParameter<String>("name") { description = "Blob name" }
                    body<ByteArray> { description = "Raw blob content" }
                }
                response {
                    HttpStatusCode.Created to { description = "Blob uploaded successfully" }
                }
            }) {
                val name = call.parameters["name"]!!
                val bytes = call.receive<ByteArray>()
                blobStorage.upload(name, bytes)
                call.respond(HttpStatusCode.Created, "Uploaded $name")
            }

            delete("/{name}", {
                tags("Blobs")
                summary = "Delete a blob"
                description = "Deletes the blob with the given name."
                request {
                    pathParameter<String>("name") { description = "Blob name" }
                }
                response {
                    HttpStatusCode.NoContent to { description = "Blob deleted" }
                    HttpStatusCode.NotFound to { description = "Blob not found" }
                }
            }) {
                val name = call.parameters["name"]!!
                val deleted = blobStorage.delete(name)
                call.respond(if (deleted) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
            }
        }
    }
}
