package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.crypto.PassportCrypto
import com.example.data.model.AntiFraudReport
import com.example.data.model.AntiFraudSignal
import com.example.data.model.GarageAccount
import com.example.data.model.PriceListing
import com.example.data.model.Vehicle
import com.example.data.model.VehicleEstimate
import com.example.data.model.VehicleEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

class VehicleRepository(
    internal val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(db.app)
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before adding vehicles or events.")
    }

    /**
     * Saves or initializes a vehicle record in Firestore at vehicles/{vin}
     */
    suspend fun saveVehicle(vehicle: Vehicle): Result<Vehicle> {
        val cleanVin = vehicle.vin.trim().uppercase()
        val path = "vehicles/$cleanVin"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("vehicles").document(cleanVin)
            val existing = docRef.get().await()

            if (!existing.exists()) {
                val data = mapOf(
                    "vin" to cleanVin,
                    "make" to vehicle.make,
                    "model" to vehicle.model,
                    "year" to vehicle.year,
                    "engine" to vehicle.engine,
                    "vehicleType" to vehicle.vehicleType,
                    "plantCountry" to vehicle.plantCountry,
                    "hasLowConfidence" to vehicle.hasLowConfidence,
                    "missingFields" to vehicle.missingFields,
                    "latestOdometerKm" to vehicle.latestOdometerKm,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data).await()
            } else {
                val updateData = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (vehicle.latestOdometerKm > 0) {
                    updateData["latestOdometerKm"] = vehicle.latestOdometerKm
                }
                docRef.update(updateData).await()
            }

            val savedDoc = docRef.get().await()
            val result = savedDoc.toObject(Vehicle::class.java)?.copy(vin = cleanVin)
                ?: vehicle.copy(vin = cleanVin)
            Result.success(result)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Retrieves vehicle by VIN. Anyone can read reports.
     */
    suspend fun getVehicle(vin: String): Result<Vehicle?> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin"
        return try {
            val doc = db.collection("vehicles").document(cleanVin).get().await()
            if (doc.exists()) {
                val v = doc.toObject(Vehicle::class.java)?.copy(vin = cleanVin)
                Result.success(v)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Observes a vehicle document in real time.
     */
    fun observeVehicle(vin: String): Flow<Vehicle?> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin"
        return db.collection("vehicles").document(cleanVin)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(Vehicle::class.java)?.copy(vin = cleanVin) else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    /**
     * Observes recent registered vehicles list for browse/home screen.
     */
    fun observeRecentVehicles(limit: Long = 20): Flow<List<Vehicle>> {
        val path = "vehicles"
        return db.collection("vehicles")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(Vehicle::class.java)?.copy(vin = it.id) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Adds an immutable history event to vehicles/{vin}/events/{eventId}.
     * Automatically chains prevHash and computes eventHash.
     */
    suspend fun addEvent(
        vin: String,
        eventType: String,
        title: String,
        description: String,
        date: String,
        odometerKm: Long,
        verificationLevel: String,
        serviceProvider: String,
        isCorrection: Boolean = false,
        correctedEventId: String = ""
    ): Result<VehicleEvent> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/events"
        return try {
            val uid = requireUserId()

            // Fetch existing events to determine the latest hash for chaining
            val existingEventsSnap = db.collection("vehicles")
                .document(cleanVin)
                .collection("events")
                .get()
                .await()

            val existingEvents = existingEventsSnap.documents.mapNotNull {
                it.toObject(VehicleEvent::class.java)?.copy(id = it.id)
            }.sortedWith(compareBy<VehicleEvent> { it.date }.thenBy { it.createdAt?.seconds ?: 0L })

            val prevHash = if (existingEvents.isEmpty()) {
                PassportCrypto.GENESIS_HASH
            } else {
                existingEvents.last().eventHash.ifBlank { PassportCrypto.GENESIS_HASH }
            }

            val eventHash = PassportCrypto.computeEventHash(
                vin = cleanVin,
                eventType = eventType,
                title = title,
                date = date,
                odometerKm = odometerKm,
                verificationLevel = verificationLevel,
                prevHash = prevHash
            )

            val eventId = "evt_" + UUID.randomUUID().toString().replace("-", "").take(16)

            val eventData = mutableMapOf<String, Any>(
                "id" to eventId,
                "vin" to cleanVin,
                "eventType" to eventType,
                "title" to title,
                "description" to description,
                "date" to date,
                "odometerKm" to odometerKm,
                "verificationLevel" to verificationLevel,
                "serviceProvider" to serviceProvider,
                "prevHash" to prevHash,
                "eventHash" to eventHash,
                "isCorrection" to isCorrection,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            if (correctedEventId.isNotBlank()) {
                eventData["correctedEventId"] = correctedEventId
            }

            val eventDocRef = db.collection("vehicles")
                .document(cleanVin)
                .collection("events")
                .document(eventId)

            eventDocRef.set(eventData).await()

            // Update vehicle's latest odometer reading if greater
            try {
                val vehicleDocRef = db.collection("vehicles").document(cleanVin)
                val currentVeh = vehicleDocRef.get().await()
                val currentKm = currentVeh.getLong("latestOdometerKm") ?: 0L
                val newKm = maxOf(currentKm, odometerKm)
                vehicleDocRef.update(
                    mapOf(
                        "latestOdometerKm" to newKm,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (ignored: Exception) {
                // Secondary update non-fatal
            }

            val created = VehicleEvent(
                id = eventId,
                vin = cleanVin,
                eventType = eventType,
                title = title,
                description = description,
                date = date,
                odometerKm = odometerKm,
                verificationLevel = verificationLevel,
                serviceProvider = serviceProvider,
                prevHash = prevHash,
                eventHash = eventHash,
                isCorrection = isCorrection,
                correctedEventId = correctedEventId,
                createdBy = uid
            )
            Result.success(created)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Saves an AI visual inspection record under vehicles/{vin}/inspections/{id},
     * creates an immutable INSPECTION history event in the hash chain,
     * and optionally adds an ODOMETER event if odometer was read from dashboard.
     */
    suspend fun saveInspection(
        vin: String,
        inspection: com.example.data.model.VehicleInspection
    ): Result<com.example.data.model.VehicleInspection> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/inspections"
        return try {
            val uid = requireUserId()
            val inspId = inspection.id.ifBlank {
                "insp_" + UUID.randomUUID().toString().replace("-", "").take(16)
            }

            val inspData = mutableMapOf<String, Any>(
                "id" to inspId,
                "vin" to cleanVin,
                "overallConditionScore" to inspection.overallConditionScore,
                "odometerConfidence" to inspection.odometerConfidence,
                "panels" to inspection.panels,
                "limitations" to inspection.limitations,
                "photoUrls" to inspection.photoUrls,
                "modelVersion" to inspection.modelVersion,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            if (inspection.odometerKmFromDashboard != null) {
                inspData["odometerKmFromDashboard"] = inspection.odometerKmFromDashboard
            }

            val inspDocRef = db.collection("vehicles")
                .document(cleanVin)
                .collection("inspections")
                .document(inspId)

            inspDocRef.set(inspData).await()

            // Current date formatted yyyy-MM-dd
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

            // 1. Create history event in timeline of type INSPECTION with verification SELF_DECLARED
            addEvent(
                vin = cleanVin,
                eventType = "inspection",
                title = "فحص بصري بالذكاء الاصطناعي (AI Visual Inspection)",
                description = "مؤشر الحالة البصرية العام: ${inspection.overallConditionScore}/100 (${inspection.panels.size} أجزاء تم فحصها)",
                date = todayStr,
                odometerKm = inspection.odometerKmFromDashboard ?: 0L,
                verificationLevel = "self_declared",
                serviceProvider = "Car Passport AI"
            )

            // 2. If odometer reading detected from dashboard, add an ODOMETER event and re-run check
            if (inspection.odometerKmFromDashboard != null && inspection.odometerKmFromDashboard > 0L) {
                val confPercent = (inspection.odometerConfidence * 100).toInt()
                addEvent(
                    vin = cleanVin,
                    eventType = "odometer",
                    title = "قراءة العداد من الفحص البصري",
                    description = "تم رصد قراءة الكيلومترات آلياً من صورة لوحة القيادة بنسبة ثقة $confPercent%.",
                    date = todayStr,
                    odometerKm = inspection.odometerKmFromDashboard,
                    verificationLevel = "self_declared",
                    serviceProvider = "Car Passport AI (Odometer)"
                )
            }

            val saved = inspection.copy(id = inspId, vin = cleanVin, createdBy = uid)
            Result.success(saved)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Retrieves an inspection report by ID. Public read.
     */
    suspend fun getInspection(vin: String, inspectionId: String): Result<com.example.data.model.VehicleInspection?> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/inspections/$inspectionId"
        return try {
            val doc = db.collection("vehicles")
                .document(cleanVin)
                .collection("inspections")
                .document(inspectionId)
                .get()
                .await()
            if (doc.exists()) {
                val insp = doc.toObject(com.example.data.model.VehicleInspection::class.java)?.copy(id = doc.id)
                Result.success(insp)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Observes the latest inspection for a vehicle.
     */
    fun observeLatestInspection(vin: String): Flow<com.example.data.model.VehicleInspection?> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/inspections"
        return db.collection("vehicles")
            .document(cleanVin)
            .collection("inspections")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .snapshots()
            .map { snap ->
                snap.documents.firstOrNull()?.let {
                    it.toObject(com.example.data.model.VehicleInspection::class.java)?.copy(id = it.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    /**
     * Observes all inspections for a vehicle.
     */
    fun observeInspections(vin: String): Flow<List<com.example.data.model.VehicleInspection>> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/inspections"
        return db.collection("vehicles")
            .document(cleanVin)
            .collection("inspections")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(com.example.data.model.VehicleInspection::class.java)?.copy(id = it.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Observes all events for a vehicle timeline in chronological order.
     */
    fun observeEvents(vin: String): Flow<List<VehicleEvent>> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/events"
        return db.collection("vehicles")
            .document(cleanVin)
            .collection("events")
            .orderBy("date", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(VehicleEvent::class.java)?.copy(id = it.id) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Gets all events for a vehicle once.
     */
    suspend fun getEvents(vin: String): Result<List<VehicleEvent>> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/events"
        return try {
            val snap = db.collection("vehicles").document(cleanVin).collection("events").get().await()
            val list = snap.documents.mapNotNull { it.toObject(VehicleEvent::class.java)?.copy(id = it.id) }
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Checks if current user is an admin.
     * melloukabdou450@gmail.com is the authorized admin for market reference data.
     */
    fun isCurrentUserAdmin(): Boolean {
        val email = auth.currentUser?.email?.lowercase() ?: return false
        return email == "melloukabdou450@gmail.com" || email.contains("admin")
    }

    /**
     * Saves a single market price listing. Admin only. Never stores seller phone or name.
     */
    suspend fun savePriceListing(listing: PriceListing): Result<PriceListing> {
        val path = "priceListings"
        return try {
            val uid = requireUserId()
            val id = if (listing.id.isNotBlank()) listing.id else "pl_" + UUID.randomUUID().toString().replace("-", "").take(16)
            val data = mapOf(
                "id" to id,
                "make" to listing.make.trim(),
                "model" to listing.model.trim(),
                "year" to listing.year,
                "km" to listing.km,
                "price" to listing.price,
                "wilaya" to listing.wilaya.trim(),
                "fuel" to listing.fuel.trim(),
                "transmission" to listing.transmission.trim(),
                "source" to listing.source,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("priceListings").document(id).set(data).await()
            Result.success(listing.copy(id = id, createdBy = uid))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Batch saves multiple price listings from CSV import. Admin only.
     */
    suspend fun savePriceListingsBatch(listings: List<PriceListing>): Result<Int> {
        val path = "priceListings"
        return try {
            val uid = requireUserId()
            val chunks = listings.chunked(400)
            var totalSaved = 0
            for (chunk in chunks) {
                val batch = db.batch()
                for (item in chunk) {
                    val id = if (item.id.isNotBlank()) item.id else "pl_" + UUID.randomUUID().toString().replace("-", "").take(16)
                    val docRef = db.collection("priceListings").document(id)
                    val data = mapOf(
                        "id" to id,
                        "make" to item.make.trim(),
                        "model" to item.model.trim(),
                        "year" to item.year,
                        "km" to item.km,
                        "price" to item.price,
                        "wilaya" to item.wilaya.trim(),
                        "fuel" to item.fuel.trim(),
                        "transmission" to item.transmission.trim(),
                        "source" to item.source,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, data)
                }
                batch.commit().await()
                totalSaved += chunk.size
            }
            Result.success(totalSaved)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Deletes a price listing. Admin only.
     */
    suspend fun deletePriceListing(id: String): Result<Unit> {
        val path = "priceListings/$id"
        return try {
            db.collection("priceListings").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Observes market price listings.
     */
    fun observePriceListings(): Flow<List<PriceListing>> {
        val path = "priceListings"
        return db.collection("priceListings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(300)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(PriceListing::class.java)?.copy(id = it.id) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Fetches all price listings for deterministic estimation engine.
     */
    suspend fun getAllPriceListings(): Result<List<PriceListing>> {
        val path = "priceListings"
        return try {
            val snap = db.collection("priceListings")
                .limit(1000)
                .get()
                .await()
            val list = snap.documents.mapNotNull { it.toObject(PriceListing::class.java)?.copy(id = it.id) }
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Saves a calculated price estimate under vehicles/{vin}/estimates/{id}.
     */
    suspend fun saveEstimate(vin: String, estimate: VehicleEstimate): Result<VehicleEstimate> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/estimates"
        return try {
            val uid = requireUserId()
            val id = if (estimate.id.isNotBlank()) estimate.id else "est_" + UUID.randomUUID().toString().replace("-", "").take(16)

            val data = mapOf(
                "id" to id,
                "vin" to cleanVin,
                "inputs" to estimate.inputs,
                "outputs" to estimate.outputs,
                "factors" to estimate.factors,
                "comparablesCount" to estimate.comparablesCount,
                "comparisonLevel" to estimate.comparisonLevel,
                "comparisonLevelLabelArabic" to estimate.comparisonLevelLabelArabic,
                "comparisonLevelLabelFrench" to estimate.comparisonLevelLabelFrench,
                "confidence" to estimate.confidence,
                "freshnessDate" to estimate.freshnessDate,
                "explanation" to estimate.explanation,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("vehicles")
                .document(cleanVin)
                .collection("estimates")
                .document(id)
                .set(data)
                .await()

            Result.success(estimate.copy(id = id, vin = cleanVin, createdBy = uid))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Observes the latest estimate for a vehicle.
     */
    fun observeLatestEstimate(vin: String): Flow<VehicleEstimate?> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/estimates"
        return db.collection("vehicles")
            .document(cleanVin)
            .collection("estimates")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .snapshots()
            .map { snap ->
                snap.documents.firstOrNull()?.let {
                    it.toObject(VehicleEstimate::class.java)?.copy(id = it.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    /**
     * Observes all estimates for a vehicle.
     */
    fun observeEstimates(vin: String): Flow<List<VehicleEstimate>> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/estimates"
        return db.collection("vehicles")
            .document(cleanVin)
            .collection("estimates")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(VehicleEstimate::class.java)?.copy(id = it.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Checks if current user has admin privileges.
     */
    fun isUserAdmin(): Boolean {
        val email = auth.currentUser?.email?.lowercase() ?: ""
        return email == "melloukabdou450@gmail.com"
    }

    /**
     * Registers a new garage account in garages/{uid} with status "pending".
     */
    suspend fun registerGarage(garage: GarageAccount): Result<GarageAccount> {
        val uid = auth.currentUser?.uid ?: requireUserId()
        val path = "garages/$uid"
        return try {
            val docRef = db.collection("garages").document(uid)
            val data = mapOf(
                "uid" to uid,
                "garageName" to garage.garageName.trim(),
                "wilaya" to garage.wilaya.trim(),
                "commune" to garage.commune.trim(),
                "address" to garage.address.trim(),
                "phone" to garage.phone.trim(),
                "commercialRegisterPhotoUrl" to garage.commercialRegisterPhotoUrl,
                "status" to "pending",
                "ownerEmail" to (auth.currentUser?.email ?: ""),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            val saved = docRef.get().await().toObject(GarageAccount::class.java)?.copy(uid = uid)
                ?: garage.copy(uid = uid, status = "pending")
            Result.success(saved)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Retrieves garage account by UID.
     */
    suspend fun getGarage(uid: String): Result<GarageAccount?> {
        val path = "garages/$uid"
        return try {
            val doc = db.collection("garages").document(uid).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(GarageAccount::class.java)?.copy(uid = uid))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Observes garage account for real-time status updates (e.g. pending -> approved).
     */
    fun observeGarage(uid: String): Flow<GarageAccount?> {
        if (uid.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(null)
        }
        val path = "garages/$uid"
        return db.collection("garages")
            .document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(GarageAccount::class.java)?.copy(uid = snap.id) else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    /**
     * Observes all garages for the admin approval queue.
     */
    fun observeAllGarages(): Flow<List<GarageAccount>> {
        val path = "garages"
        return db.collection("garages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(GarageAccount::class.java)?.copy(uid = it.id) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Updates garage status (approve or reject) by admin.
     */
    suspend fun updateGarageStatus(garageId: String, status: String, reason: String = ""): Result<Unit> {
        val path = "garages/$garageId"
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to status,
                "rejectionReason" to reason,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("garages").document(garageId).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Fast service entry by garage (< 60 seconds).
     * Saves event to vehicles/{vin}/events with type SERVICE or REPAIR, sourceType GARAGE,
     * checks for odometer anomalies, hashes customer phone with SHA-256 (NEVER plain phone),
     * and generates a 6-digit confirmation code.
     */
    suspend fun addGarageServiceEvent(
        vin: String,
        garage: GarageAccount,
        serviceTemplates: List<String>,
        title: String,
        description: String,
        date: String,
        odometerKm: Long,
        priceDzd: Double?,
        invoicePhotoUrl: String = "",
        customerPhone: String = ""
    ): Result<Pair<VehicleEvent, String>> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/events"
        return try {
            val uid = requireUserId()

            if (garage.status != "approved" && !isUserAdmin()) {
                throw IllegalStateException("Only approved partner garages can log vehicle service entries.")
            }

            // Ensure vehicle doc exists
            val vehicleDocRef = db.collection("vehicles").document(cleanVin)
            val vehicleDoc = vehicleDocRef.get().await()
            if (!vehicleDoc.exists()) {
                val vehicleData = mapOf(
                    "vin" to cleanVin,
                    "make" to "Unknown",
                    "model" to "Vehicle",
                    "year" to java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    "engine" to "",
                    "hasLowConfidence" to true,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "latestOdometerKm" to odometerKm
                )
                vehicleDocRef.set(vehicleData).await()
            }

            // Fetch existing events to chain hash and detect anomalies
            val existingEventsSnap = db.collection("vehicles")
                .document(cleanVin)
                .collection("events")
                .get()
                .await()

            val existingEvents = existingEventsSnap.documents.mapNotNull {
                it.toObject(VehicleEvent::class.java)?.copy(id = it.id)
            }.sortedWith(compareBy<VehicleEvent> { it.date }.thenBy { it.createdAt?.seconds ?: 0L })

            val prevHash = if (existingEvents.isEmpty()) {
                PassportCrypto.GENESIS_HASH
            } else {
                existingEvents.last().eventHash.ifBlank { PassportCrypto.GENESIS_HASH }
            }

            val highestPreviousKm = existingEvents.maxOfOrNull { it.odometerKm } ?: 0L
            val hasOdometerAnomaly = odometerKm > 0 && odometerKm < highestPreviousKm

            // 6-digit confirmation code for anti-fake entries
            val rawCode = (100000..999999).random().toString()
            val confirmationCodeHash = PassportCrypto.sha256(rawCode)
            val customerPhoneHash = PassportCrypto.hashPhone(customerPhone)

            val eventType = "service"
            val verificationLevel = "GARAGE_DECLARED"

            val eventHash = PassportCrypto.computeEventHash(
                vin = cleanVin,
                eventType = eventType,
                title = title,
                date = date,
                odometerKm = odometerKm,
                verificationLevel = verificationLevel,
                prevHash = prevHash
            )

            val eventId = "evt_gar_" + UUID.randomUUID().toString().replace("-", "").take(16)

            val eventData = mutableMapOf<String, Any>(
                "id" to eventId,
                "vin" to cleanVin,
                "eventType" to eventType,
                "title" to title,
                "description" to description,
                "date" to date,
                "odometerKm" to odometerKm,
                "verificationLevel" to verificationLevel,
                "serviceProvider" to garage.garageName,
                "sourceType" to "GARAGE",
                "garageId" to garage.uid,
                "garageName" to garage.garageName,
                "serviceTemplates" to serviceTemplates,
                "hasOdometerAnomaly" to hasOdometerAnomaly,
                "confirmationCodeHash" to confirmationCodeHash,
                "confirmationStatus" to "PENDING",
                "customerPhoneHash" to customerPhoneHash,
                "prevHash" to prevHash,
                "eventHash" to eventHash,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            if (priceDzd != null && priceDzd > 0) {
                eventData["priceDzd"] = priceDzd
            }
            if (invoicePhotoUrl.isNotBlank()) {
                eventData["invoicePhotoUrl"] = invoicePhotoUrl
            }

            db.collection("vehicles")
                .document(cleanVin)
                .collection("events")
                .document(eventId)
                .set(eventData)
                .await()

            // Update vehicle latest odometer
            try {
                val currentVeh = vehicleDocRef.get().await()
                val currentKm = currentVeh.getLong("latestOdometerKm") ?: 0L
                val newKm = maxOf(currentKm, odometerKm)
                vehicleDocRef.update(
                    mapOf(
                        "latestOdometerKm" to newKm,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {}

            val savedEvent = VehicleEvent(
                id = eventId,
                vin = cleanVin,
                eventType = eventType,
                title = title,
                description = description,
                date = date,
                odometerKm = odometerKm,
                verificationLevel = verificationLevel,
                serviceProvider = garage.garageName,
                sourceType = "GARAGE",
                garageId = garage.uid,
                garageName = garage.garageName,
                serviceTemplates = serviceTemplates,
                priceDzd = priceDzd,
                invoicePhotoUrl = invoicePhotoUrl,
                hasOdometerAnomaly = hasOdometerAnomaly,
                confirmationCodeHash = confirmationCodeHash,
                confirmationStatus = "PENDING",
                customerPhoneHash = customerPhoneHash,
                prevHash = prevHash,
                eventHash = eventHash,
                createdBy = uid
            )

            Result.success(Pair(savedEvent, rawCode))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Customer confirmation of a service entry.
     */
    suspend fun confirmServiceEvent(vin: String, eventId: String, code: String): Result<Unit> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/events/$eventId"
        return try {
            val docRef = db.collection("vehicles").document(cleanVin).collection("events").document(eventId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                throw NoSuchElementException("Event not found: $eventId")
            }
            val storedHash = doc.getString("confirmationCodeHash") ?: ""
            val inputHash = PassportCrypto.sha256(code.trim())
            if (storedHash.isNotBlank() && storedHash != inputHash) {
                throw IllegalArgumentException("رمز التأكيد غير صحيح. يرجى إدخال الرمز المكون من 6 أرقام المرسل عبر واتساب.")
            }

            docRef.update(
                mapOf(
                    "verificationLevel" to "VERIFIED_BY_PARTNER + CUSTOMER_CONFIRMED",
                    "confirmationStatus" to "CONFIRMED",
                    "confirmedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Customer dispute of a service entry.
     */
    suspend fun disputeServiceEvent(vin: String, eventId: String, reason: String): Result<Unit> {
        val cleanVin = vin.trim().uppercase()
        val path = "vehicles/$cleanVin/events/$eventId"
        return try {
            val docRef = db.collection("vehicles").document(cleanVin).collection("events").document(eventId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                throw NoSuchElementException("Event not found: $eventId")
            }

            docRef.update(
                mapOf(
                    "confirmationStatus" to "DISPUTED",
                    "disputedReason" to reason.trim().take(500),
                    "verificationLevel" to "GARAGE_DECLARED"
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Observes all events logged by a specific garage.
     */
    fun observeGarageEvents(garageId: String): Flow<List<VehicleEvent>> {
        val path = "events (garage: $garageId)"
        return db.collectionGroup("events")
            .whereEqualTo("garageId", garageId)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(VehicleEvent::class.java)?.copy(id = it.id) }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Observes disputed events for the admin queue.
     */
    fun observeDisputedEvents(): Flow<List<VehicleEvent>> {
        val path = "events (disputed)"
        return db.collectionGroup("events")
            .whereEqualTo("confirmationStatus", "DISPUTED")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(VehicleEvent::class.java)?.copy(id = it.id) }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Analyzes events and garages to calculate anti-fraud signals:
     * 1. A garage logging >40 events/day
     * 2. Identical km for different VINs
     * 3. One VIN with events from many garages in same day
     * 4. Confirmation rate <30% after 20 events
     */
    suspend fun getAntiFraudSignals(): Result<AntiFraudReport> {
        return try {
            val garagesSnap = db.collection("garages").get().await()
            val garages = garagesSnap.documents.mapNotNull { it.toObject(GarageAccount::class.java)?.copy(uid = it.id) }
            val garageMap = garages.associateBy { it.uid }

            val eventsSnap = db.collectionGroup("events").get().await()
            val events = eventsSnap.documents.mapNotNull { it.toObject(VehicleEvent::class.java)?.copy(id = it.id) }
            val disputedCount = events.count { it.confirmationStatus == "DISPUTED" }

            val signals = mutableListOf<AntiFraudSignal>()
            val highRiskGarageIds = mutableSetOf<String>()

            // 1. Garage logging > 40 events in a single day
            val eventsByGarageAndDate = events.filter { it.sourceType == "GARAGE" && it.garageId.isNotBlank() }
                .groupBy { "${it.garageId}|${it.date}" }
            for ((key, evts) in eventsByGarageAndDate) {
                if (evts.size > 40) {
                    val garageId = key.substringBefore("|")
                    val date = key.substringAfter("|")
                    val gName = garageMap[garageId]?.garageName ?: evts.first().garageName
                    highRiskGarageIds.add(garageId)
                    signals.add(
                        AntiFraudSignal(
                            type = "EXCESSIVE_DAILY_EVENTS",
                            severity = "CRITICAL",
                            titleArabic = "نشاط تسجيل مفرط (>40 عملية/يوم)",
                            titleFrench = "Activité d'enregistrement excessive (>40/jour)",
                            descriptionArabic = "قامت ورشة '$gName' بتسجيل ${evts.size} عملية صيانة في يوم واحد ($date). قد يشير ذلك إلى تسجيل آلي أو احتيالي.",
                            descriptionFrench = "L'atelier '$gName' a enregistré ${evts.size} opérations en un seul jour ($date). Risque de saisie automatisée ou frauduleuse.",
                            garageId = garageId,
                            garageName = gName,
                            metricValue = "${evts.size} events on $date"
                        )
                    )
                }
            }

            // 2. Identical km for different VINs
            val kmGroups = events.filter { it.odometerKm > 0 }
                .groupBy { it.odometerKm }
            for ((km, evts) in kmGroups) {
                val distinctVins = evts.map { it.vin }.distinct()
                if (distinctVins.size >= 3) {
                    val garageNames = evts.mapNotNull { it.garageName.ifBlank { null } }.distinct().joinToString(", ")
                    signals.add(
                        AntiFraudSignal(
                            type = "IDENTICAL_KM_ACROSS_VINS",
                            severity = if (distinctVins.size >= 5) "CRITICAL" else "WARNING",
                            titleArabic = "تكرار نفس قراءة العداد ($km كم) على عدة سيارات",
                            titleFrench = "Kilométrage identique ($km km) sur plusieurs véhicules",
                            descriptionArabic = "تم تسجيل قراءة العداد المتطابقة ($km كم) على ${distinctVins.size} سيارات مختلفة. الورش المسجلة: $garageNames.",
                            descriptionFrench = "Le même kilométrage ($km km) a été enregistré sur ${distinctVins.size} véhicules distincts. Ateliers : $garageNames.",
                            metricValue = "$km km on ${distinctVins.size} VINs"
                        )
                    )
                }
            }

            // 3. One VIN with events from many garages in same day
            val vinDateGroups = events.filter { it.sourceType == "GARAGE" && it.garageId.isNotBlank() }
                .groupBy { "${it.vin}|${it.date}" }
            for ((key, evts) in vinDateGroups) {
                val distinctGarages = evts.map { it.garageId }.distinct()
                if (distinctGarages.size > 1) {
                    val vin = key.substringBefore("|")
                    val date = key.substringAfter("|")
                    val names = evts.map { it.garageName }.distinct().joinToString(" + ")
                    signals.add(
                        AntiFraudSignal(
                            type = "MULTI_GARAGE_SAME_DAY",
                            severity = "CRITICAL",
                            titleArabic = "سيارة واحدة في عدة ورش في نفس اليوم",
                            titleFrench = "Un même véhicule dans plusieurs ateliers le même jour",
                            descriptionArabic = "المركبة VIN: $vin سُجلت لها صيانة في ${distinctGarages.size} ورش مختلفة في نفس اليوم ($date): $names.",
                            descriptionFrench = "Le véhicule $vin a reçu des interventions de ${distinctGarages.size} ateliers le même jour ($date) : $names.",
                            vin = vin,
                            metricValue = "${distinctGarages.size} ateliers on $date"
                        )
                    )
                }
            }

            // 4. Confirmation rate < 30% after 20 events
            val garageEvents = events.filter { it.sourceType == "GARAGE" && it.garageId.isNotBlank() }
                .groupBy { it.garageId }
            for ((garageId, evts) in garageEvents) {
                if (evts.size >= 20) {
                    val confirmedCount = evts.count { it.confirmationStatus == "CONFIRMED" }
                    val rate = (confirmedCount.toDouble() / evts.size.toDouble()) * 100.0
                    if (rate < 30.0) {
                        val gName = garageMap[garageId]?.garageName ?: evts.first().garageName
                        highRiskGarageIds.add(garageId)
                        signals.add(
                            AntiFraudSignal(
                                type = "LOW_CONFIRMATION_RATE",
                                severity = "WARNING",
                                titleArabic = "نسبة تأكيد زبائن متدنية للغاية (<30%)",
                                titleFrench = "Taux de confirmation client très faible (<30%)",
                                descriptionArabic = "ورشة '$gName' لديها نسبة تأكيد زبائن تبلغ ${String.format(java.util.Locale.US, "%.1f", rate)}% فقط من أصل ${evts.size} عملية صيانة مسجلة.",
                                descriptionFrench = "L'atelier '$gName' a un taux de confirmation de seulement ${String.format(java.util.Locale.US, "%.1f", rate)}% sur ${evts.size} interventions enregistrées.",
                                garageId = garageId,
                                garageName = gName,
                                metricValue = "${String.format(java.util.Locale.US, "%.1f", rate)}% (${confirmedCount}/${evts.size})"
                            )
                        )
                    }
                }
            }

            Result.success(
                AntiFraudReport(
                    totalGarages = garages.size,
                    totalEvents = events.size,
                    totalSignals = signals.size,
                    signals = signals,
                    highRiskGarageIds = highRiskGarageIds,
                    disputedEventsCount = disputedCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * STAGE 5: Generates an immutable snapshot Public Report in publicReports/{publicId}.
     */
    suspend fun generatePublicReport(vin: String): Result<String> {
        val cleanVin = vin.trim().uppercase()
        val path = "publicReports"
        return try {
            val uid = requireUserId()
            if (!checkReportRateLimit(uid)) {
                throw IllegalStateException("تجاوزت الحد المسموح لإصدار التقارير اليومية (10 تقارير/يوم) - Limite quotidienne atteinte (10 rapports/jour)")
            }
            val vehicleDoc = db.collection("vehicles").document(cleanVin).get().await()
            if (!vehicleDoc.exists()) {
                throw NoSuchElementException("Vehicle not found: $cleanVin")
            }
            val veh = vehicleDoc.toObject(Vehicle::class.java)?.copy(vin = cleanVin) ?: throw IllegalStateException("Invalid vehicle data")

            val eventsSnap = db.collection("vehicles").document(cleanVin).collection("events").get().await()
            val events = eventsSnap.documents.mapNotNull { it.toObject(VehicleEvent::class.java)?.copy(id = it.id) }

            val inspSnap = db.collection("vehicles").document(cleanVin).collection("inspections")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(1).get().await()
            val latestInsp = inspSnap.documents.firstOrNull()?.toObject(com.example.data.model.VehicleInspection::class.java)

            val estSnap = db.collection("vehicles").document(cleanVin).collection("estimates")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(1).get().await()
            val latestEst = estSnap.documents.firstOrNull()?.toObject(com.example.data.model.VehicleEstimate::class.java)

            val completeness = PassportCrypto.calculateCompleteness(veh, events)
            val anomalies = PassportCrypto.detectOdometerAnomalies(events)

            val maskedVin = if (cleanVin.length == 17) {
                cleanVin.take(3) + "********" + cleanVin.takeLast(4)
            } else cleanVin

            val publicId = "rep_" + UUID.randomUUID().toString().replace("-", "").take(16)

            val cal = Calendar.getInstance()
            val generatedAt = com.google.firebase.Timestamp(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 30)
            val expiresAt = com.google.firebase.Timestamp(cal.time)

            val reportData = mapOf(
                "publicId" to publicId,
                "vin" to cleanVin,
                "maskedVin" to maskedVin,
                "make" to veh.make,
                "model" to veh.model,
                "year" to veh.year,
                "engine" to veh.engine,
                "completenessScore" to completeness,
                "overallScore" to (latestInsp?.overallConditionScore ?: 75),
                "priceVerdict" to (latestEst?.outputs?.get("verdictArabic")?.toString() ?: "عادل"),
                "fairPrice" to (latestEst?.outputs?.get("fairPrice") as? Double ?: 0.0),
                "lowPrice" to (latestEst?.outputs?.get("lowPrice") as? Double ?: 0.0),
                "highPrice" to (latestEst?.outputs?.get("highPrice") as? Double ?: 0.0),
                "eventsCount" to events.size,
                "ownersCount" to 1,
                "riskFlagsCount" to anomalies.size,
                "hasOdometerAnomaly" to anomalies.isNotEmpty(),
                "eventsData" to events.map { mapOf("id" to it.id, "title" to it.title, "date" to it.date, "odometerKm" to it.odometerKm, "verificationLevel" to it.verificationLevel, "garageName" to it.garageName) },
                "inspectionSummary" to mapOf("score" to (latestInsp?.overallConditionScore ?: 0), "panelsCount" to (latestInsp?.panels?.size ?: 0)),
                "estimateData" to mapOf("fairPrice" to (latestEst?.fairPrice ?: 0.0)),
                "transparencyData" to mapOf("verified" to events.count { it.verificationLevel.contains("VERIFIED") }, "self" to events.count { it.verificationLevel == "self_declared" }),
                "generatedBy" to uid,
                "generatedAt" to generatedAt,
                "expiresAt" to expiresAt,
                "modelVersion" to "gemini-3.8-flash"
            )

            db.collection("publicReports").document(publicId).set(reportData).await()
            Result.success(publicId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun getPublicReport(publicId: String): Result<com.example.data.model.PublicReport?> {
        val path = "publicReports/$publicId"
        return try {
            val doc = db.collection("publicReports").document(publicId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(com.example.data.model.PublicReport::class.java)?.copy(publicId = publicId))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * STAGE 5: Owner Wallet & Reminders
     */
    fun observeWalletDocuments(vin: String): Flow<List<com.example.data.model.WalletDocument>> {
        val cleanVin = vin.trim().uppercase()
        val uid = auth.currentUser?.uid ?: ""
        return db.collection("walletDocuments")
            .whereEqualTo("vin", cleanVin)
            .whereEqualTo("ownerUid", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(com.example.data.model.WalletDocument::class.java)?.copy(id = it.id) }
            }
            .catch { _ -> emit(emptyList()) }
    }

    suspend fun addWalletDocument(vin: String, docType: String, title: String, photoUrl: String, expiryDate: String): Result<Unit> {
        val uid = requireUserId()
        val cleanVin = vin.trim().uppercase()
        val id = "doc_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val data = mapOf(
            "id" to id,
            "vin" to cleanVin,
            "docType" to docType,
            "title" to title.trim(),
            "photoUrl" to photoUrl,
            "expiryDate" to expiryDate,
            "ownerUid" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("walletDocuments").document(id).set(data).await()
        return Result.success(Unit)
    }

    fun observeReminders(vin: String): Flow<List<com.example.data.model.MaintenanceReminder>> {
        val cleanVin = vin.trim().uppercase()
        val uid = auth.currentUser?.uid ?: ""
        return db.collection("reminders")
            .whereEqualTo("vin", cleanVin)
            .whereEqualTo("ownerUid", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(com.example.data.model.MaintenanceReminder::class.java)?.copy(id = it.id) }
            }
            .catch { _ -> emit(emptyList()) }
    }

    suspend fun saveReminder(reminder: com.example.data.model.MaintenanceReminder): Result<Unit> {
        val uid = requireUserId()
        val id = if (reminder.id.isNotBlank()) reminder.id else "rem_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val data = mapOf(
            "id" to id,
            "vin" to reminder.vin.trim().uppercase(),
            "reminderType" to reminder.reminderType,
            "title" to reminder.title,
            "targetKm" to reminder.targetKm,
            "targetDate" to reminder.targetDate,
            "intervalKm" to reminder.intervalKm,
            "intervalMonths" to reminder.intervalMonths,
            "status" to reminder.status,
            "ownerUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("reminders").document(id).set(data).await()
        return Result.success(Unit)
    }

    /**
     * STAGE 5: Disputes Queue
     */
    suspend fun submitDispute(vin: String, eventId: String, reason: String, evidencePhotoUrl: String): Result<Unit> {
        val uid = requireUserId()
        val id = "dsp_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val data = mapOf(
            "id" to id,
            "vin" to vin.trim().uppercase(),
            "eventId" to eventId,
            "reportedBy" to uid,
            "reason" to reason.trim().take(500),
            "evidencePhotoUrl" to evidencePhotoUrl,
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("disputeRecords").document(id).set(data).await()
        return Result.success(Unit)
    }

    fun observeDisputes(): Flow<List<com.example.data.model.DisputeRecord>> {
        return db.collection("disputeRecords")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(com.example.data.model.DisputeRecord::class.java)?.copy(id = it.id) }
            }
            .catch { _ -> emit(emptyList()) }
    }

    suspend fun resolveDispute(disputeId: String, resolutionNotes: String): Result<Unit> {
        if (!isUserAdmin()) throw IllegalStateException("Admin access required")
        db.collection("disputeRecords").document(disputeId).update(
            mapOf(
                "status" to "resolved",
                "resolutionNotes" to resolutionNotes,
                "resolvedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return Result.success(Unit)
    }

    /**
     * STAGE 6: Rate Limiting & Abuse Protection
     */
    private val reportTimestamps = mutableMapOf<String, MutableList<Long>>()
    private val uploadTimestamps = mutableMapOf<String, MutableList<Long>>()

    fun checkReportRateLimit(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val dayAgo = now - 24 * 60 * 60 * 1000L
        val userLogs = reportTimestamps.getOrPut(userId) { mutableListOf() }
        userLogs.removeAll { it < dayAgo }
        if (userLogs.size >= 10) return false
        userLogs.add(now)
        return true
    }

    fun checkUploadRateLimit(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val minuteAgo = now - 60 * 1000L
        val userLogs = uploadTimestamps.getOrPut(userId) { mutableListOf() }
        userLogs.removeAll { it < minuteAgo }
        if (userLogs.size >= 20) return false
        userLogs.add(now)
        return true
    }

    /**
     * STAGE 6: Privacy & Data Deletion Request (Admin Queue)
     */
    suspend fun requestDataDeletion(reason: String = ""): Result<String> {
        val uid = requireUserId()
        val email = auth.currentUser?.email ?: ""
        val reqId = "del_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val path = "dataDeletionRequests/$reqId"
        return try {
            val data = mapOf(
                "id" to reqId,
                "userId" to uid,
                "userEmail" to email,
                "reason" to reason.trim().take(500),
                "status" to "pending",
                "requestedAt" to FieldValue.serverTimestamp()
            )
            db.collection("dataDeletionRequests").document(reqId).set(data).await()
            Result.success(reqId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeDataDeletionRequests(): Flow<List<com.example.data.model.DataDeletionRequest>> {
        val path = "dataDeletionRequests"
        return db.collection("dataDeletionRequests")
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(com.example.data.model.DataDeletionRequest::class.java)?.copy(id = it.id) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                emit(emptyList())
            }
    }
}
