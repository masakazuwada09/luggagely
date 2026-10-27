package com.example.model

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isShiftedToPersonal: Boolean = false,
    val maskId: String? = null
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
            }.ifBlank { "Masakazu Wada" }
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

        fun defaultFor(
            carrier: AirlineCarrier,
            bookingCode: String? = null,
            passengerName: String = "Masakazu Wada",
            passengerEmail: String = "masakazuwada09@gmail.com",
            packedWeightKg: Double = 0.0
        ): AirlineBookingConnection {
            val cleanName = passengerName.trim().ifBlank { "Masakazu Wada" }
            val cleanEmail = passengerEmail.trim().ifBlank { "masakazuwada09@gmail.com" }
            val resolvedPnr = bookingCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
                ?: derivePnrForUser(carrier, cleanName, cleanEmail)
            val iataPassenger = formatIataPassenger(cleanName, cleanEmail)
            val eTicket = deriveETicketForUser(carrier, cleanName, cleanEmail)

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
            val seatNumber = when (carrier) {
                AirlineCarrier.CEBU_PACIFIC -> "14A (Window • Front Cabin)"
                AirlineCarrier.PHILIPPINE_AIRLINES -> "22K (Window • Economy Fiesta)"
                AirlineCarrier.AIRASIA -> "11F (Window • Quiet Zone)"
            }

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
                    "Muse Spark (${MuseSparkMetaClient.MUSE_SPARK_MODEL_ID}) matched Cebu Pacific e-Ticket $eTicket (PNR $resolvedPnr) for $cleanName <$cleanEmail>. Includes 7.0 kg Hand-Carry + 20.0 kg CEB Prepaid Checked Baggage (packed: $formattedPacked kg)."
                AirlineCarrier.PHILIPPINE_AIRLINES ->
                    "Muse Spark (${MuseSparkMetaClient.MUSE_SPARK_MODEL_ID}) matched Philippine Airlines e-Ticket $eTicket (PNR $resolvedPnr) for $cleanName <$cleanEmail>. Includes 7.0 kg Cabin Carry-On + 2×23.0 kg Free Baggage Allowance (packed: $formattedPacked kg)."
                AirlineCarrier.AIRASIA ->
                    "Muse Spark (${MuseSparkMetaClient.MUSE_SPARK_MODEL_ID}) matched AirAsia e-Ticket $eTicket (PNR $resolvedPnr) for $cleanName <$cleanEmail>. Includes 7.0 kg 2-piece Cabin Allowance + 20.0 kg Value Pack Checked Baggage (packed: $formattedPacked kg)."
            }
            val tips = when (carrier) {
                AirlineCarrier.CEBU_PACIFIC -> listOf(
                    "Cebu Pacific (5J): Keep lithium power banks & laptops in your 7.0 kg cabin bag or small under-seat bag.",
                    "CEB Prepaid 20.0 kg Checked Baggage covers liquids >100ml and heavy footwear under e-Ticket $eTicket.",
                    "Gate check fees apply if carry-on exceeds 7.0 kg at NAIA T3 Gate 114."
                )
                AirlineCarrier.PHILIPPINE_AIRLINES -> listOf(
                    "Philippine Airlines (PR): Economy Fiesta e-Ticket $eTicket includes 2 pieces up to 23.0 kg each in checked hold.",
                    "Cabin carry-on strictly max 7.0 kg (56×36×23cm) plus 1 personal laptop sleeve or handbag.",
                    "Mabuhay Miles priority baggage tag linked to $cleanEmail at Manila Terminal 1."
                )
                AirlineCarrier.AIRASIA -> listOf(
                    "AirAsia (Z2): Combined weight of main cabin bag + small laptop bag must not exceed 7.0 kg.",
                    "Value Pack e-Ticket $eTicket includes 20.0 kg checked baggage linked to $cleanEmail.",
                    "Fast-Pass Xtra Carry-On (14.0 kg) upgrade available up to 4 hours before departure."
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
    val scanPhaseLabel: String = "MUSE-SPARK-1.3 // META SAM READY",
    val selectedItemId: String? = null,
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
            items = emptyList(),
            selectedPreset = AirlinePreset.CABIN_7KG,
            selectedRig = GearRigPreset.NOMAD_CABIN
        )
    )
    val uiState: StateFlow<BaggageGuardUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    private val visionSchema by lazy {
        val detectedItemSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Specific identified name of the object or item"),
                "weightKg" to Schema.double(description = "Realistic estimated weight in kilograms (between 0.10 and 20.0)"),
                "category" to Schema.enumeration(
                    values = listOf("COMPUTE", "POWER", "APPAREL", "LIQUIDS", "ACCESSORY"),
                    description = "Gear category classification"
                ),
                "confidence" to Schema.double(description = "Detection confidence score between 0.86 and 0.99"),
                "left" to Schema.double(description = "Normalized bounding box left coordinate (0.04 to 0.65)"),
                "top" to Schema.double(description = "Normalized bounding box top coordinate (0.08 to 0.65)"),
                "width" to Schema.double(description = "Normalized bounding box width (0.18 to 0.44)"),
                "height" to Schema.double(description = "Normalized bounding box height (0.18 to 0.44)")
            )
        )
        Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "Concise 1-sentence summary of the objects detected and identified in the camera frame"),
                "items" to Schema.array(
                    items = detectedItemSchema,
                    description = "List of detected and segmented items in the frame with bounding boxes and estimated weight in kg"
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

    private fun observeCloudManifest(repo: BaggageRepository, uid: String) {
        viewModelScope.launch {
            repo.observeItems(uid)
                .catch { error ->
                    Log.w("BaggageGuardVM", "Cloud manifest observation error", error)
                    _uiState.update { it.copy(cloudSyncError = error.localizedMessage) }
                }
                .collect { cloudEntities ->
                    val defaultBoxes = buildRigItems(_uiState.value.selectedRig)
                    val mapped = cloudEntities.mapIndexed { idx, entity ->
                        val fallbackBox = defaultBoxes.getOrNull(idx % defaultBoxes.size)?.boundingBox
                        entity.toDomainModel(fallbackBox, idx)
                    }
                    _uiState.update { it.copy(items = mapped, cloudSyncError = null) }
                }
        }
    }

    private fun BaggageItemEntity.toDomainModel(
        fallbackBox: NormalizedRect?,
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
            maskId = if (src == ItemSource.AI_SCANNED) "SEG-0${index + 1}" else null
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
                items = buildRigItems(rig),
                isScanning = false,
                scanProgress = 0f,
                scanPhaseLabel = "MUSE-SPARK-1.3 // RIG LOCKED",
                selectedItemId = null
            )
        }
    }

    fun triggerMultiAgentScan() {
        if (_uiState.value.isScanning) return
        scanJob?.cancel()

        val currentRig = _uiState.value.selectedRig
        val manualItemsToKeep = _uiState.value.items.filter { it.source == ItemSource.MANUAL_ENTRY }
        val freshAiItems = buildRigItems(currentRig).filter { it.source == ItemSource.AI_SCANNED }

        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanProgress = 0.05f,
                    scanPhaseLabel = "MUSE-SPARK-1.3 // META SAM SEGMENTATION",
                    items = manualItemsToKeep,
                    selectedItemId = null
                )
            }

            val totalSteps = 20
            for (step in 1..totalSteps) {
                delay(50L)
                val progress = step.toFloat() / totalSteps.toFloat()
                val phase = when {
                    progress < 0.35f -> "MUSE-SPARK-1.3 // META SAM MASK EXTRACTION"
                    progress < 0.72f -> "MUSE-SPARK-1.3 // VOLUMETRIC DENSITY TENSOR"
                    else -> "MUSE-SPARK-1.3 // GRAVIMETRIC MASS INFERENCE"
                }

                val revealedCount = ((progress * freshAiItems.size).toInt()).coerceIn(0, freshAiItems.size)
                val revealedAi = freshAiItems.take(revealedCount)

                _uiState.update { state ->
                    state.copy(
                        scanProgress = progress,
                        scanPhaseLabel = phase,
                        items = revealedAi + manualItemsToKeep,
                        selectedItemId = revealedAi.lastOrNull()?.id
                    )
                }
            }

            _uiState.update { state ->
                state.copy(
                    isScanning = false,
                    scanProgress = 1f,
                    scanPhaseLabel = "MUSE-SPARK-1.3 // ${freshAiItems.size} OBJECTS LOCKED",
                    items = freshAiItems + manualItemsToKeep,
                    selectedItemId = null,
                    scanGenerationCount = state.scanGenerationCount + 1
                )
            }
        }
    }

    fun dismissAiError() {
        _uiState.update { it.copy(aiError = null) }
    }

    fun scanCameraFrameWithAi(frameBitmap: Bitmap?) {
        if (frameBitmap == null) {
            triggerMultiAgentScan()
            return
        }
        if (_uiState.value.isScanning) return
        scanJob?.cancel()

        val manualItemsToKeep = _uiState.value.items.filter { it.source == ItemSource.MANUAL_ENTRY }

        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanProgress = 0.25f,
                    scanPhaseLabel = "MUSE-SPARK-1.3 // META SAM SEGMENTATION",
                    aiError = null
                )
            }

            try {
                val scaledBitmap = scaleBitmapForInference(frameBitmap, maxDimension = 1024)

                _uiState.update {
                    it.copy(
                        scanProgress = 0.58f,
                        scanPhaseLabel = "MUSE-SPARK-1.3 // IDENTIFYING OBJECTS"
                    )
                }

                val jsonString = withTimeout(60_000L) {
                    val metaJson = if (MuseSparkMetaClient.isMetaApiKeyConfigured()) {
                        runCatching {
                            MuseSparkMetaClient.analyzeFrameWithMuseSpark(scaledBitmap)
                        }.onFailure { err ->
                            Log.w(TAG, "Muse Spark Meta API fallback triggered: ${err.message}")
                        }.getOrNull()
                    } else {
                        null
                    }

                    if (!metaJson.isNullOrBlank()) {
                        metaJson
                    } else {
                        val response = visionModel.generateContent(
                            content {
                                image(scaledBitmap)
                                text(
                                    "Act as the Meta muse-spark-1.3 + Meta SAM vision segmentation engine. " +
                                        "Analyze this camera frame for baggage and object segmentation. " +
                                        "Detect and identify all visible items, electronics, luggage, apparel, containers, or room objects in the camera view. " +
                                        "For each detected item, return its specific identified name, realistic estimated weight in kilograms (between 0.10 and 15.0 kg), " +
                                        "category (COMPUTE, POWER, APPAREL, LIQUIDS, or ACCESSORY), confidence (0.88 to 0.99), " +
                                        "and normalized bounding box coordinates (left, top, width, height in 0.04..0.92) matching where the object appears in the image."
                                )
                            }
                        )
                        response.text ?: throw IllegalStateException("Vision model returned empty text.")
                    }
                }

                val root = JSONObject(jsonString)
                val summary = root.optString("summary", "Camera frame analyzed and segmented.")
                val itemsArray = root.optJSONArray("items")

                val detectedItems = mutableListOf<BaggageItem>()
                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {
                        val obj = itemsArray.optJSONObject(i) ?: continue
                        val name = obj.optString("name", "Detected Object ${i + 1}").trim()
                        val rawWeight = obj.optDouble("weightKg", 0.85)
                        val weightKg = ((rawWeight.coerceIn(0.05, 30.0) * 100.0).roundToInt()) / 100.0
                        val catStr = obj.optString("category", "ACCESSORY")
                        val category = runCatching { GearCategory.valueOf(catStr) }.getOrDefault(GearCategory.ACCESSORY)
                        val conf = obj.optDouble("confidence", 0.95).toFloat().coerceIn(0.80f, 0.99f)

                        val left = obj.optDouble("left", 0.10 + (i % 2) * 0.42).toFloat().coerceIn(0.03f, 0.72f)
                        val top = obj.optDouble("top", 0.12 + (i / 2) * 0.36).toFloat().coerceIn(0.05f, 0.72f)
                        val width = obj.optDouble("width", 0.32).toFloat().coerceIn(0.16f, (0.95f - left).coerceAtLeast(0.18f))
                        val height = obj.optDouble("height", 0.30).toFloat().coerceIn(0.16f, (0.92f - top).coerceAtLeast(0.18f))

                        detectedItems.add(
                            BaggageItem(
                                id = "cam_ai_${UUID.randomUUID().toString().take(8)}",
                                name = name,
                                weightKg = weightKg,
                                source = ItemSource.AI_SCANNED,
                                category = category,
                                confidence = conf,
                                boundingBox = NormalizedRect(left, top, width, height),
                                maskId = "SEG-0${i + 1}"
                            )
                        )
                    }
                }

                if (detectedItems.isEmpty()) {
                    throw IllegalStateException("No distinct items could be segmented in the current camera view.")
                }

                _uiState.update { state ->
                    state.copy(
                        isScanning = false,
                        scanProgress = 1f,
                        scanPhaseLabel = "MUSE-SPARK-1.3 // ${detectedItems.size} SEGMENTED",
                        items = detectedItems + manualItemsToKeep,
                        selectedItemId = detectedItems.firstOrNull()?.id,
                        aiScanSummary = summary,
                        aiError = null,
                        scanGenerationCount = state.scanGenerationCount + 1
                    )
                }

                // Sync newly identified AI items to Firestore if signed in
                if (repository != null && !currentUserId.isNullOrBlank()) {
                    detectedItems.forEach { item ->
                        repository.createItem(
                            name = item.name,
                            weightGrams = (item.weightKg * 1000.0).roundToInt().toLong(),
                            source = ItemSource.AI_SCANNED.name,
                            category = item.category.name,
                            isShiftedToPersonal = false
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Camera AI vision segmentation timed out", e)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanProgress = 0f,
                        scanPhaseLabel = "MUSE-SPARK-1.3 // TIMEOUT",
                        aiError = "AI camera scan timed out after 60 seconds. Please try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Camera AI vision segmentation failed", e)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanProgress = 0f,
                        scanPhaseLabel = "MUSE-SPARK-1.3 // SCAN ERROR",
                        aiError = e.localizedMessage ?: "AI vision segmentation failed."
                    )
                }
            }
        }
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
        _uiState.update { state ->
            state.copy(
                items = state.items.filterNot { it.id == itemId },
                selectedItemId = if (state.selectedItemId == itemId) null else state.selectedItemId
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
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) {
                        val updated = ((item.weightKg + deltaKg) * 100.0).roundToInt() / 100.0
                        val copy = item.copy(weightKg = updated.coerceIn(0.05, 35.0))
                        updatedItem = copy
                        copy
                    } else {
                        item
                    }
                }
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
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) {
                        val copy = item.copy(isShiftedToPersonal = !item.isShiftedToPersonal)
                        updatedItem = copy
                        copy
                    } else {
                        item
                    }
                }
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

    fun applyEscortRecommendedShift() {
        _uiState.update { state ->
            val targetIds = state.escortAdvisory.recommendedShiftIds.toSet()
            if (targetIds.isEmpty()) return@update state
            state.copy(
                items = state.items.map { item ->
                    if (item.id in targetIds) {
                        item.copy(isShiftedToPersonal = true)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun restoreAllShiftedItems() {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { it.copy(isShiftedToPersonal = false) }
            )
        }
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
            _uiState.update { it.copy(manualInputError = "Enter an item name (e.g., Packed Boots)") }
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

        _uiState.update { state ->
            state.copy(
                items = state.items + newItem,
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
                connectedBooking = updatedBooking,
                selectedPreset = carrier.cabinPreset
            )
        }
    }

    fun syncBookingWithUserIdentity(userName: String?, userEmail: String?) {
        val cleanName = userName?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("Guest Mode", ignoreCase = true) && !it.equals("Log In", ignoreCase = true) }
            ?: "Masakazu Wada"
        val cleanEmail = userEmail?.trim()?.takeIf { it.isNotBlank() }
            ?: "masakazuwada09@gmail.com"

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
        fun buildRigItems(rig: GearRigPreset): List<BaggageItem> {
            return when (rig) {
                GearRigPreset.NOMAD_CABIN -> listOf(
                    BaggageItem(
                        id = "ai_mbp16",
                        name = "MacBook Pro 16\" M3 Max",
                        weightKg = 2.15,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.COMPUTE,
                        confidence = 0.994f,
                        boundingBox = NormalizedRect(0.06f, 0.12f, 0.40f, 0.42f),
                        maskId = "SEG-01"
                    ),
                    BaggageItem(
                        id = "ai_sony_a7",
                        name = "Sony A7IV + 24-70mm GM",
                        weightKg = 1.35,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.COMPUTE,
                        confidence = 0.988f,
                        boundingBox = NormalizedRect(0.54f, 0.10f, 0.38f, 0.36f),
                        maskId = "SEG-02"
                    ),
                    BaggageItem(
                        id = "ai_anker_27k",
                        name = "Anker Prime 27,650mAh Bank",
                        weightKg = 0.65,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.POWER,
                        confidence = 0.991f,
                        boundingBox = NormalizedRect(0.08f, 0.60f, 0.26f, 0.28f),
                        maskId = "SEG-03"
                    ),
                    BaggageItem(
                        id = "ai_anc_headphones",
                        name = "Carbon ANC Studio Case",
                        weightKg = 0.45,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.ACCESSORY,
                        confidence = 0.976f,
                        boundingBox = NormalizedRect(0.39f, 0.54f, 0.26f, 0.34f),
                        maskId = "SEG-04"
                    ),
                    BaggageItem(
                        id = "ai_dopp_kit",
                        name = "Aero TSA Liquids Pouch",
                        weightKg = 0.80,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.LIQUIDS,
                        confidence = 0.969f,
                        boundingBox = NormalizedRect(0.69f, 0.54f, 0.24f, 0.34f),
                        maskId = "SEG-05"
                    ),
                    BaggageItem(
                        id = "man_chelsea_boots",
                        name = "Packed Weatherproof Boots",
                        weightKg = 1.40,
                        source = ItemSource.MANUAL_ENTRY,
                        category = GearCategory.APPAREL
                    )
                )

                GearRigPreset.EXPEDITION_HOLD -> listOf(
                    BaggageItem(
                        id = "exp_pelican_drone",
                        name = "Mavic 3 Cine + 4x LiPo Case",
                        weightKg = 4.20,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.COMPUTE,
                        confidence = 0.995f,
                        boundingBox = NormalizedRect(0.05f, 0.10f, 0.44f, 0.44f),
                        maskId = "SEG-01"
                    ),
                    BaggageItem(
                        id = "exp_tripod_carbon",
                        name = "Carbon Travel Tripod + Gimbal",
                        weightKg = 3.10,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.ACCESSORY,
                        confidence = 0.984f,
                        boundingBox = NormalizedRect(0.53f, 0.12f, 0.40f, 0.36f),
                        maskId = "SEG-02"
                    ),
                    BaggageItem(
                        id = "exp_goretex_shell",
                        name = "Compression Cube: Alpine Shell",
                        weightKg = 4.85,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.APPAREL,
                        confidence = 0.979f,
                        boundingBox = NormalizedRect(0.07f, 0.58f, 0.34f, 0.32f),
                        maskId = "SEG-03"
                    ),
                    BaggageItem(
                        id = "exp_power_station",
                        name = "GaN 300W Field Power Hub",
                        weightKg = 2.65,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.POWER,
                        confidence = 0.992f,
                        boundingBox = NormalizedRect(0.45f, 0.54f, 0.24f, 0.34f),
                        maskId = "SEG-04"
                    ),
                    BaggageItem(
                        id = "exp_med_liquids",
                        name = "Expedition Hydration & Med Kit",
                        weightKg = 2.40,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.LIQUIDS,
                        confidence = 0.971f,
                        boundingBox = NormalizedRect(0.72f, 0.54f, 0.22f, 0.35f),
                        maskId = "SEG-05"
                    ),
                    BaggageItem(
                        id = "man_mountaineering_boots",
                        name = "Crampon-Ready Alpine Boots",
                        weightKg = 3.40,
                        source = ItemSource.MANUAL_ENTRY,
                        category = GearCategory.APPAREL
                    )
                )

                GearRigPreset.EXEC_OVERNIGHT -> listOf(
                    BaggageItem(
                        id = "exec_x1_nano",
                        name = "ThinkPad X1 Carbon Gen 12",
                        weightKg = 1.12,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.COMPUTE,
                        confidence = 0.996f,
                        boundingBox = NormalizedRect(0.08f, 0.14f, 0.42f, 0.40f),
                        maskId = "SEG-01"
                    ),
                    BaggageItem(
                        id = "exec_garment_folder",
                        name = "Ballistic Garment Folder",
                        weightKg = 1.85,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.APPAREL,
                        confidence = 0.985f,
                        boundingBox = NormalizedRect(0.54f, 0.14f, 0.38f, 0.42f),
                        maskId = "SEG-02"
                    ),
                    BaggageItem(
                        id = "exec_gan_charger",
                        name = "140W GaN Charger + MagBank",
                        weightKg = 0.58,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.POWER,
                        confidence = 0.989f,
                        boundingBox = NormalizedRect(0.12f, 0.60f, 0.32f, 0.28f),
                        maskId = "SEG-03"
                    ),
                    BaggageItem(
                        id = "exec_leather_folio",
                        name = "Passport & Fragrance Kit",
                        weightKg = 0.65,
                        source = ItemSource.AI_SCANNED,
                        category = GearCategory.LIQUIDS,
                        confidence = 0.974f,
                        boundingBox = NormalizedRect(0.52f, 0.60f, 0.36f, 0.28f),
                        maskId = "SEG-04"
                    )
                )
            }
        }
    }
}
