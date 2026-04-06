package at.ac.hcw.se

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import java.io.ByteArrayOutputStream
import java.io.InputStream

class BlobStorageService(
    connectionString: String,
    containerName: String,
) {
    private val containerClient: BlobContainerClient =
        BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .getBlobContainerClient(containerName)
            .also { if (!it.exists()) it.create() }

    fun upload(blobName: String, data: ByteArray, overwrite: Boolean = true) {
        containerClient.getBlobClient(blobName)
            .upload(data.inputStream(), data.size.toLong(), overwrite)
    }

    fun upload(blobName: String, stream: InputStream, length: Long, overwrite: Boolean = true) {
        containerClient.getBlobClient(blobName).upload(stream, length, overwrite)
    }

    fun download(blobName: String): ByteArray {
        val out = ByteArrayOutputStream()
        containerClient.getBlobClient(blobName).downloadStream(out)
        return out.toByteArray()
    }

    fun delete(blobName: String): Boolean =
        containerClient.getBlobClient(blobName).deleteIfExists()

    fun exists(blobName: String): Boolean =
        containerClient.getBlobClient(blobName).exists()

    fun list(): List<String> =
        containerClient.listBlobs().map { it.name }
}
