package at.ac.hcw.se.service

import at.ac.hcw.se.business.BookingResult
import at.ac.hcw.se.business.Car
import at.ac.hcw.se.dto.CarCreateRequest
import at.ac.hcw.se.dto.CarUpdate
import at.ac.hcw.se.repository.CarRepository

class CarService(
    private val carRepository: CarRepository,
) {

    suspend fun create(dto: CarCreateRequest): Int =
        carRepository.create(dto)

    suspend fun listAll(): List<Car> =
        carRepository.listAll()

    suspend fun listAllAvailable(): List<Car> =
        carRepository.listAvailable()

    suspend fun getById(id: Int): Car? =
        carRepository.findById(id)

    suspend fun update(id: Int, dto: CarUpdate): Boolean =
        carRepository.update(id, dto)

    suspend fun delete(id: Int): Boolean =
        carRepository.delete(id)

    suspend fun book(carId: Int, userId: Int): BookingResult =
        carRepository.book(carId, userId)

    suspend fun unbook(carId: Int): BookingResult =
        carRepository.unbook(carId)

    suspend fun listBookedByUser(userId: Int): List<Car> =
        carRepository.listBookedByUser(userId)
}
