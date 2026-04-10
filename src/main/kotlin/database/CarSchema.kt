package at.ac.hcw.se.database

import at.ac.hcw.se.dto.CarResponse
import at.ac.hcw.se.dto.CarUpdate
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object CarSchema : IntIdTable("cars") {
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
}

class CarEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CarEntity>(CarSchema)
    var manufacturer by CarSchema.manufacturer
    var model by CarSchema.model
    var year by CarSchema.year
    var price_per_day by CarSchema.price_per_day
    var description by CarSchema.description
    var image by CarSchema.image_url
    var transmission by CarSchema.transmission
    var power by CarSchema.power
    var fuel_type by CarSchema.fuel_type
    var is_available by CarSchema.is_available

    fun toResponse() = CarResponse(
        id = id.value,
        manufacturer = manufacturer,
        model = model,
        year = year,
        pricePerDay = price_per_day,
        description = description,
        imageUrl = image,
        transmission = transmission.name,
        power = power,
        fuelType = fuel_type.name,
        isAvailable = is_available
    )
}

class CarService(private val database: Database) {
    suspend fun listAllAvailableCars(): List<CarEntity> =
        newSuspendedTransaction(Dispatchers.IO, database) {
        CarEntity.find { CarSchema.is_available eq true }.toList()
    }

    suspend fun getSingleCar(id: Int): CarEntity? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            return@newSuspendedTransaction CarEntity.findById(id)
        }

    suspend fun update(id: Int, dto: CarUpdate): Boolean =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val car = CarEntity.findById(id) ?: return@newSuspendedTransaction false
            dto.manufacturer?.let   { car.manufacturer= it }
            dto.model?.let          { car.model= it }
            dto.year?.let           { car.year= it }
            dto.pricePerDay?.let    { car.price_per_day= it }
            dto.description?.let    { car.description= it }
            dto.imageUrl?.let       { car.image = it }
            dto.transmission?.let   { car.transmission = Transmission.valueOf(it) }
            dto.power?.let          { car.power = it }
            dto.fuelType?.let       { car.fuel_type = FuelType.valueOf(it) }
            true
        }

    suspend fun delete(id: Int): Boolean =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val car = CarEntity.findById(id) ?: return@newSuspendedTransaction false
            car.delete()
            true
        }
}


//getallcars(avaialble=true/false, manuf, name, transm), getsinglecar(alles), updatecar(als admin), deletecar(), addcar()