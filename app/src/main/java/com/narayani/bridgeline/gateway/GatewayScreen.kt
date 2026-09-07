package com.narayani.bridgeline.gateway

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narayani.bridgeline.model.Message

@Composable
fun GatewayScreen(message: Message) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "BRIDGELINE GATEWAY",
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "🚨 SOS RECEIVED",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Message: ${message.text}",
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Location: ${message.latitude}, ${message.longitude}",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Priority: ${message.priority}",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "TTL: ${message.ttl}",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Message ID: ${message.id}",
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Status: DELIVERED",
            fontSize = 16.sp
        )
    }
}
@Composable
fun CriticalEmergencyScreen(message: Message) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = " CRITICAL EMERGENCY",
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Emergency assistance required",
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Location:",
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "${message.latitude}, ${message.longitude}",
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Status: EMERGENCY",
            fontSize = 16.sp
        )
    }
}