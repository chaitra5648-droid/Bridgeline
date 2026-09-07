package com.narayani.bridgeline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.narayani.bridgeline.networking.BridgelinePacket
import com.narayani.bridgeline.networking.NearbyManager
import com.narayani.bridgeline.ui.theme.BridgelineTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var nearbyManager: NearbyManager? = null

    private var isMasterNode by mutableStateOf(false)

    private val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (hasAllRequiredPermissions()) {
                startNearby()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        setContent {
            BridgelineTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    ComposerScreen(
                        modifier = Modifier.padding(innerPadding),
                        fusedLocationClient = fusedLocationClient,
                        isMaster = isMasterNode,

                        onModeChanged = { master ->

                            isMasterNode = master
                            restartNearby()
                        },

                        onStartNearby = {
                            requestPermissionsIfNeeded()
                        },

                        onSendPacket = { packet ->

                            nearbyManager?.sendPacket(packet)
                        }
                    )
                }
            }
        }

        requestPermissionsIfNeeded()
    }

    // ---------------------------------------------------------
    // PERMISSIONS
    // ---------------------------------------------------------

    private fun requestPermissionsIfNeeded() {

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permissions.add(
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        }

        permissions.add(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        permissions.add(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val missingPermissions =
            permissions.filter { permission ->

                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (missingPermissions.isNotEmpty()) {

            requestPermissionsLauncher.launch(
                missingPermissions.toTypedArray()
            )

        } else {

            startNearby()
        }
    }

    private fun hasAllRequiredPermissions(): Boolean {

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permissions.add(
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        }

        /*
         * GPS only requires either fine OR coarse.
         */
        val fineLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocation && !coarseLocation) {
            return false
        }

        return permissions.all { permission ->

            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // ---------------------------------------------------------
    // NEARBY
    // ---------------------------------------------------------

    private fun startNearby() {

        if (!hasAllRequiredPermissions()) {
            return
        }

        nearbyManager?.stop()

        nearbyManager = NearbyManager(
            this,
            isMasterNode,

            onPacketReceived = { packet ->

                runOnUiThread {

                    receivedPackets.add(
                        0,
                        DisplayMessage(
                            packet = packet,
                            displayStatus =
                                if (isMasterNode) {
                                    "RECEIVED BY MASTER"
                                } else {
                                    "RECEIVED"
                                }
                        )
                    )
                }
            },

            onRelay = { packet ->

                runOnUiThread {

                    receivedPackets.add(
                        0,
                        DisplayMessage(
                            packet = packet,
                            displayStatus =
                                "RELAYED • TTL ${packet.ttl}"
                        )
                    )
                }
            }
        )

        nearbyManager?.start()
    }

    private fun restartNearby() {

        nearbyManager?.stop()

        nearbyManager = null

        if (hasAllRequiredPermissions()) {
            startNearby()
        }
    }

    override fun onDestroy() {

        nearbyManager?.stop()

        nearbyManager = null

        super.onDestroy()
    }

    companion object {

        val receivedPackets =
            mutableStateListOf<DisplayMessage>()
    }
}


// =============================================================
// DISPLAY MESSAGE
// =============================================================

data class DisplayMessage(
    val packet: BridgelinePacket,
    val displayStatus: String
)


// =============================================================
// COMPOSER SCREEN
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    modifier: Modifier = Modifier,
    fusedLocationClient: FusedLocationProviderClient,
    isMaster: Boolean,
    onModeChanged: (Boolean) -> Unit,
    onStartNearby: () -> Unit,
    onSendPacket: (BridgelinePacket) -> Unit
) {

    val context = LocalContext.current

    var messageText by remember {
        mutableStateOf("")
    }

    var peopleCount by remember {
        mutableStateOf("")
    }

    var priority by remember {
        mutableStateOf("Medium")
    }

    var statusText by remember {
        mutableStateOf("")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    val priorities =
        listOf(
            "Low",
            "Medium",
            "High"
        )


    // ---------------------------------------------------------
    // PRIORITY CONVERSION
    // ---------------------------------------------------------

    fun priorityNumber(value: String): Int {

        return when (value) {

            "Low" -> 1

            "Medium" -> 2

            "High" -> 3

            else -> 2
        }
    }


    // ---------------------------------------------------------
    // SEND MESSAGE
    // ---------------------------------------------------------

    fun sendMessage(
        text: String,
        sos: Boolean
    ) {

        if (text.isBlank() && !sos) {

            statusText =
                "Enter a message first"

            return
        }


        /*
         * IMPORTANT:
         *
         * Accept either FINE or COARSE location.
         *
         * Previously this only checked FINE location,
         * which caused:
         *
         * "Location permission not granted"
         *
         * even when Android had granted approximate location.
         */

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (!fineLocationGranted && !coarseLocationGranted) {

            statusText =
                "Location permission not granted"

            return
        }


        /*
         * Get the phone's most recent GPS location.
         */

        fusedLocationClient
            .getLastLocation()
            .addOnSuccessListener { location ->

                /*
                 * If Android does not currently have a
                 * cached location, use 0.0 instead of crashing.
                 */

                val lat =
                    location?.latitude ?: 0.0

                val lng =
                    location?.longitude ?: 0.0


                /*
                 * SOS gets its own emergency message.
                 */

                val finalText =
                    if (sos) {

                        if (text.isBlank()) {

                            "SOS EMERGENCY - Immediate help needed"

                        } else {

                            "SOS EMERGENCY - $text"
                        }

                    } else {

                        text
                    }


                /*
                 * Create the actual Bridgeline packet.
                 *
                 * This is what gets passed to NearbyManager.
                 */

                val packet =
                    BridgelinePacket(

                        id =
                            UUID.randomUUID().toString(),

                        text =
                            finalText,

                        lat =
                            lat,

                        long =
                            lng,

                        priority =
                            if (sos) {
                                3
                            } else {
                                priorityNumber(priority)
                            },

                        peopleCount =
                            peopleCount.toIntOrNull() ?: 0,

                        status =
                            "SENT",

                        isSOS =
                            sos,

                        ttl =
                            15
                    )


                /*
                 * Send packet into the Nearby mesh.
                 */

                onSendPacket(packet)


                /*
                 * Also display the packet on the
                 * sender's screen.
                 */

                MainActivity.receivedPackets.add(
                    0,
                    DisplayMessage(
                        packet = packet,

                        displayStatus =
                            if (sos) {
                                "SOS SENT • TTL 15"
                            } else {
                                "SENT • TTL 15"
                            }
                    )
                )


                statusText =
                    if (sos) {

                        "HIGH ALERT SOS SENT at ($lat, $lng)"

                    } else {

                        "Message sent at ($lat, $lng)"
                    }


                if (!sos) {
                    messageText = ""
                }
            }

            .addOnFailureListener {

                statusText =
                    "Could not get GPS location"
            }
    }


    // =========================================================
    // UI
    // =========================================================

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "BRIDGELINE",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text =
                if (isMaster) {
                    "MASTER NODE"
                } else {
                    "RELAY NODE"
                },

            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // =====================================================
        // RELAY / MASTER
        // =====================================================

        Text(
            text = "NETWORK MODE",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            Button(
                onClick = {
                    onModeChanged(false)
                },

                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {

                Text("RELAY MODE")
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Button(
                onClick = {
                    onModeChanged(true)
                },

                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {

                Text("MASTER MODE")
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )


        Button(
            onClick = onStartNearby,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                if (isMaster) {
                    "START MASTER NETWORK"
                } else {
                    "START RELAY NETWORK"
                }
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // SOS
        // =====================================================

        Button(
            onClick = {
                sendMessage(
                    "",
                    true
                )
            },

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color.Red
                ),

            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {

            Text(
                text =
                    "HIGH ALERT SOS - SEND EMERGENCY ALERT",

                color =
                    Color.White
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // MESSAGE
        // =====================================================

        Text(
            text =
                "Or send a detailed message:",

            style =
                MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )


        OutlinedTextField(
            value = messageText,

            onValueChange = {
                messageText = it
            },

            label = {
                Text("Type your message")
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // =====================================================
        // PEOPLE COUNT
        // =====================================================

        OutlinedTextField(
            value = peopleCount,

            onValueChange = {
                peopleCount = it
            },

            label = {
                Text("Number of people in danger")
            },

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Number
                ),

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // =====================================================
        // PRIORITY
        // =====================================================

        ExposedDropdownMenuBox(
            expanded = expanded,

            onExpandedChange = {
                expanded = !expanded
            }
        ) {

            OutlinedTextField(
                value = priority,

                onValueChange = {},

                readOnly = true,

                label = {
                    Text("Priority")
                },

                modifier =
                    Modifier
                        .menuAnchor()
                        .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,

                onDismissRequest = {
                    expanded = false
                }
            ) {

                priorities.forEach { option ->

                    DropdownMenuItem(

                        text = {
                            Text(option)
                        },

                        onClick = {

                            priority = option

                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // =====================================================
        // SEND BUTTON
        // =====================================================

        Button(
            onClick = {

                sendMessage(
                    messageText,
                    false
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("SEND MESSAGE")
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )


        Text(
            text = statusText,

            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // MESH ACTIVITY
        // =====================================================

        Text(
            text =
                "MESH MESSAGE ACTIVITY",

            style =
                MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )


        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
        ) {

            items(
                items =
                    MainActivity.receivedPackets
            ) { item ->

                val packet =
                    item.packet


                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 4.dp
                            ),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (packet.isSOS) {
                                    Color(0xFFB71C1C)
                                } else {
                                    Color(0xFF2A2A2A)
                                }
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(14.dp)
                    ) {


                        if (packet.isSOS) {

                            Text(
                                text =
                                    "HIGH ALERT SOS",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,

                                color =
                                    Color.White
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )
                        }


                        Text(
                            text =
                                packet.text,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,

                            color =
                                if (packet.isSOS) {
                                    Color.White
                                } else {
                                    Color(0xFFE0E0E0)
                                }
                        )


                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )


                        Text(
                            text =
                                item.displayStatus,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                if (packet.isSOS) {
                                    Color(0xFFFFCDD2)
                                } else {
                                    Color(0xFFB0B0B0)
                                }
                        )


                        Text(
                            text =
                                "Priority: ${packet.priority} | " +
                                        "People: ${packet.peopleCount}",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                if (packet.isSOS) {
                                    Color(0xFFFFCDD2)
                                } else {
                                    Color(0xFFB0B0B0)
                                }
                        )


                        Text(
                            text =
                                "Status: ${packet.status}",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                if (packet.isSOS) {
                                    Color(0xFFFFCDD2)
                                } else {
                                    Color(0xFFB0B0B0)
                                }
                        )


                        Text(
                            text =
                                "TTL: ${packet.ttl}",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                if (packet.isSOS) {
                                    Color(0xFFFFCDD2)
                                } else {
                                    Color(0xFFB0B0B0)
                                }
                        )


                        Text(
                            text =
                                "Location: " +
                                        "(${String.format("%.4f", packet.lat)}, " +
                                        "${String.format("%.4f", packet.long)})",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                if (packet.isSOS) {
                                    Color(0xFFFFCDD2)
                                } else {
                                    Color(0xFFB0B0B0)
                                }
                        )


                        Text(
                            text =
                                "ID: ${packet.id}",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                Color(0xFF808080)
                        )
                    }
                }
            }
        }
    }
}