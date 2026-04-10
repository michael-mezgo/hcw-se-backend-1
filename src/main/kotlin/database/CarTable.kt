package at.ac.hcw.se.database

import at.ac.hcw.se.business.FuelType
import at.ac.hcw.se.business.Transmission
import org.jetbrains.exposed.dao.id.IntIdTable

object CarTable : IntIdTable("cars") {
    val manufacturer = varchar("manufacturer", 255)
    val model = varchar("model", 255)
    val year = integer("year")
    val price_per_day = double("price_per_day")
    val description = varchar("description", 1000)
    val image_url = varchar("image_url", 1000)
    val transmission = enumerationByName("transmission", 50, Transmission::class)
    val power = integer("power")
    val fuel_type = enumerationByName("fuel_type", 50, FuelType::class)
    val is_available = bool("is_available")
    val latitude = double("latitude")
    val longitude = double("longitude")
}
