package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplaneTicket
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MuseSparkMetaClient
import com.example.model.AirlineCarrier
import com.example.model.BaggageGuardUiState
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DashboardBorderLight
import com.example.ui.theme.DashboardCardWhite
import com.example.ui.theme.DashboardChipGray
import com.example.ui.theme.DashboardSurfaceLight
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.FacebookBlueSoftBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AirlineLogoEmblem(
    carrier: AirlineCarrier,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(carrier.brandPrimaryHex)
    val secondaryColor = Color(carrier.brandSecondaryHex)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            when (carrier) {
                AirlineCarrier.CEBU_PACIFIC -> {
                    // Cebu Pacific sky wing + sun emblem
                    drawCircle(
                        color = Color(0xFFFFD600).copy(alpha = 0.85f),
                        radius = w * 0.18f,
                        center = Offset(w * 0.76f, h * 0.25f)
                    )
                    val wingPath = Path().apply {
                        moveTo(w * 0.12f, h * 0.78f)
                        quadraticTo(w * 0.45f, h * 0.48f, w * 0.88f, h * 0.62f)
                        lineTo(w * 0.88f, h * 0.86f)
                        close()
                    }
                    drawPath(wingPath, color = Color.White.copy(alpha = 0.22f))
                }

                AirlineCarrier.PHILIPPINE_AIRLINES -> {
                    // PAL dual-triangle tail fin + golden sun rays
                    val redFin = Path().apply {
                        moveTo(w * 0.55f, h * 0.12f)
                        lineTo(w * 0.92f, h * 0.88f)
                        lineTo(w * 0.48f, h * 0.88f)
                        close()
                    }
                    drawPath(redFin, color = Color(0xFFCE1126).copy(alpha = 0.85f))
                    drawCircle(
                        color = Color(0xFFFCD116),
                        radius = w * 0.13f,
                        center = Offset(w * 0.32f, h * 0.28f)
                    )
                }

                AirlineCarrier.AIRASIA -> {
                    // AirAsia signature red circle & swoosh
                    drawCircle(
                        color = Color.White.copy(alpha = 0.18f),
                        radius = w * 0.42f,
                        center = Offset(w * 0.72f, h * 0.28f)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = carrier.iataCode,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (size >= 44.dp) 15.sp else 12.sp,
                    lineHeight = 15.sp
                ),
                color = Color.White
            )
            Text(
                text = when (carrier) {
                    AirlineCarrier.CEBU_PACIFIC -> "CEB"
                    AirlineCarrier.PHILIPPINE_AIRLINES -> "PAL"
                    AirlineCarrier.AIRASIA -> "AIR"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 0.6.sp
                ),
                color = Color.White.copy(alpha = 0.92f)
            )
        }
    }
}

@Composable
fun ConnectedAirlineDashboardBanner(
    uiState: BaggageGuardUiState,
    onSelectCarrier: (AirlineCarrier) -> Unit,
    onApplyAllowanceMode: (Boolean) -> Unit,
    onOpenProfileBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val booking = uiState.connectedBooking
    val carrier = booking.carrier
    val isCheckedActive = booking.activeAllowanceMode == "CHECKED"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp))
            .testTag("dashboard_connected_airline_card"),
        color = DashboardCardWhite,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AirlineLogoEmblem(carrier = carrier, size = 46.dp)

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = carrier.carrierName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = FacebookBlueSoftBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PNR: ${booking.bookingCode}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = FacebookBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${booking.flightNumber} • ${booking.route}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${booking.passengerName} • ${booking.passengerEmail}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FacebookBlue
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenProfileBooking() }
                        .testTag("dashboard_open_booking_profile_btn"),
                    color = FacebookBlueSoftBg,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "E-Ticket",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FacebookBlue
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            // Real Booking Metadata Strip (E-Ticket, Seat, Terminal/Gate)
            Surface(
                color = DashboardSurfaceLight,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DashboardBorderLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "E-TICKET: ${booking.eTicketNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${booking.terminalGate} • Seat ${booking.seatNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = EmeraldSafe.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "CONFIRMED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                color = EmeraldSafe
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Quick Carrier Switcher Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AirlineCarrier.entries.forEach { option ->
                    val selected = option == carrier
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectCarrier(option) }
                            .testTag("dashboard_carrier_chip_${option.id}"),
                        color = if (selected) FacebookBlue else DashboardSurfaceLight,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, if (selected) FacebookBlue else DashboardBorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) Color.White else Color(option.brandPrimaryHex)
                                    )
                            )
                            Text(
                                text = "${option.carrierName} (${option.iataCode})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            // Baggage Inclusions Row (Cabin vs Checked)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onApplyAllowanceMode(false) },
                    color = if (!isCheckedActive) FacebookBlueSoftBg else DashboardSurfaceLight,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (!isCheckedActive) FacebookBlue else DashboardBorderLight
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "CABIN INCLUSION",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (!isCheckedActive) FacebookBlue else TextSecondary
                        )
                        Text(
                            text = "${BaggageGuardUiState.formatWeight(booking.cabinInclusionKg)} kg Carry-On",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onApplyAllowanceMode(true) },
                    color = if (isCheckedActive) FacebookBlueSoftBg else DashboardSurfaceLight,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isCheckedActive) FacebookBlue else DashboardBorderLight
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "CHECKED INCLUSION",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isCheckedActive) FacebookBlue else TextSecondary
                        )
                        Text(
                            text = "${BaggageGuardUiState.formatWeight(booking.checkedInclusionKg)} kg Hold Bag",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AirlineBookingProfileCard(
    uiState: BaggageGuardUiState,
    passengerName: String?,
    passengerEmail: String? = null,
    onBookingCodeChange: (String) -> Unit,
    onConnectAndAnalyzeBooking: (String?, String?) -> Unit,
    onApplyAllowanceMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val booking = uiState.connectedBooking
    val isCheckedActive = booking.activeAllowanceMode == "CHECKED"
    val resolvedName = passengerName?.takeIf {
        it.isNotBlank() && !it.equals("Guest Mode", ignoreCase = true) && !it.equals("Log In", ignoreCase = true)
    } ?: booking.passengerName
    val resolvedEmail = passengerEmail?.takeIf { it.isNotBlank() } ?: booking.passengerEmail

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp))
            .testTag("profile_airline_booking_card"),
        color = DashboardCardWhite,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AirplaneTicket,
                        contentDescription = null,
                        tint = FacebookBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "VERIFIED AIRLINE BOOKING DETAILS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Matched to $resolvedName ($resolvedEmail) • ${MuseSparkMetaClient.MUSE_SPARK_MODEL_ID}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = FacebookBlueSoftBg,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = MuseSparkMetaClient.MUSE_SPARK_MODEL_ID.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = FacebookBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Active Connected Booking Pass & Real Passenger / E-Ticket Details
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DashboardSurfaceLight,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DashboardBorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Airline Logo + PNR Code + Flight Route Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AirlineLogoEmblem(carrier = booking.carrier, size = 48.dp)

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = booking.carrier.carrierName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = TextPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Booking",
                                        tint = FacebookBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${booking.flightNumber} • ${booking.route}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = TextSecondary
                                )
                                Text(
                                    text = booking.fareBundle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = FacebookBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Booking Code Badge
                        Surface(
                            color = DashboardCardWhite,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FacebookBlue)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "BOOKING CODE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = TextSecondary
                                    )
                                )
                                Text(
                                    text = booking.bookingCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FacebookBlue
                                    )
                                )
                            }
                        }
                    }

                    // Real Passenger Record (Name, Email, E-Ticket, Schedule, Gate & Seat)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DashboardCardWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DashboardBorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PASSENGER NAME (IATA RECORD)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    )
                                    Text(
                                        text = "$resolvedName (${booking.iataPassengerName})",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                                Surface(
                                    color = EmeraldSafe.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = booking.bookingStatus,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = EmeraldSafe
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "REGISTERED BOOKING EMAIL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    )
                                    Text(
                                        text = resolvedEmail,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FacebookBlue
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "E-TICKET NUMBER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    )
                                    Text(
                                        text = booking.eTicketNumber,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = DashboardBorderLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DEPARTURE & TERMINAL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    )
                                    Text(
                                        text = booking.departureSchedule,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = booking.terminalGate,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "ASSIGNED SEAT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    )
                                    Text(
                                        text = booking.seatNumber,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Cabin & Checked Baggage Inclusions Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cabin Baggage Inclusion Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onApplyAllowanceMode(false) }
                                .testTag("profile_apply_cabin_inclusion"),
                            color = if (!isCheckedActive) FacebookBlueSoftBg else DashboardCardWhite,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (!isCheckedActive) 1.5.dp else 1.dp,
                                color = if (!isCheckedActive) FacebookBlue else DashboardBorderLight
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CABIN INCLUSION",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (!isCheckedActive) FacebookBlue else TextSecondary
                                    )
                                    if (!isCheckedActive) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = FacebookBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${BaggageGuardUiState.formatWeight(booking.cabinInclusionKg)} kg",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = booking.cabinInclusionDetails,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }

                        // Checked Baggage Inclusion Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onApplyAllowanceMode(true) }
                                .testTag("profile_apply_checked_inclusion"),
                            color = if (isCheckedActive) FacebookBlueSoftBg else DashboardCardWhite,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isCheckedActive) 1.5.dp else 1.dp,
                                color = if (isCheckedActive) FacebookBlue else DashboardBorderLight
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CHECKED INCLUSION",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isCheckedActive) FacebookBlue else TextSecondary
                                    )
                                    if (isCheckedActive) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = FacebookBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${BaggageGuardUiState.formatWeight(booking.checkedInclusionKg)} kg",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = booking.checkedInclusionDetails,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Muse Spark 1.3 Airline Booking Analysis Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DashboardCardWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DashboardBorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "MUSE MODEL (${booking.analyzedByModel.uppercase()}) BOOKING ANALYSIS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = FacebookBlue
                                    )
                                )
                            }

                            Text(
                                text = booking.museHeadline,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isOverweight) CrimsonAlert else EmeraldSafe
                                )
                            )

                            Text(
                                text = booking.museSummary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                ),
                                color = TextPrimary
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                booking.musePolicyTips.forEach { tip ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = FacebookBlue
                                            )
                                        )
                                        Text(
                                            text = tip,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            ),
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Booking Reference (PNR) Lookup & Re-Sync by Name & Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.bookingCodeInput,
                    onValueChange = onBookingCodeChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_booking_pnr_input"),
                    singleLine = true,
                    label = { Text("Booking Reference / PNR") },
                    placeholder = { Text(booking.bookingCode) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onConnectAndAnalyzeBooking(resolvedName, resolvedEmail)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FacebookBlue,
                        unfocusedBorderColor = DashboardBorderLight,
                        focusedContainerColor = DashboardSurfaceLight,
                        unfocusedContainerColor = DashboardSurfaceLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onConnectAndAnalyzeBooking(resolvedName, resolvedEmail)
                    },
                    enabled = !uiState.isCheckingBooking,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("profile_check_booking_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor = Color.White
                    )
                ) {
                    if (uiState.isCheckingBooking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Syncing...",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sync Booking",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

