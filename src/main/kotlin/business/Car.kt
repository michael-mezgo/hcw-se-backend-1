package at.ac.hcw.se.business

import at.ac.hcw.se.BlobStorageService
import at.ac.hcw.se.dto.AdminCarResponse
import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.dto.CoordinateDto

class Car(
    val id: Int,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: Double,
    val description: String,
    val imageName: String,
    val transmission: String,
    val power: Int,
    val fuelType: String,
    val bookedBy: User?,
    val location: Coordinate,
) {
    val isAvailable: Boolean get() = bookedBy == null

    private fun resolveImageUrl(blobStorageService: BlobStorageService?): String {
        if (imageName.isEmpty() || blobStorageService == null) return ""
        return blobStorageService.getSignedUrl(blobName = imageName, expiryMinutes = 15)
    }

    fun toAdminResponse(blobStorageService: BlobStorageService?) = AdminCarResponse(
        id = id,
        manufacturer = manufacturer,
        model = model,
        year = year,
        pricePerDay = pricePerDay,
        description = description,
        imageUrl = resolveImageUrl(blobStorageService),
        transmission = transmission,
        power = power,
        fuelType = fuelType,
        isAvailable = isAvailable,
        location = CoordinateDto(location.latitude, location.longitude),
        bookedBy = bookedBy?.toResponse(),
    )

    fun toResponse(blobStorageService: BlobStorageService?) = CarResponse(
        id = id,
        manufacturer = manufacturer,
        model = model,
        year = year,
        pricePerDay = pricePerDay,
        description = description,
        imageUrl = resolveImageUrl(blobStorageService),
        transmission = transmission,
        power = power,
        fuelType = fuelType,
        isAvailable = isAvailable,
        location = CoordinateDto(location.latitude, location.longitude),
    )
}