package at.ac.hcw.se.repository.exposed

import at.ac.hcw.se.BlobStorageService
import at.ac.hcw.se.business.BookingResult
import at.ac.hcw.se.business.Car
import at.ac.hcw.se.business.FuelType
import at.ac.hcw.se.business.Transmission
import at.ac.hcw.se.database.CarEntity
import at.ac.hcw.se.database.CarTable
import at.ac.hcw.se.database.UserEntity
import at.ac.hcw.se.dto.CarCreateRequest
import at.ac.hcw.se.dto.CarUpdate
import at.ac.hcw.se.repository.CarRepository
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ExposedCarRepository(
    private val database: Database,
    private val blobStorage: BlobStorageService? = null,
) : CarRepository {

    override suspend fun create(dto: CarCreateRequest): Int =
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
                latitude = dto.location.latitude
                longitude = dto.location.longitude
            }.id.value
        }

    override suspend fun listAll(): List<Car> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            CarEntity.all().map { it.toDomain() }
        }

    override suspend fun listAvailable(): List<Car> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            CarEntity.find { CarTable.booked_by.isNull() }
                .map { it.toDomain() }
        }

    override suspend fun findById(id: Int): Car? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            CarEntity.findById(id)?.toDomain()
        }

    override suspend fun update(id: Int, dto: CarUpdate): Boolean =
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

    override suspend fun delete(id: Int): Boolean =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val car = CarEntity.findById(id) ?: return@newSuspendedTransaction false
            val imageName = car.image
            car.delete()
            if (imageName.isNotEmpty()) blobStorage?.delete(imageName)
            true
        }

    override suspend fun book(carId: Int, userId: Int): BookingResult =
        newSuspendedTransaction(Dispatchers.IO, database) {
            CarEntity.findById(carId)
                ?: return@newSuspendedTransaction BookingResult.CarNotFound
            UserEntity.findById(userId)
                ?: return@newSuspendedTransaction BookingResult.UserNotFound

            val updatedRows = CarTable.update(
                where = { (CarTable.id eq carId) and CarTable.booked_by.isNull() }
            ) {
                it[booked_by] = userId
            }

            if (updatedRows == 0) BookingResult.CarUnavailable
            else BookingResult.CarBooked
        }

    override suspend fun unbook(carId: Int): BookingResult =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val car = CarEntity.findById(carId)
                ?: return@newSuspendedTransaction BookingResult.CarNotFound
            if (car.booked_by == null) {
                return@newSuspendedTransaction BookingResult.CarUnavailable
            }
            car.booked_by = null
            BookingResult.CarUnbooked
        }

    override suspend fun listBookedByUser(userId: Int): List<Car> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            CarEntity.find { CarTable.booked_by eq userId }
                .map { it.toDomain() }
        }
}
