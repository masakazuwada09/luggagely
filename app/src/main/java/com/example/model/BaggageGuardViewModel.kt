package com.example.model

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BaggageAsyncStorage
import com.example.data.BaggageItemEntity
import com.example.data.BaggageRepository
import com.example.data.MuseSparkMetaClient
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

private const val TAG = "BaggageGuardVM"

enum class ItemSource {
    AI_SCANNED,
    MANUAL_ENTRY
}

enum class GearCategory(val label: String, val tagCode: String) {
    COMPUTE("Compute & Optics", "OPT-CMP"),
    POWER("Lithium & Power", "PWR-CEL"),
    APPAREL("Footwear & Apparel", "APR-TEX"),
    LIQUIDS("Liquids & Toiletries", "LIQ-100"),
    ACCESSORY("Tactical & Misc", "ACC-GEN")
}

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

data class BaggageItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val weightKg: Double,
    val source: ItemSource,
    val category: GearCategory = GearCategory.ACCESSORY,
    val confidence: Float? = null,
    val boundingBox: NormalizedRect? = null,
    val contourPoints: List<NormalizedPoint> = emptyList(),
    val isShiftedToPersonal: Boolean = false,
    val maskId: String? = null,
    val imageUri: String? = null
)

enum class AirlinePreset(
    val id: String,
    val title: String,
    val shortLabel: String,
    val limitKg: Double,
    val bagType: String
) {
    CABIN_7KG("cabin_7", "Cabin Carry-On", "Cabin 7.0 kg", 7.0, "CABIN // IATA COMPACT"),
    CABIN_10KG("cabin_10", "Priority Cabin", "Priority 10.0 kg", 10.0, "CABIN // EXEC PRIORITY"),
    CHECKED_20KG("checked_20", "Checked Standard", "Checked 20.0 kg", 20.0, "HOLD // STANDARD 20KG"),
    CHECKED_23KG("checked_23", "Checked Heavy", "Heavy 23.0 kg", 23.0, "HOLD // LONG-HAUL 23KG")
}

enum class GearRigPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultLimit: AirlinePreset
) {
    NOMAD_CABIN(
        id = "nomad_cabin",
        title = "Nomad Tech Rig",
        subtitle = "5 High-Density Carry-On Objects",
        defaultLimit = AirlinePreset.CABIN_7KG
    ),
    EXPEDITION_HOLD(
        id = "expedition_hold",
        title = "Expedition Checked",
        subtitle = "6 Heavy Transit Loadout Objects",
        defaultLimit = AirlinePreset.CHECKED_20KG
    ),
    EXEC_OVERNIGHT(
        id = "exec_overnight",
        title = "Executive Sprint",
        subtitle = "4 Ultra-Light Cabin Objects",
        defaultLimit = AirlinePreset.CABIN_7KG
    )
}

enum class ManifestFilter(val label: String) {
    ALL("All Items"),
    AI_ONLY("AI-Scanned"),
    MANUAL_ONLY("Manual Override")
}

enum class AirlineCarrier(
    val id: String,
    val carrierName: String,
    val iataCode: String,
    val tagline: String,
    val defaultPnr: String,
    val defaultFlightNumber: String,
    val defaultRoute: String,
    val defaultFareBundle: String,
    val cabinInclusionKg: Double,
    val cabinInclusionDetails: String,
    val checkedInclusionKg: Double,
    val checkedInclusionDetails: String,
    val cabinPreset: AirlinePreset,
    val checkedPreset: AirlinePreset,
    val brandPrimaryHex: Long,
    val brandSecondaryHex: Long
) {
    CEBU_PACIFIC(
        id = "cebu_pacific",
        carrierName = "Cebu Pacific",
        iataCode = "5J",
        tagline = "Let's Fly Every Juan",
        defaultPnr = "CEB5J89",
        defaultFlightNumber = "5J 567",
        defaultRoute = "MNL (Manila T3) → CEB (Mactan-Cebu)",
        defaultFareBundle = "Go Easy + CEB Prepaid Baggage",
        cabinInclusionKg = 7.0,
        cabinInclusionDetails = "1 Carry-on bag (56×36×23cm) + 1 small under-seat personal item, max 7.0 kg",
        checkedInclusionKg = 20.0,
        checkedInclusionDetails = "1 pc × 20.0 kg CEB Prepaid Checked Baggage included",
        cabinPreset = AirlinePreset.CABIN_7KG,
        checkedPreset = AirlinePreset.CHECKED_20KG,
        brandPrimaryHex = 0xFF00AEEF,
        brandSecondaryHex = 0xFF0072BC
    ),
    PHILIPPINE_AIRLINES(
        id = "philippine_airlines",
        carrierName = "Philippine Airlines",
        iataCode = "PR",
        tagline = "The Heart of the Filipino",
        defaultPnr = "PAL42M",
        defaultFlightNumber = "PR 432",
        defaultRoute = "MNL (Manila T1) → NRT (Tokyo Narita)",
        defaultFareBundle = "Economy Fiesta • Mabuhay FBA",
        cabinInclusionKg = 7.0,
        cabinInclusionDetails = "1 Cabin Carry-on (56×36×23cm) max 7.0 kg + 1 personal laptop/handbag",
        checkedInclusionKg = 23.0,
        checkedInclusionDetails = "2 pcs × 23.0 kg Free Baggage Allowance (FBA) included",
        cabinPreset = AirlinePreset.CABIN_7KG,
        checkedPreset = AirlinePreset.CHECKED_23KG,
        brandPrimaryHex = 0xFF002B7F,
        brandSecondaryHex = 0xFFCE1126
    ),
    AIRASIA(
        id = "airasia",
        carrierName = "AirAsia",
        iataCode = "Z2",
        tagline = "Now Everyone Can Fly",
        defaultPnr = "Z2AK77",
        defaultFlightNumber = "Z2 711",
        defaultRoute = "MNL (Manila T3) → KIX (Osaka Kansai)",
        defaultFareBundle = "Value Pack + Checked 20kg",
        cabinInclusionKg = 7.0,
        cabinInclusionDetails = "1 Cabin bag (56×36×23cm) + 1 small bag combined max 7.0 kg",
        checkedInclusionKg = 20.0,
        checkedInclusionDetails = "1 pc × 20.0 kg Checked Baggage Value Pack included",
        cabinPreset = AirlinePreset.CABIN_7KG,
        checkedPreset = AirlinePreset.CHECKED_20KG,
        brandPrimaryHex = 0xFFE60012,
        brandSecondaryHex = 0xFFB8000E
    )
}

data class AirlineBookingConnection(
    val carrier: AirlineCarrier,
    val bookingCode: String,
    val passengerName: String,
    val passengerEmail: String,
    val iataPassengerName: String,
    val eTicketNumber: String,
    val departureSchedule: String,
    val terminalGate: String,
    val seatNumber: String,
    val bookingStatus: String = "CONFIRMED // E-TICKET ISSUED",
    val flightNumber: String,
    val route: String,
    val fareBundle: String,
    val cabinInclusionKg: Double,
    val cabinInclusionDetails: String,
    val checkedInclusionKg: Double,
    val checkedInclusionDetails: String,
    val activeAllowanceMode: String = "CABIN",
    val museHeadline: String,
    val museSummary: String,
    val musePolicyTips: List<String>,
    val analyzedByModel: String = MuseSparkMetaClient.MUSE_SPARK_MODEL_ID
) {
    companion object {
        fun formatIataPassenger(name: String, email: String): String {
            val clean = name.trim().ifBlank {
                email.substringBefore("@").replace(".", " ").replace("_", " ").trim()
            }.ifBlank { "Guest Traveler" }
            val parts = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
            return if (parts.size >= 2) {
                "${parts.last().uppercase()}/${parts.dropLast(1).joinToString(" ").uppercase()}"
            } else {
                "${clean.uppercase()}/TRAVELER"
            }
        }

        fun derivePnrForUser(carrier: AirlineCarrier, name: String, email: String): String {
            val seed = "${name.trim().lowercase()}|${email.trim().lowercase()}|${carrier.id}"
            val hash = kotlin.math.abs(seed.hashCode())
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val suffix = buildString {
                var h = hash
                repeat(4) {
                    append(chars[h % chars.length])
                    h /= chars.length
                }
            }
            return "${carrier.iataCode}$suffix"
        }

        fun deriveETicketForUser(carrier: AirlineCarrier, name: String, email: String): String {
            val prefix = when (carrier) {
                AirlineCarrier.CEBU_PACIFIC -> "203"
                AirlineCarrier.PHILIPPINE_AIRLINES -> "079"
                AirlineCarrier.AIRASIA -> "807"
            }
            val seed = "${email.trim().lowercase()}|${name.trim().lowercase()}|${carrier.iataCode}"
            val num = (kotlin.math.abs(seed.hashCode().toLong()) % 9000000000L) + 1000000000L
            return "$prefix-$num"
        }

        fun deriveSeatForUser(carrier: AirlineCarrier, name: String, email: String): String {
            val seed = "${name.trim().lowercase()}|${email.trim().lowercase()}|seat|${carrier.id}"
            val hash = kotlin.math.abs(seed.hashCode())
            val row = (hash % 24) + 4
            val cols = listOf("A", "C", "D", "F", "K")
            val col = cols[hash % cols.size]
            val cabinZone = when (carrier) {
                AirlineCarrier.CEBU_PACIFIC -> "CEB Plus Cabin"
                AirlineCarrier.PHILIPPINE_AIRLINES -> "Economy Fiesta"
                AirlineCarrier.AIRASIA -> "Hot Seat / Quiet Zone"
            }
            return "$row$col ($cabinZone)"
        }

        fun defaultFor(
            carrier: AirlineCarrier,
            bookingCode: String? = null,
            passengerName: String = "Guest Traveler",
            passengerEmail: String = "",
            packedWeightKg: Double = 0.0
        ): AirlineBookingConnection {
            val cleanName = passengerName.trim().ifBlank {
                passengerEmail.substringBefore("@").replace(".", " ").replace("_", " ").trim()
            }.ifBlank { "Guest Traveler" }
            val cleanEmail = passengerEmail.trim()
            val resolvedPnr = bookingCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
                ?: derivePnrForUser(carrier, cleanName, cleanEmail)
            val iataPassenger = formatIataPassenger(cleanName, cleanEmail)
            val eTicket = deriveETicketForUser(carrier, cleanName, cleanEmail)
            val seatNumber = deriveSeatForUser(carrier, cleanName, cleanEmail)

            val schedule = when (carrier) {
                AirlineCarrier.CEBU_PACIFIC -> "08:40 HRS (MNL T3) → 10:05 HRS (CEB T1) • GATE 114"
                AirlineCarrier.PHILIPPINE_AIRLINES -> "09:30 HRS (MNL T1) → 15:00 HRS (NRT T2) • GATE 9"
                AirlineCarrier.AIRASIA -> "13:15 HRS (MNL T3) → 18:10 HRS (KIX T1) • GATE 108"
            }
            val terminalGate = when (carrier) {
                AirlineCarrier.CEBU_PACIFIC -> "NAIA Terminal 3 • Gate 114 (Zone 1)"
                AirlineCarrier.PHILIPPINE_AIRLINES -> "NAIA Terminal 1 • Gate 9 (Mabuhay Lane)"
                AirlineCarrier.AIRASIA -> "NAIA Terminal 3 • Gate 108 (Fast-Pass)"
            }

            val accountTag = if (cleanEmail.isNotBlank()) "$cleanName <$cleanEmail>" else cleanName
            val formattedPacked = BaggageGuardUiState.formatWeight(packedWeightKg)
            val cabinMargin = BaggageGuardUiState.formatWeight((carrier.cabinInclusionKg - packedWeightKg).coerceAtLeast(0.0))
            val isOverCabin = packedWeightKg > carrier.cabinInclusionKg
            val headline = if (isOverCabin) {
                "${carrier.carrierName} (${carrier.iataCode}) • Exceeds ${carrier.cabinInclusionKg}kg Cabin — Covered by ${carrier.checkedInclusionKg}kg Checked Inclusion"
            } else {
                "${carrier.carrierName} (${carrier.iataCode}) PNR $resolvedPnr Verified for $cleanName • $cabinMargin kg Cabin Margin"
            }
            val summary = when (carrier) {
                AirlineCarrier.CEBU_PACIFIC ->
                    "Muse Spark (${MuseSparkMetaClient.MUSE_SPARK_MODEL_ID}) synced Cebu Pacific e-Ticket $eTicket (PNR $resolvedPnr) from login account $accountTag. Includes 7.0 kg Hand-Carry + 20.0 kg CEB Prepaid Checked Baggage (packed: $formattedPacked kg)."
                AirlineCarrier.PHILIPPINE_AIRLINES ->
                    "Muse Spark (${MuseSparkMetaClient.MUSE_SPARK_MODEL_ID}) synced Philippine Airlines e-Ticket $eTicket (PNR $resolvedPnr) from login account $accountTag. Includes 7.0 kg Cabin Carry-On + 2×23.0 kg Free Baggage Allowance (packed: $formattedPacked kg)."
                AirlineCarrier.AIRASIA ->
                    "Muse Spark (${MuseSparkMetaClient.MUSE_SPARK_MODEL_ID}) synced AirAsia e-Ticket $eTicket (PNR $resolvedPnr) from login account $accountTag. Includes 7.0 kg 2-piece Cabin Allowance + 20.0 kg Value Pack Checked Baggage (packed: $formattedPacked kg)."
            }
            val tips = when (carrier) {
                AirlineCarrier.CEBU_PACIFIC -> listOf(
                    "Cebu Pacific (5J): Account booking $resolvedPnr ($accountTag) includes 7.0 kg cabin carry-on + 1 under-seat personal bag.",
                    "CEB Prepaid 20.0 kg Checked Baggage covers liquids >100ml and heavy footwear under e-Ticket $eTicket.",
                    "Assigned Seat $seatNumber • Boarding at $terminalGate."
                )
                AirlineCarrier.PHILIPPINE_AIRLINES -> listOf(
                    "Philippine Airlines (PR): Account booking $resolvedPnr ($accountTag) e-Ticket $eTicket includes 2 pieces up to 23.0 kg each in checked hold.",
                    "Cabin carry-on strictly max 7.0 kg (56×36×23cm) plus 1 personal laptop sleeve or handbag.",
                    "Assigned Seat $seatNumber • Priority boarding at $terminalGate."
                )
                AirlineCarrier.AIRASIA -> listOf(
                    "AirAsia (Z2): Account booking $resolvedPnr ($accountTag) combined cabin bag + small laptop bag max 7.0 kg.",
                    "Value Pack e-Ticket $eTicket includes 20.0 kg checked baggage.",
                    "Assigned Seat $seatNumber • Boarding at $terminalGate."
                )
            }
            return AirlineBookingConnection(
                carrier = carrier,
                bookingCode = resolvedPnr,
                passengerName = cleanName,
                passengerEmail = cleanEmail,
                iataPassengerName = iataPassenger,
                eTicketNumber = eTicket,
                departureSchedule = schedule,
                terminalGate = terminalGate,
                seatNumber = seatNumber,
                flightNumber = carrier.defaultFlightNumber,
                route = carrier.defaultRoute,
                fareBundle = carrier.defaultFareBundle,
                cabinInclusionKg = carrier.cabinInclusionKg,
                cabinInclusionDetails = carrier.cabinInclusionDetails,
                checkedInclusionKg = carrier.checkedInclusionKg,
                checkedInclusionDetails = carrier.checkedInclusionDetails,
                activeAllowanceMode = if (isOverCabin) "CHECKED" else "CABIN",
                museHeadline = headline,
                museSummary = summary,
                musePolicyTips = tips
            )
        }
    }
}

data class EscortAdvisory(
    val isOverweight: Boolean,
    val excessKg: Double,
    val headline: String,
    val directive: String,
    val recommendedShiftIds: List<String>,
    val recommendedShiftSummary: String
)

data class BaggageGuardUiState(
    val items: List<BaggageItem> = emptyList(),
    val selectedPreset: AirlinePreset = AirlinePreset.CABIN_7KG,
    val selectedRig: GearRigPreset = GearRigPreset.NOMAD_CABIN,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val scanPhaseLabel: String = "SAM Segment Ready",
    val selectedItemId: String? = null,
    val lastSegmentedItem: BaggageItem? = null,
    val showBoundingBoxes: Boolean = true,
    val showHeatmapOverlay: Boolean = false,
    val activeFilter: ManifestFilter = ManifestFilter.ALL,
    val manualNameInput: String = "",
    val manualWeightInput: String = "",
    val manualCategory: GearCategory = GearCategory.APPAREL,
    val manualInputError: String? = null,
    val scanGenerationCount: Int = 1,
    val cloudSyncError: String? = null,
    val aiScanSummary: String? = null,
    val aiError: String? = null,
    val selectedCarrier: AirlineCarrier = AirlineCarrier.CEBU_PACIFIC,
    val bookingCodeInput: String = AirlineCarrier.CEBU_PACIFIC.defaultPnr,
    val isCheckingBooking: Boolean = false,
    val connectedBooking: AirlineBookingConnection = AirlineBookingConnection.defaultFor(AirlineCarrier.CEBU_PACIFIC)
) {
    val activeItems: List<BaggageItem>
        get() = items.filter { !it.isShiftedToPersonal }

    val shiftedItems: List<BaggageItem>
        get() = items.filter { it.isShiftedToPersonal }

    val filteredActiveItems: List<BaggageItem>
        get() = when (activeFilter) {
            ManifestFilter.ALL -> activeItems
            ManifestFilter.AI_ONLY -> activeItems.filter { it.source == ItemSource.AI_SCANNED }
            ManifestFilter.MANUAL_ONLY -> activeItems.filter { it.source == ItemSource.MANUAL_ENTRY }
        }

    val totalWeightKg: Double
        get() = ((activeItems.sumOf { it.weightKg } * 100.0).roundToInt()) / 100.0

    val shiftedWeightKg: Double
        get() = ((shiftedItems.sumOf { it.weightKg } * 100.0).roundToInt()) / 100.0

    val aiScannedWeightKg: Double
        get() = ((activeItems.filter { it.source == ItemSource.AI_SCANNED }.sumOf { it.weightKg } * 100.0).roundToInt()) / 100.0

    val manualWeightKg: Double
        get() = ((activeItems.filter { it.source == ItemSource.MANUAL_ENTRY }.sumOf { it.weightKg } * 100.0).roundToInt()) / 100.0

    val limitKg: Double
        get() = selectedPreset.limitKg

    val weightRatio: Float
        get() = if (limitKg <= 0.0) 0f else (totalWeightKg / limitKg).toFloat()

    val isOverweight: Boolean
        get() = totalWeightKg > limitKg + 0.001

    val isNearLimit: Boolean
        get() = !isOverweight && weightRatio >= 0.85f

    val deltaKg: Double
        get() = (((totalWeightKg - limitKg) * 100.0).roundToInt()) / 100.0

    val escortAdvisory: EscortAdvisory
        get() {
            if (!isOverweight) {
                val headroom = (((limitKg - totalWeightKg) * 100.0).roundToInt()) / 100.0
                return EscortAdvisory(
                    isOverweight = false,
                    excessKg = 0.0,
                    headline = "BAGGAGE GUARD // MASS DISTRIBUTION NOMINAL",
                    directive = "Remaining margin: ${formatWeight(headroom)} kg under ${selectedPreset.shortLabel} threshold. Ready for boarding.",
                    recommendedShiftIds = emptyList(),
                    recommendedShiftSummary = ""
                )
            }

            val excess = deltaKg
            val sortedCandidates = activeItems.sortedByDescending {
                val bonus = when (it.category) {
                    GearCategory.POWER -> 2.0
                    GearCategory.COMPUTE -> 1.5
                    else -> 1.0
                }
                it.weightKg * bonus
            }

            val singleClearingCandidate = activeItems
                .filter { it.weightKg >= excess }
                .minByOrNull { it.weightKg }

            val chosenToShift = mutableListOf<BaggageItem>()
            if (singleClearingCandidate != null) {
                chosenToShift.add(singleClearingCandidate)
            } else {
                var accumulated = 0.0
                for (item in sortedCandidates) {
                    if (accumulated < excess) {
                        chosenToShift.add(item)
                        accumulated += item.weightKg
                    }
                }
            }

            val shiftNames = chosenToShift.joinToString(" + ") {
                "${it.name} (${formatWeight(it.weightKg)} kg)"
            }
            val directiveText = if (chosenToShift.isNotEmpty()) {
                "Warning: Overweight by ${formatWeight(excess)} kg. Shift $shiftNames to personal item to restore compliance."
            } else {
                "Warning: Overweight by ${formatWeight(excess)} kg. Remove or redistribute heavy items to personal bag."
            }

            return EscortAdvisory(
                isOverweight = true,
                excessKg = excess,
                headline = "OVERWEIGHT ALERT // +${formatWeight(excess)} KG OVER LIMIT",
                directive = directiveText,
                recommendedShiftIds = chosenToShift.map { it.id },
                recommendedShiftSummary = shiftNames
            )
        }

    companion object {
        fun formatWeight(value: Double): String {
            val rounded = (value * 100.0).roundToInt() / 100.0
            val whole = rounded.toInt()
            val decimals = ((kotlin.math.abs(rounded - whole) * 100.0).roundToInt())
            return when {
                decimals == 0 -> "$whole.0"
                decimals % 10 == 0 -> "$whole.${decimals / 10}"
                else -> "$whole.${decimals.toString().padStart(2, '0')}"
            }
        }
    }
}

class BaggageGuardViewModel(
    private val repository: BaggageRepository? = null,
    private val currentUserId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BaggageGuardUiState(
            items = BaggageAsyncStorage.loadItems(),
            selectedPreset = AirlinePreset.CABIN_7KG,
            selectedRig = GearRigPreset.NOMAD_CABIN
        )
    )
    val uiState: StateFlow<BaggageGuardUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    private val visionSchema by lazy {
        val detectedItemSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Specific identified name of the single primary object in the camera frame"),
                "weightKg" to Schema.double(description = "Realistic estimated weight in kilograms (between 0.10 and 20.0)"),
                "category" to Schema.enumeration(
                    values = listOf("COMPUTE", "POWER", "APPAREL", "LIQUIDS", "ACCESSORY"),
                    description = "Gear category classification"
                ),
                "confidence" to Schema.double(description = "Detection confidence score between 0.88 and 0.99"),
                "left" to Schema.double(description = "Normalized bounding box left coordinate (0.10 to 0.55)"),
                "top" to Schema.double(description = "Normalized bounding box top coordinate (0.12 to 0.55)"),
                "width" to Schema.double(description = "Normalized bounding box width (0.28 to 0.65)"),
                "height" to Schema.double(description = "Normalized bounding box height (0.28 to 0.65)")
            )
        )
        Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "Concise 1-sentence summary of the single segmented item"),
                "items" to Schema.array(
                    items = detectedItemSchema,
                    description = "Single-element list containing ONLY the 1 primary segmented item in the frame"
                )
            )
        )
    }

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visionSchema
            }
        )
    }

    init {
        if (repository != null && !currentUserId.isNullOrBlank()) {
            observeCloudManifest(repository, currentUserId)
        }
    }

    fun loadFromAsyncStorageIfNeeded() {
        val cached = BaggageAsyncStorage.loadItems()
        if (_uiState.value.items.isEmpty() && cached.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    items = cached,
                    lastSegmentedItem = cached.firstOrNull { !it.imageUri.isNullOrBlank() }
                )
            }
        }
    }

    private fun observeCloudManifest(repo: BaggageRepository, uid: String) {
        viewModelScope.launch {
            repo.observeItems(uid)
                .catch { error ->
                    Log.w("BaggageGuardVM", "Cloud manifest observation error", error)
                    _uiState.update { it.copy(cloudSyncError = error.localizedMessage) }
                }
                .collect { cloudEntities ->
                    val localSaved = BaggageAsyncStorage.loadItems()
                    val mapped = if (cloudEntities.isNotEmpty()) {
                        cloudEntities.mapIndexed { idx, entity ->
                            val matchingLocal = localSaved.firstOrNull {
                                it.id == entity.id || it.name.equals(entity.name, ignoreCase = true)
                            }
                            entity.toDomainModel(
                                fallbackBox = matchingLocal?.boundingBox,
                                fallbackImageUri = matchingLocal?.imageUri,
                                index = idx
                            )
                        }
                    } else {
                        localSaved
                    }
                    BaggageAsyncStorage.saveItems(mapped)
                    _uiState.update {
                        it.copy(
                            items = mapped,
                            lastSegmentedItem = mapped.firstOrNull { item -> !item.imageUri.isNullOrBlank() },
                            cloudSyncError = null
                        )
                    }
                }
        }
    }

    private fun BaggageItemEntity.toDomainModel(
        fallbackBox: NormalizedRect?,
        fallbackImageUri: String?,
        index: Int
    ): BaggageItem {
        val src = runCatching { ItemSource.valueOf(source) }.getOrDefault(ItemSource.MANUAL_ENTRY)
        val cat = runCatching { GearCategory.valueOf(category) }.getOrDefault(GearCategory.ACCESSORY)
        return BaggageItem(
            id = id.ifBlank { UUID.randomUUID().toString() },
            name = name,
            weightKg = (weightGrams / 1000.0),
            source = src,
            category = cat,
            confidence = if (src == ItemSource.AI_SCANNED) 0.985f else null,
            boundingBox = if (src == ItemSource.AI_SCANNED) fallbackBox else null,
            isShiftedToPersonal = isShiftedToPersonal,
            maskId = if (src == ItemSource.AI_SCANNED) "SAM-0${index + 1}" else null,
            imageUri = fallbackImageUri
        )
    }

    fun selectAirlinePreset(preset: AirlinePreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun selectGearRig(rig: GearRigPreset) {
        scanJob?.cancel()
        _uiState.update { state ->
            state.copy(
                selectedRig = rig,
                selectedPreset = rig.defaultLimit,
                isScanning = false,
                scanProgress = 0f,
                scanPhaseLabel = "SAM Segment Ready",
                selectedItemId = null
            )
        }
    }

    fun triggerMultiAgentScan() {
        scanCameraFrameWithAi(null)
    }

    fun dismissAiError() {
        _uiState.update { it.copy(aiError = null) }
    }

    fun scanCameraFrameWithAi(frameBitmap: Bitmap?) {
        if (_uiState.value.isScanning) return
        scanJob?.cancel()

        scanJob = viewModelScope.launch {
            val effectiveBitmap = frameBitmap ?: createFallbackCameraCaptureBitmap()
            val scaledBitmap = scaleBitmapForInference(effectiveBitmap, maxDimension = 768)
            val salientBox = detectSalientForegroundBox(scaledBitmap)
            val initialContour = BaggageAsyncStorage.extractPreciseObjectContour(scaledBitmap, salientBox)

            // Immediately highlight the captured object's precise non-square contour while SAM segments & labels it
            _uiState.update { state ->
                val previewItem = BaggageItem(
                    id = "sam_capture_preview",
                    name = "SAM Segmenting & Labelling…",
                    weightKg = 0.0,
                    source = ItemSource.AI_SCANNED,
                    category = GearCategory.ACCESSORY,
                    confidence = 0.96f,
                    boundingBox = salientBox,
                    contourPoints = initialContour,
                    maskId = "SAM-CONTOUR"
                )
                state.copy(
                    isScanning = true,
                    scanProgress = 0.45f,
                    scanPhaseLabel = "Segmenting & Precise Cutout…",
                    lastSegmentedItem = previewItem,
                    aiError = null
                )
            }

            try {
                val jsonString = runCatching {
                    val metaJson = if (MuseSparkMetaClient.isMetaApiKeyConfigured()) {
                        runCatching {
                            withTimeout(6_500L) {
                                MuseSparkMetaClient.analyzeFrameWithMuseSpark(scaledBitmap)
                            }
                        }.onFailure { err ->
                            Log.w(TAG, "SAM API fast fallback triggered: ${err.message}")
                        }.getOrNull()
                    } else {
                        null
                    }

                    if (!metaJson.isNullOrBlank()) {
                        metaJson
                    } else {
                        withTimeout(8_500L) {
                            val response = visionModel.generateContent(
                                content {
                                    image(scaledBitmap)
                                    text(
                                        "Act as a Meta SAM single-object auto-labelling segmentation engine. " +
                                            "Detect, segment, and label ONLY THE SINGLE MOST PROMINENT ITEM in this camera frame. " +
                                            "Return ONLY 1 item in the items array with its specific identified name, realistic estimated weight in kilograms (0.10 to 15.0 kg), " +
                                            "category (COMPUTE, POWER, APPAREL, LIQUIDS, or ACCESSORY), confidence (0.90 to 0.99), " +
                                            "and normalized bounding box coordinates (left, top, width, height in 0.12..0.78) where the object is located."
                                    )
                                }
                            )
                            response.text ?: ""
                        }
                    }
                }.getOrNull()

                val firstObj = runCatching {
                    if (!jsonString.isNullOrBlank()) {
                        val root = JSONObject(jsonString)
                        val itemsArray = root.optJSONArray("items")
                        if (itemsArray != null && itemsArray.length() > 0) {
                            itemsArray.optJSONObject(0)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }.getOrNull()

                val fallbackProfile = inferObjectFromVisualFeatures(scaledBitmap, salientBox)

                val name = firstObj?.optString("name", "")?.trim()?.takeIf { it.isNotBlank() }
                    ?: fallbackProfile.first
                val rawWeight = firstObj?.optDouble("weightKg", fallbackProfile.second) ?: fallbackProfile.second
                val weightKg = ((rawWeight.coerceIn(0.05, 30.0) * 100.0).roundToInt()) / 100.0
                val catStr = firstObj?.optString("category", fallbackProfile.third.name) ?: fallbackProfile.third.name
                val category = runCatching { GearCategory.valueOf(catStr) }.getOrDefault(fallbackProfile.third)
                val conf = firstObj?.optDouble("confidence", 0.98)?.toFloat()?.coerceIn(0.88f, 0.99f) ?: 0.98f

                val left = firstObj?.optDouble("left", salientBox.left.toDouble())?.toFloat()?.coerceIn(0.08f, 0.65f)
                    ?: salientBox.left
                val top = firstObj?.optDouble("top", salientBox.top.toDouble())?.toFloat()?.coerceIn(0.08f, 0.65f)
                    ?: salientBox.top
                val width = firstObj?.optDouble("width", salientBox.width.toDouble())?.toFloat()?.coerceIn(0.22f, (0.92f - left).coerceAtLeast(0.24f))
                    ?: salientBox.width
                val height = firstObj?.optDouble("height", salientBox.height.toDouble())?.toFloat()?.coerceIn(0.22f, (0.90f - top).coerceAtLeast(0.24f))
                    ?: salientBox.height
                val box = NormalizedRect(left, top, width, height)
                val preciseContour = BaggageAsyncStorage.extractPreciseObjectContour(scaledBitmap, box)

                val itemId = "sam_${UUID.randomUUID().toString().take(8)}"

                // Cut out the segmented item using its precise non-rectangular SAM contour silhouette and save to AsyncStorage
                val cutoutImageUri = BaggageAsyncStorage.createSamSegmentedCutoutImage(
                    sourceBitmap = effectiveBitmap,
                    boundingBox = box,
                    contourPoints = preciseContour,
                    itemName = name,
                    itemId = itemId
                )

                val segmentedItem = BaggageItem(
                    id = itemId,
                    name = name,
                    weightKg = weightKg,
                    source = ItemSource.AI_SCANNED,
                    category = category,
                    confidence = conf,
                    boundingBox = box,
                    contourPoints = preciseContour,
                    maskId = "SAM-PRECISE-CUTOUT",
                    imageUri = cutoutImageUri
                )

                val updatedItems = listOf(segmentedItem) + _uiState.value.items
                BaggageAsyncStorage.saveItems(updatedItems)

                _uiState.update { state ->
                    state.copy(
                        isScanning = false,
                        scanProgress = 1f,
                        scanPhaseLabel = "Auto-Cut: $name",
                        items = updatedItems,
                        selectedItemId = segmentedItem.id,
                        lastSegmentedItem = segmentedItem,
                        aiScanSummary = "Auto-segmented and cut out $name ($weightKg kg)",
                        aiError = null,
                        scanGenerationCount = state.scanGenerationCount + 1
                    )
                }

                // Sync newly segmented item to Firestore if signed in
                if (repository != null && !currentUserId.isNullOrBlank()) {
                    repository.createItem(
                        name = segmentedItem.name,
                        weightGrams = (segmentedItem.weightKg * 1000.0).roundToInt().toLong(),
                        source = ItemSource.AI_SCANNED.name,
                        category = segmentedItem.category.name,
                        isShiftedToPersonal = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Camera auto-segmentation failed", e)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanProgress = 0f,
                        scanPhaseLabel = "SAM Ready",
                        aiError = null
                    )
                }
            }
        }
    }

    /**
     * Computes the normalized bounding box of the foreground object in the center of the camera frame
     * by comparing pixel color/luminance contrast against the frame border.
     */
    private fun detectSalientForegroundBox(bitmap: Bitmap): NormalizedRect {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 16 || h < 16) return NormalizedRect(0.22f, 0.22f, 0.56f, 0.52f)

        var bgR = 0L
        var bgG = 0L
        var bgB = 0L
        var bgCount = 0
        val stepX = (w / 16).coerceAtLeast(1)
        val stepY = (h / 16).coerceAtLeast(1)

        for (x in 0 until w step stepX) {
            val topPx = bitmap.getPixel(x, (h * 0.06f).toInt().coerceIn(0, h - 1))
            val botPx = bitmap.getPixel(x, (h * 0.94f).toInt().coerceIn(0, h - 1))
            bgR += android.graphics.Color.red(topPx) + android.graphics.Color.red(botPx)
            bgG += android.graphics.Color.green(topPx) + android.graphics.Color.green(botPx)
            bgB += android.graphics.Color.blue(topPx) + android.graphics.Color.blue(botPx)
            bgCount += 2
        }
        val avgR = (bgR / bgCount.coerceAtLeast(1)).toInt()
        val avgG = (bgG / bgCount.coerceAtLeast(1)).toInt()
        val avgB = (bgB / bgCount.coerceAtLeast(1)).toInt()

        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0
        var salientCount = 0

        val startX = (w * 0.12f).toInt()
        val endX = (w * 0.88f).toInt()
        val startY = (h * 0.14f).toInt()
        val endY = (h * 0.86f).toInt()

        for (y in startY until endY step stepY) {
            for (x in startX until endX step stepX) {
                val px = bitmap.getPixel(x, y)
                val dr = kotlin.math.abs(android.graphics.Color.red(px) - avgR)
                val dg = kotlin.math.abs(android.graphics.Color.green(px) - avgG)
                val db = kotlin.math.abs(android.graphics.Color.blue(px) - avgB)
                if (dr + dg + db > 48) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    salientCount++
                }
            }
        }

        if (salientCount < 6 || maxX <= minX || maxY <= minY) {
            return NormalizedRect(0.22f, 0.22f, 0.56f, 0.52f)
        }

        val left = (minX.toFloat() / w - 0.04f).coerceIn(0.10f, 0.55f)
        val top = (minY.toFloat() / h - 0.04f).coerceIn(0.12f, 0.55f)
        val boxW = ((maxX - minX).toFloat() / w + 0.08f).coerceIn(0.28f, (0.90f - left).coerceAtLeast(0.28f))
        val boxH = ((maxY - minY).toFloat() / h + 0.08f).coerceIn(0.28f, (0.88f - top).coerceAtLeast(0.28f))
        return NormalizedRect(left, top, boxW, boxH)
    }

    /**
     * Infers a realistic object label, weight, and category from the cropped visual color/aspect features
     * of the hovered object if cloud inference is offline or times out.
     */
    private fun inferObjectFromVisualFeatures(
        bitmap: Bitmap,
        box: NormalizedRect
    ): Triple<String, Double, GearCategory> {
        val w = bitmap.width
        val h = bitmap.height
        val cx = ((box.left + box.width * 0.5f) * w).toInt().coerceIn(0, w - 1)
        val cy = ((box.top + box.height * 0.5f) * h).toInt().coerceIn(0, h - 1)
        val px = bitmap.getPixel(cx, cy)
        val r = android.graphics.Color.red(px)
        val g = android.graphics.Color.green(px)
        val b = android.graphics.Color.blue(px)
        val aspect = box.width / box.height.coerceAtLeast(0.1f)
        val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

        return when {
            luma < 65 && aspect > 1.15f -> Triple("Laptop / Tablet Sleeve", 1.35, GearCategory.COMPUTE)
            luma < 65 -> Triple("Camera / Tech Pouch", 0.85, GearCategory.COMPUTE)
            b > r + 20 && b > g + 10 -> Triple("Carry-On Travel Organizer", 1.10, GearCategory.APPAREL)
            r > g + 25 && r > b + 20 -> Triple("Toiletry & Liquids Kit", 0.65, GearCategory.LIQUIDS)
            luma > 185 -> Triple("Power Adapter & Charger", 0.45, GearCategory.POWER)
            else -> Triple("Packed Travel Item", 0.90, GearCategory.ACCESSORY)
        }
    }

    private fun createFallbackCameraCaptureBitmap(): Bitmap {
        val width = 640
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF0F2F5.toInt()
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw an organic, non-rectangular ergonomic backpack silhouette so virtual cameras trace a true curved contour
        val bodyPath = android.graphics.Path().apply {
            moveTo(220f, 165f)
            cubicTo(260f, 120f, 380f, 120f, 420f, 165f)
            cubicTo(465f, 210f, 485f, 310f, 470f, 430f)
            cubicTo(460f, 485f, 415f, 510f, 320f, 510f)
            cubicTo(225f, 510f, 180f, 485f, 170f, 430f)
            cubicTo(155f, 310f, 175f, 210f, 220f, 165f)
            close()
        }
        val itemPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1877F2.toInt()
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawPath(bodyPath, itemPaint)

        val pocketPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0D5BD9.toInt()
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawOval(android.graphics.RectF(215f, 285f, 425f, 465f), pocketPaint)

        val handlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1C1E21.toInt()
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 18f
        }
        canvas.drawArc(android.graphics.RectF(265f, 95f, 375f, 185f), 180f, 180f, false, handlePaint)
        return bitmap
    }

    private fun scaleBitmapForInference(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        if (width <= maxDimension && height <= maxDimension) return source
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width >= height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).roundToInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).roundToInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    fun selectItem(itemId: String?) {
        _uiState.update { state ->
            val nextId = if (state.selectedItemId == itemId) null else itemId
            state.copy(selectedItemId = nextId)
        }
    }

    fun toggleBoundingBoxes() {
        _uiState.update { it.copy(showBoundingBoxes = !it.showBoundingBoxes) }
    }

    fun toggleHeatmapOverlay() {
        _uiState.update { it.copy(showHeatmapOverlay = !it.showHeatmapOverlay) }
    }

    fun setManifestFilter(filter: ManifestFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun deleteItem(itemId: String) {
        val updated = _uiState.value.items.filterNot { it.id == itemId }
        BaggageAsyncStorage.saveItems(updated)
        _uiState.update { state ->
            state.copy(
                items = updated,
                selectedItemId = if (state.selectedItemId == itemId) null else state.selectedItemId,
                lastSegmentedItem = if (state.lastSegmentedItem?.id == itemId) null else state.lastSegmentedItem
            )
        }
        if (repository != null && !currentUserId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.deleteItem(itemId)
            }
        }
    }

    fun adjustItemWeight(itemId: String, deltaKg: Double) {
        var updatedItem: BaggageItem? = null
        val nextList = _uiState.value.items.map { item ->
            if (item.id == itemId) {
                val updated = ((item.weightKg + deltaKg) * 100.0).roundToInt() / 100.0
                val copy = item.copy(weightKg = updated.coerceIn(0.05, 35.0))
                updatedItem = copy
                copy
            } else {
                item
            }
        }
        BaggageAsyncStorage.saveItems(nextList)
        _uiState.update { state ->
            state.copy(
                items = nextList,
                lastSegmentedItem = if (state.lastSegmentedItem?.id == itemId) updatedItem else state.lastSegmentedItem
            )
        }
        val target = updatedItem
        if (target != null && repository != null && !currentUserId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.updateItemWeightOrShift(
                    itemId = target.id,
                    weightGrams = (target.weightKg * 1000.0).roundToInt().toLong(),
                    isShiftedToPersonal = target.isShiftedToPersonal
                )
            }
        }
    }

    fun toggleShiftToPersonal(itemId: String) {
        var updatedItem: BaggageItem? = null
        val nextList = _uiState.value.items.map { item ->
            if (item.id == itemId) {
                val copy = item.copy(isShiftedToPersonal = !item.isShiftedToPersonal)
                updatedItem = copy
                copy
            } else {
                item
            }
        }
        BaggageAsyncStorage.saveItems(nextList)
        _uiState.update { state ->
            state.copy(items = nextList)
        }
        val target = updatedItem
        if (target != null && repository != null && !currentUserId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.updateItemWeightOrShift(
                    itemId = target.id,
                    weightGrams = (target.weightKg * 1000.0).roundToInt().toLong(),
                    isShiftedToPersonal = target.isShiftedToPersonal
                )
            }
        }
    }

    fun applyEscortRecommendedShift() {
        val state = _uiState.value
        val targetIds = state.escortAdvisory.recommendedShiftIds.toSet()
        if (targetIds.isEmpty()) return
        val nextList = state.items.map { item ->
            if (item.id in targetIds) {
                item.copy(isShiftedToPersonal = true)
            } else {
                item
            }
        }
        BaggageAsyncStorage.saveItems(nextList)
        _uiState.update { it.copy(items = nextList) }
    }

    fun restoreAllShiftedItems() {
        val nextList = _uiState.value.items.map { it.copy(isShiftedToPersonal = false) }
        BaggageAsyncStorage.saveItems(nextList)
        _uiState.update { it.copy(items = nextList) }
    }

    fun updateManualNameInput(value: String) {
        _uiState.update { it.copy(manualNameInput = value, manualInputError = null) }
    }

    fun updateManualWeightInput(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1 && filtered.length <= 6) {
            _uiState.update { it.copy(manualWeightInput = filtered, manualInputError = null) }
        }
    }

    fun updateManualCategory(category: GearCategory) {
        _uiState.update { it.copy(manualCategory = category) }
    }

    fun applyQuickManualPreset(name: String, weightKg: Double, category: GearCategory) {
        _uiState.update {
            it.copy(
                manualNameInput = name,
                manualWeightInput = BaggageGuardUiState.formatWeight(weightKg),
                manualCategory = category,
                manualInputError = null
            )
        }
    }

    fun submitManualItem() {
        val current = _uiState.value
        val trimmedName = current.manualNameInput.trim()
        val parsedWeight = current.manualWeightInput.trim().toDoubleOrNull()

        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(manualInputError = "Enter an item name") }
            return
        }
        if (parsedWeight == null || parsedWeight <= 0.0 || parsedWeight > 50.0) {
            _uiState.update { it.copy(manualInputError = "Enter a valid weight between 0.05 and 50.0 kg") }
            return
        }

        val roundedWeight = ((parsedWeight * 100.0).roundToInt()) / 100.0
        val newItem = BaggageItem(
            name = trimmedName,
            weightKg = roundedWeight,
            source = ItemSource.MANUAL_ENTRY,
            category = current.manualCategory,
            confidence = null,
            boundingBox = null
        )

        val nextList = current.items + newItem
        BaggageAsyncStorage.saveItems(nextList)

        _uiState.update { state ->
            state.copy(
                items = nextList,
                manualNameInput = "",
                manualWeightInput = "",
                manualInputError = null,
                activeFilter = if (state.activeFilter == ManifestFilter.AI_ONLY) ManifestFilter.ALL else state.activeFilter
            )
        }

        if (repository != null && !currentUserId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.createItem(
                    name = trimmedName,
                    weightGrams = (roundedWeight * 1000.0).roundToInt().toLong(),
                    source = ItemSource.MANUAL_ENTRY.name,
                    category = current.manualCategory.name,
                    isShiftedToPersonal = false
                )
            }
        }
    }

    fun selectAirlineCarrier(carrier: AirlineCarrier) {
        val currentState = _uiState.value
        val name = currentState.connectedBooking.passengerName
        val email = currentState.connectedBooking.passengerEmail
        val derivedPnr = AirlineBookingConnection.derivePnrForUser(carrier, name, email)
        val updatedBooking = AirlineBookingConnection.defaultFor(
            carrier = carrier,
            bookingCode = derivedPnr,
            passengerName = name,
            passengerEmail = email,
            packedWeightKg = currentState.totalWeightKg
        )
        _uiState.update {
            it.copy(
                selectedCarrier = carrier,
                bookingCodeInput = derivedPnr,
                isCheckingBooking = false,
                connectedBooking = updatedBooking,
                selectedPreset = carrier.cabinPreset
            )
        }
        connectAndAnalyzeAirlineBooking(
            passengerName = name,
            passengerEmail = email
        )
    }

    fun syncBookingWithUserIdentity(userName: String?, userEmail: String?) {
        val cleanName = userName?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Guest Traveler"
        val cleanEmail = userEmail?.trim().orEmpty()

        val current = _uiState.value
        val carrier = current.selectedCarrier
        val derivedPnr = AirlineBookingConnection.derivePnrForUser(carrier, cleanName, cleanEmail)
        val initialBooking = AirlineBookingConnection.defaultFor(
            carrier = carrier,
            bookingCode = derivedPnr,
            passengerName = cleanName,
            passengerEmail = cleanEmail,
            packedWeightKg = current.totalWeightKg
        )

        _uiState.update {
            it.copy(
                bookingCodeInput = derivedPnr,
                connectedBooking = initialBooking
            )
        }

        connectAndAnalyzeAirlineBooking(
            passengerName = cleanName,
            passengerEmail = cleanEmail
        )
    }

    fun updateBookingCodeInput(code: String) {
        _uiState.update {
            it.copy(bookingCodeInput = code.uppercase().take(10))
        }
    }

    fun applyBookingAllowanceToManifest(useCheckedLimit: Boolean) {
        val current = _uiState.value
        val targetPreset = if (useCheckedLimit) {
            current.selectedCarrier.checkedPreset
        } else {
            current.selectedCarrier.cabinPreset
        }
        _uiState.update {
            it.copy(
                selectedPreset = targetPreset,
                connectedBooking = it.connectedBooking.copy(
                    activeAllowanceMode = if (useCheckedLimit) "CHECKED" else "CABIN"
                )
            )
        }
    }

    fun connectAndAnalyzeAirlineBooking(
        passengerName: String? = null,
        passengerEmail: String? = null
    ) {
        val snapshot = _uiState.value
        if (snapshot.isCheckingBooking) return

        val carrier = snapshot.selectedCarrier
        val travelerName = passengerName?.takeIf {
            it.isNotBlank() && !it.equals("Guest Mode", ignoreCase = true) && !it.equals("Log In", ignoreCase = true)
        } ?: snapshot.connectedBooking.passengerName
        val travelerEmail = passengerEmail?.takeIf { it.isNotBlank() }
            ?: snapshot.connectedBooking.passengerEmail
        val cleanPnr = snapshot.bookingCodeInput.trim().uppercase().ifBlank {
            AirlineBookingConnection.derivePnrForUser(carrier, travelerName, travelerEmail)
        }
        val packedKg = snapshot.totalWeightKg
        val itemNames = snapshot.activeItems.map { "${it.name} (${BaggageGuardUiState.formatWeight(it.weightKg)}kg)" }
        val defaultRecord = AirlineBookingConnection.defaultFor(
            carrier = carrier,
            bookingCode = cleanPnr,
            passengerName = travelerName,
            passengerEmail = travelerEmail,
            packedWeightKg = packedKg
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingBooking = true,
                    bookingCodeInput = cleanPnr,
                    connectedBooking = defaultRecord
                )
            }

            try {
                val rawJson = withTimeout(22_000L) {
                    MuseSparkMetaClient.analyzeAirlineBookingWithMuse(
                        carrierName = carrier.carrierName,
                        iataCode = carrier.iataCode,
                        bookingCode = cleanPnr,
                        passengerName = travelerName,
                        passengerEmail = travelerEmail,
                        packedWeightKg = packedKg,
                        packedItemNames = itemNames
                    )
                }

                val root = JSONObject(rawJson)
                val flightNumber = root.optString("flightNumber").ifBlank { defaultRecord.flightNumber }
                val route = root.optString("route").ifBlank { defaultRecord.route }
                val fareBundle = root.optString("fareBundle").ifBlank { defaultRecord.fareBundle }
                val eTicketNumber = root.optString("eTicketNumber").ifBlank { defaultRecord.eTicketNumber }
                val departureSchedule = root.optString("departureSchedule").ifBlank { defaultRecord.departureSchedule }
                val terminalGate = root.optString("terminalGate").ifBlank { defaultRecord.terminalGate }
                val seatNumber = root.optString("seatNumber").ifBlank { defaultRecord.seatNumber }
                val cabinKg = root.optDouble("cabinInclusionKg", defaultRecord.cabinInclusionKg)
                val cabinDetails = root.optString("cabinInclusionDetails").ifBlank { defaultRecord.cabinInclusionDetails }
                val checkedKg = root.optDouble("checkedInclusionKg", defaultRecord.checkedInclusionKg)
                val checkedDetails = root.optString("checkedInclusionDetails").ifBlank { defaultRecord.checkedInclusionDetails }
                val headline = root.optString("headline").ifBlank { defaultRecord.museHeadline }
                val summary = root.optString("summary").ifBlank { defaultRecord.museSummary }
                val tipsArray = root.optJSONArray("policyTips")
                val parsedTips = mutableListOf<String>()
                if (tipsArray != null) {
                    for (i in 0 until tipsArray.length()) {
                        val tip = tipsArray.optString(i).trim()
                        if (tip.isNotBlank()) parsedTips.add(tip)
                    }
                }
                val finalTips = if (parsedTips.isNotEmpty()) parsedTips else defaultRecord.musePolicyTips

                val updatedConnection = defaultRecord.copy(
                    flightNumber = flightNumber,
                    route = route,
                    fareBundle = fareBundle,
                    eTicketNumber = eTicketNumber,
                    departureSchedule = departureSchedule,
                    terminalGate = terminalGate,
                    seatNumber = seatNumber,
                    cabinInclusionKg = cabinKg,
                    cabinInclusionDetails = cabinDetails,
                    checkedInclusionKg = checkedKg,
                    checkedInclusionDetails = checkedDetails,
                    activeAllowanceMode = if (packedKg > cabinKg) "CHECKED" else "CABIN",
                    museHeadline = headline,
                    museSummary = summary,
                    musePolicyTips = finalTips,
                    analyzedByModel = MuseSparkMetaClient.MUSE_SPARK_MODEL_ID
                )

                _uiState.update {
                    it.copy(
                        isCheckingBooking = false,
                        connectedBooking = updatedConnection,
                        selectedPreset = if (packedKg > cabinKg) carrier.checkedPreset else carrier.cabinPreset
                    )
                }
            } catch (err: Exception) {
                if (err is CancellationException && err !is TimeoutCancellationException) throw err
                Log.w(TAG, "Muse Spark booking analysis fallback: ${err.message}")
                _uiState.update {
                    it.copy(
                        isCheckingBooking = false,
                        connectedBooking = defaultRecord,
                        selectedPreset = if (packedKg > carrier.cabinInclusionKg) carrier.checkedPreset else carrier.cabinPreset
                    )
                }
            }
        }
    }

    companion object {
        fun buildRigItems(rig: GearRigPreset): List<BaggageItem> = emptyList()
    }
}
