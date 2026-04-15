package at.ac.hcw.se

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.OffsetDateTime

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

    fun getUrl(blobName: String): String =
        containerClient.getBlobClient(blobName).blobUrl

    fun getSignedUrl(blobName: String, expiryMinutes: Long = 60): String {
        val sasPermission = BlobSasPermission().setReadPermission(true)
        val sasValues = BlobServiceSasSignatureValues(
            OffsetDateTime.now().plusMinutes(expiryMinutes),
            sasPermission
        )
        val blobClient = containerClient.getBlobClient(blobName)
        val sasToken = blobClient.generateSas(sasValues)
        return "${blobClient.blobUrl}?$sasToken"
    }
}
