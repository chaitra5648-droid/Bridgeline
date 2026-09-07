package com.narayani.bridgeline.networking

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class NearbyManager(
    appContext: Context,
    private val isMasterNode: Boolean,
    private val onPacketReceived: (BridgelinePacket) -> Unit,
    private val onRelay: (BridgelinePacket) -> Unit
) {

    private val connectionsClient =
        Nearby.getConnectionsClient(appContext)

    private val connectedEndpoints = mutableSetOf<String>()
    private val seenPacketIds = mutableSetOf<String>()

    companion object {
        private const val TAG = "NearbyManager"
        private const val SERVICE_ID = "com.narayani.bridgeline"
        private const val DEVICE_NAME = "Bridgeline Device"
        private val STRATEGY = Strategy.P2P_CLUSTER
    }

    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {
                if (payload.type != Payload.Type.BYTES) {
                    return
                }

                val bytes = payload.asBytes() ?: return
                val json = bytes.toString(Charsets.UTF_8)

                try {
                    val packet =
                        BridgelinePacket.fromJson(json)

                    Log.d(
                        TAG,
                        "Packet received: ${packet.id}"
                    )

                    if (seenPacketIds.contains(packet.id)) {
                        Log.d(
                            TAG,
                            "Duplicate ignored: ${packet.id}"
                        )
                        return
                    }

                    seenPacketIds.add(packet.id)

                    onPacketReceived(packet)

                    if (!isMasterNode) {
                        relayPacket(
                            packet,
                            endpointId
                        )
                    }

                } catch (exception: Exception) {
                    Log.e(
                        TAG,
                        "Invalid packet",
                        exception
                    )
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
                Log.d(
                    TAG,
                    "Payload transfer: ${update.status}"
                )
            }
        }

    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {
                Log.d(
                    TAG,
                    "Connection initiated with ${connectionInfo.endpointName}"
                )

                connectionsClient
                    .acceptConnection(
                        endpointId,
                        payloadCallback
                    )
                    .addOnSuccessListener {
                        Log.d(
                            TAG,
                            "Connection accepted: $endpointId"
                        )
                    }
                    .addOnFailureListener {
                        Log.e(
                            TAG,
                            "Could not accept connection",
                            it
                        )
                    }
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                if (result.status.isSuccess) {

                    connectedEndpoints.add(
                        endpointId
                    )

                    Log.d(
                        TAG,
                        "CONNECTED to $endpointId"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Connection failed: $endpointId"
                    )
                }
            }

            override fun onDisconnected(
                endpointId: String
            ) {

                connectedEndpoints.remove(
                    endpointId
                )

                Log.d(
                    TAG,
                    "Disconnected from $endpointId"
                )
            }
        }

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {

                Log.d(
                    TAG,
                    "Found: ${info.endpointName}"
                )

                if (!connectedEndpoints.contains(endpointId)) {

                    connectionsClient
                        .requestConnection(
                            DEVICE_NAME,
                            endpointId,
                            connectionLifecycleCallback
                        )
                        .addOnSuccessListener {
                            Log.d(
                                TAG,
                                "Connection request sent"
                            )
                        }
                        .addOnFailureListener {
                            Log.e(
                                TAG,
                                "Connection request failed",
                                it
                            )
                        }
                }
            }

            override fun onEndpointLost(
                endpointId: String
            ) {

                Log.d(
                    TAG,
                    "Lost: $endpointId"
                )
            }
        }

    fun start() {

        Log.d(
            TAG,
            "Starting Bridgeline network"
        )

        startAdvertising()
        startDiscovery()
    }

    private fun startAdvertising() {

        val options =
            AdvertisingOptions.Builder()
                .setStrategy(STRATEGY)
                .build()

        connectionsClient
            .startAdvertising(
                DEVICE_NAME,
                SERVICE_ID,
                connectionLifecycleCallback,
                options
            )
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "Advertising started"
                )
            }
            .addOnFailureListener {
                Log.e(
                    TAG,
                    "Advertising failed",
                    it
                )
            }
    }

    private fun startDiscovery() {

        val options =
            DiscoveryOptions.Builder()
                .setStrategy(STRATEGY)
                .build()

        connectionsClient
            .startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                options
            )
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "Discovery started"
                )
            }
            .addOnFailureListener {
                Log.e(
                    TAG,
                    "Discovery failed",
                    it
                )
            }
    }

    fun sendPacket(
        packet: BridgelinePacket
    ) {

        if (connectedEndpoints.isEmpty()) {

            Log.e(
                TAG,
                "No connected devices"
            )

            return
        }

        seenPacketIds.add(
            packet.id
        )

        val payload =
            Payload.fromBytes(
                packet.toJson()
                    .toByteArray(Charsets.UTF_8)
            )

        connectionsClient
            .sendPayload(
                connectedEndpoints.toList(),
                payload
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Packet sent: ${packet.id}"
                )

            }
            .addOnFailureListener {

                Log.e(
                    TAG,
                    "Packet send failed",
                    it
                )
            }
    }

    private fun relayPacket(
        packet: BridgelinePacket,
        sourceEndpointId: String
    ) {

        val relayEndpoints =
            connectedEndpoints.filter {
                it != sourceEndpointId
            }

        if (relayEndpoints.isEmpty()) {

            Log.d(
                TAG,
                "No relay nodes available"
            )

            return
        }

        val payload =
            Payload.fromBytes(
                packet.toJson()
                    .toByteArray(Charsets.UTF_8)
            )

        connectionsClient
            .sendPayload(
                relayEndpoints,
                payload
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "Packet relayed: ${packet.id}"
                )

                onRelay(packet)
            }
            .addOnFailureListener {

                Log.e(
                    TAG,
                    "Packet relay failed",
                    it
                )
            }
    }

    fun stop() {

        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()

        connectedEndpoints.clear()
    }
}