package at.ac.hcw.se.service

import at.ac.hcw.se.BlobStorageService
import at.ac.hcw.se.business.Car
import at.ac.hcw.se.database.CarEntity
import at.ac.hcw.se.database.CarTable
import at.ac.hcw.se.business.FuelType
import at.ac.hcw.se.business.Transmission
import at.ac.hcw.se.dto.CarCreateRequest
import at.ac.hcw.se.dto.CarUpdate
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object CarService {

    private lateinit var database: Database
    private var blobStorage: BlobStorageService? = null

    fun init(database: Database, blobStorage: BlobStorageService? = null) {
        this.database = database
        this.blobStorage = blobStorage
    }

    private fun resolveImageUrl(blobName: String): String =
        blobStorage?.getSignedUrl(blobName, expiryMinutes = 15) ?: blobName

    suspend fun create(dto: CarCreateRequest): Int =
        newSuspendedTransaction(Dispatchers.IO, database) {
            CarEntity.new {
                manufacturer = dto.manufacturer
                model = dto.model
                year = dto.year
                price_per_day = dto.pricePerDay
                description = dto.description
                image = dto.imageUrl
                transmission = Transmission.valueOf(dto.transmission)
                power = dto.power
                fuel_type = FuelType.valueOf(dto.fuelType)
                is_available = true
                latitude = dto.location.latitude
                longitude = dto.location.longitude
            }.id.value
        }

    suspend fun listAllAvailable(): List<Car> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            CarEntity.find { CarTable.is_available eq true }.map { it.toDomain(::resolveImageUrl) }
        }

    suspend fun getById(id: Int): Car? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            CarEntity.findById(id)?.toDomain(::resolveImageUrl)
        }

    suspend fun update(id: Int, dto: CarUpdate): Boolean =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val car = CarEntity.findById(id) ?: return@newSuspendedTransaction false
            dto.manufacturer?.let { car.manufacturer = it }
            dto.model?.let { car.model = it }
            dto.year?.let { car.year = it }
            dto.pricePerDay?.let { car.price_per_day = it }
            dto.description?.let { car.description = it }
            dto.imageUrl?.let { car.image = it }
            dto.transmission?.let { car.transmission = Transmission.valueOf(it) }
            dto.power?.let { car.power = it }
            dto.fuelType?.let { car.fuel_type = FuelType.valueOf(it) }
            dto.location?.let {
                car.latitude = it.latitude
                car.longitude = it.longitude
            }
            true
        }

    suspend fun delete(id: Int): Boolean =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val car = CarEntity.findById(id) ?: return@newSuspendedTransaction false
            car.delete()
            true
        }
}