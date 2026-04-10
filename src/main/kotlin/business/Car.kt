package at.ac.hcw.se.business

import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.dto.CoordinateDto

class Car(
    val id: Int,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: Double,
    val description: String,
    val imageUrl: String,
    val transmission: String,
    val power: Int,
    val fuelType: String,
    val isAvailable: Boolean,
    val location: Coordinate,
) {

    fun toResponse() = CarResponse(
        id = id,
        manufacturer = manufacturer,
        model = model,
        year = year,
        pricePerDay = pricePerDay,
        description = description,
        imageUrl = imageUrl,
        transmission = transmission,
        power = power,
        fuelType = fuelType,
        isAvailable = isAvailable,
        location = CoordinateDto(location.latitude, location.longitude),
    )
}