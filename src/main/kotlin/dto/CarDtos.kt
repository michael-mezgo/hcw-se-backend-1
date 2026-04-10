package at.ac.hcw.se.dto

import kotlinx.serialization.Serializable

@Serializable
data class CoordinateDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class CarCreateRequest(
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: Double,
    val description: String,
    val imageUrl: String,
    val transmission: String,
    val power: Int,
    val fuelType: String,
    val location: CoordinateDto,
)

@Serializable
data class CarUpdate(
    val manufacturer: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val pricePerDay: Double? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val transmission: String? = null,
    val power: Int? = null,
    val fuelType: String? = null,
    val location: CoordinateDto? = null,
)

@Serializable
data class CarResponse(
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
    val location: CoordinateDto,
)