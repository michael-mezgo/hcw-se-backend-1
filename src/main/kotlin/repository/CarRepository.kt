package at.ac.hcw.se.repository

import at.ac.hcw.se.business.BookingResult
import at.ac.hcw.se.business.Car
import at.ac.hcw.se.dto.CarCreateRequest
import at.ac.hcw.se.dto.CarUpdate

interface CarRepository {
    suspend fun create(dto: CarCreateRequest): Int
    suspend fun listAvailable(): List<Car>
    suspend fun findById(id: Int): Car?
    suspend fun update(id: Int, dto: CarUpdate): Boolean
    suspend fun delete(id: Int): Boolean
    suspend fun book(carId: Int, userId: Int): BookingResult
    suspend fun unbook(carId: Int): BookingResult
}
