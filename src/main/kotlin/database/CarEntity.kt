package at.ac.hcw.se.database

import at.ac.hcw.se.business.Car
import at.ac.hcw.se.business.Coordinate
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class CarEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CarEntity>(CarTable)
    var manufacturer by CarTable.manufacturer
    var model by CarTable.model
    var year by CarTable.year
    var price_per_day by CarTable.price_per_day
    var description by CarTable.description
    var image by CarTable.image_url
    var transmission by CarTable.transmission
    var power by CarTable.power
    var fuel_type by CarTable.fuel_type
    var is_available by CarTable.is_available
    var latitude by CarTable.latitude
    var longitude by CarTable.longitude

    fun toDomain() = Car(
        id           = id.value,
        manufacturer = manufacturer,
        model        = model,
        year         = year,
        pricePerDay  = price_per_day,
        description  = description,
        imageUrl     = image,
        transmission = transmission.name,
        power        = power,
        fuelType     = fuel_type.name,
        isAvailable  = is_available,
        location     = Coordinate(latitude, longitude),
    )
}
