package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Vehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VehicleRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveVehicle_authenticated_createsDocument() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VehicleRepository(firestore)

        val uniqueVin = "1HGCR2F83HA" + (100000 + (Math.random() * 899999).toInt())
        val vehicle = Vehicle(
            vin = uniqueVin,
            make = "Honda",
            model = "Accord",
            year = 2017,
            engine = "2.4L I4",
            hasLowConfidence = false,
            createdBy = uid
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveVehicle(vehicle)
        }
        assertTrue("Vehicle save should succeed", saveResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getVehicle(uniqueVin)
        }
        assertTrue(fetched.isSuccess)
        assertNotNull(fetched.getOrNull())
        assertEquals("Honda", fetched.getOrNull()?.make)
    }

    @Test
    fun getVehicle_unauthenticated_canReadPublicReport() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VehicleRepository(firestore)

        val uniqueVin = "4T1BF1FK5EU" + (100000 + (Math.random() * 899999).toInt())
        val vehicle = Vehicle(
            vin = uniqueVin,
            make = "Toyota",
            model = "Camry",
            year = 2021,
            engine = "2.5L I4",
            hasLowConfidence = false,
            createdBy = uid
        )
        repository.saveVehicle(vehicle)

        // Sign out to simulate public anonymous viewer of a vehicle report
        auth.signOut()

        val publicReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getVehicle(uniqueVin)
        }
        assertTrue("Public read of vehicle report should succeed", publicReadResult.isSuccess)
        assertEquals("Toyota", publicReadResult.getOrNull()?.make)
    }

    @Test
    fun addEvent_authenticated_createsEventAndChainsHash() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VehicleRepository(firestore)

        val uniqueVin = "1FTFW1ED4MF" + (100000 + (Math.random() * 899999).toInt())
        val vehicle = Vehicle(
            vin = uniqueVin,
            make = "Ford",
            model = "F-150",
            year = 2020,
            engine = "3.5L V6",
            hasLowConfidence = false,
            createdBy = uid
        )
        repository.saveVehicle(vehicle)

        val eventResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addEvent(
                vin = uniqueVin,
                eventType = "service",
                title = "10k Service",
                description = "Oil and filters",
                date = "2024-03-01",
                odometerKm = 10000L,
                verificationLevel = "verified",
                serviceProvider = "Official Ford Service"
            )
        }

        if (eventResult.isFailure) {
            println("ADD_EVENT_FAILURE: ${eventResult.exceptionOrNull()}")
            eventResult.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Adding event should succeed: ${eventResult.exceptionOrNull()}", eventResult.isSuccess)
        val event = eventResult.getOrThrow()
        assertTrue("Event hash should be non-empty SHA-256", event.eventHash.isNotEmpty())
        assertEquals(64, event.eventHash.length)

        // Second event to verify chaining
        val secondResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addEvent(
                vin = uniqueVin,
                eventType = "odometer",
                title = "Odometer Check",
                description = "Routine reading",
                date = "2024-06-01",
                odometerKm = 15000L,
                verificationLevel = "dealer_certified",
                serviceProvider = "Certified Dealer"
            )
        }
        if (secondResult.isFailure) {
            println("SECOND_EVENT_FAILURE: ${secondResult.exceptionOrNull()}")
            secondResult.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Second event should succeed: ${secondResult.exceptionOrNull()}", secondResult.isSuccess)
        val secondEvent = secondResult.getOrThrow()
        assertEquals(event.eventHash, secondEvent.prevHash)
    }

    @Test
    fun addEvent_unauthenticated_failsFast() = runBlocking {
        auth.signOut()
        val repository = VehicleRepository(firestore)

        val uniqueVin = "WBA3A5C55CF" + (100000 + (Math.random() * 899999).toInt())
        val eventResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addEvent(
                vin = uniqueVin,
                eventType = "service",
                title = "Brake Check",
                description = "",
                date = "2024-01-01",
                odometerKm = 5000L,
                verificationLevel = "self_declared",
                serviceProvider = ""
            )
        }
        assertTrue("Unauthenticated add event must fail", eventResult.isFailure)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_tester@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
