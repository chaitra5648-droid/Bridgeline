package com.narayani.bridgeline

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.narayani.bridgeline.ui.theme.BridgelineTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            BridgelineTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ComposerScreen(
                        modifier = Modifier.padding(innerPadding),
                        fusedLocationClient = fusedLocationClient,
                        hasPermission = {
                            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        }
                    )
                }
            }
        }
    }
}

data class SentMessage(
    val id: String,
    val text: String,
    val lat: Double,
    val lng: Double,
    val priority: String,
    val peopleCount: String,
    val status: String,
    val isSOS: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    modifier: Modifier = Modifier,
    fusedLocationClient: FusedLocationProviderClient,
    hasPermission: () -> Boolean
) {
    var messageText by remember { mutableStateOf("") }
    var peopleCount by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var statusText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val priorities = listOf("Low", "Medium", "High")

    val messageHistory = remember { mutableStateListOf<SentMessage>() }

    fun sendMessage(text: String, isSOS: Boolean) {
        if (text.isBlank() && !isSOS) {
            statusText = "Enter a message first"
            return
        }

        if (hasPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0

                val rawText = if (isSOS) "SOS EMERGENCY - Immediate help needed" else text
                val finalText = CryptoHelper.encrypt(rawText)

                val newMessage = SentMessage(
                    id = UUID.randomUUID().toString(),
                    text = finalText,
                    lat = lat,
                    lng = lng,
                    priority = if (isSOS) "High" else priority,
                    peopleCount = if (peopleCount.isBlank()) "Not specified" else peopleCount,
                    status = "Sent",
                    isSOS = isSOS
                )
                messageHistory.add(0, newMessage)
                statusText = if (isSOS) "HIGH ALERT SOS SENT at ($lat, $lng)" else "Sent at ($lat, $lng)"
                if (!isSOS) messageText = ""
            }
        } else {
            statusText = "Location permission not granted"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Bridgeline - Message Composer", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // SOS BUTTON
        Button(
            onClick = { sendMessage("", isSOS = true) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("HIGH ALERT SOS - SEND EMERGENCY ALERT", color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Or send a detailed message:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Type your message") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = peopleCount,
            onValueChange = { peopleCount = it },
            label = { Text("Number of people in danger") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = priority,
                onValueChange = {},
                readOnly = true,
                label = { Text("Priority") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                priorities.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            priority = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { sendMessage(messageText, isSOS = false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = statusText, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Message History", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(messageHistory) { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.isSOS) Color(0xFFB71C1C) else Color(0xFF2A2A2A)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (msg.isSOS) {
                            Text(
                                text = "HIGH ALERT SOS ALERT",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isSOS) Color.White else Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "People in danger: ${msg.peopleCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (msg.isSOS) Color(0xFFFFCDD2) else Color(0xFFB0B0B0)
                        )
                        Text(
                            text = "Priority: ${msg.priority} | Status: ${msg.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (msg.isSOS) Color(0xFFFFCDD2) else Color(0xFFB0B0B0)
                        )
                        Text(
                            text = "Location: (${String.format("%.4f", msg.lat)}, ${String.format("%.4f", msg.lng)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (msg.isSOS) Color(0xFFFFCDD2) else Color(0xFFB0B0B0)
                        )
                    }
                }
            }
        }
    }
}