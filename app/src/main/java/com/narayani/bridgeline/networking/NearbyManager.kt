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
    context: Context,
    private val isMasterNode: Boolean,
    private val onPacketReceived: (BridgelinePacket) -> Unit,
    private val onRelay: (BridgelinePacket) -> Unit
) {

    private val connectionsClient =
        Nearby.getConnectionsClient(context.applicationContext)


    private val connectedEndpoints =
        mutableSetOf<String>()

    private val pendingConnections =
        mutableSetOf<String>()


    private val seenPacketIds =
        mutableSetOf<String>()


    private val relayedPacketIds =
        mutableSetOf<String>()

    companion object {

        private const val TAG =
            "NearbyManager"

        private const val SERVICE_ID =
            "com.narayani.bridgeline"

        private const val DEVICE_NAME =
            "Bridgeline Device"

        private val STRATEGY =
            Strategy.P2P_CLUSTER
    }



    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {

                if (payload.type != Payload.Type.BYTES) {

                    Log.d(
                        TAG,
                        "Ignoring non-byte payload"
                    )

                    return
                }

                val bytes =
                    payload.asBytes()

                if (bytes == null) {

                    Log.e(
                        TAG,
                        "Received empty payload"
                    )

                    return
                }

                val json =
                    bytes.toString(
                        Charsets.UTF_8
                    )

                Log.d(
                    TAG,
                    "Payload received from $endpointId"
                )

                Log.d(
                    TAG,
                    "JSON: $json"
                )

                try {

                    val packet =
                        BridgelinePacket.fromJson(json)

                    Log.d(
                        TAG,
                        "Packet received: ${packet.id}"
                    )

                    /*
                     * Duplicate protection.
                     */
                    synchronized(seenPacketIds) {

                        if (
                            seenPacketIds.contains(
                                packet.id
                            )
                        ) {

                            Log.d(
                                TAG,
                                "Duplicate ignored: ${packet.id}"
                            )

                            return
                        }

                        seenPacketIds.add(
                            packet.id
                        )
                    }


                    /*
                     * EVERY node displays the packet it receives.
                     */
                    onPacketReceived(packet)


                    /*
                     * MASTER IS THE DESTINATION.
                     *
                     * Therefore the master does NOT relay.
                     */
                    if (isMasterNode) {

                        Log.d(
                            TAG,
                            "MASTER received packet ${packet.id}"
                        )

                        return
                    }



                    if (packet.ttl <= 0) {

                        Log.d(
                            TAG,
                            "TTL expired for ${packet.id}"
                        )

                        return
                    }



                    synchronized(relayedPacketIds) {

                        if (
                            relayedPacketIds.contains(
                                packet.id
                            )
                        ) {

                            Log.d(
                                TAG,
                                "Already relayed ${packet.id}"
                            )

                            return
                        }

                        relayedPacketIds.add(
                            packet.id
                        )
                    }


                    val relayPacket =
                        packet.copy(
                            ttl = packet.ttl - 1,
                            status = "RELAYED"
                        )


                    Log.d(
                        TAG,
                        "Relaying ${packet.id}: " +
                                "${packet.ttl} -> ${relayPacket.ttl}"
                    )


                    relayPacket(
                        relayPacket,
                        endpointId
                    )
                }

                catch (exception: Exception) {

                    Log.e(
                        TAG,
                        "Invalid packet received",
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
                    "Payload transfer " +
                            "endpoint=$endpointId " +
                            "status=${update.status} " +
                            "bytes=${update.bytesTransferred}"
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
                    "Connection initiated with " +
                            "${connectionInfo.endpointName} " +
                            "[$endpointId]"
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
                    .addOnFailureListener { error ->

                        Log.e(
                            TAG,
                            "Could not accept connection: $endpointId",
                            error
                        )
                    }
            }


            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {


                synchronized(pendingConnections) {

                    pendingConnections.remove(
                        endpointId
                    )
                }


                if (result.status.isSuccess) {

                    synchronized(connectedEndpoints) {

                        connectedEndpoints.add(
                            endpointId
                        )
                    }

                    Log.d(
                        TAG,
                        "================================"
                    )

                    Log.d(
                        TAG,
                        "CONNECTED: $endpointId"
                    )

                    Log.d(
                        TAG,
                        "Connected devices: " +
                                connectedEndpoints.size
                    )

                    Log.d(
                        TAG,
                        "================================"
                    )

                } else {

                    Log.e(
                        TAG,
                        "CONNECTION FAILED: $endpointId"

                    )
                }
            }


            override fun onDisconnected(
                endpointId: String
            ) {

                synchronized(connectedEndpoints) {

                    connectedEndpoints.remove(
                        endpointId
                    )
                }

                synchronized(pendingConnections) {

                    pendingConnections.remove(
                        endpointId
                    )
                }

                Log.d(
                    TAG,
                    "Disconnected: $endpointId"
                )

                Log.d(
                    TAG,
                    "Connected devices: " +
                            connectedEndpoints.size
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
                    "================================"
                )

                Log.d(
                    TAG,
                    "FOUND DEVICE"
                )

                Log.d(
                    TAG,
                    "Name: ${info.endpointName}"
                )

                Log.d(
                    TAG,
                    "Endpoint: $endpointId"
                )

                Log.d(
                    TAG,
                    "================================"
                )


                /*
                 * Already connected?
                 */
                synchronized(connectedEndpoints) {

                    if (
                        connectedEndpoints.contains(
                            endpointId
                        )
                    ) {

                        Log.d(
                            TAG,
                            "Already connected to $endpointId"
                        )

                        return
                    }
                }


                /*
                 * Connection already being requested?
                 */
                synchronized(pendingConnections) {

                    if (
                        pendingConnections.contains(
                            endpointId
                        )
                    ) {

                        Log.d(
                            TAG,
                            "Connection already pending: " +
                                    endpointId
                        )

                        return
                    }

                    pendingConnections.add(
                        endpointId
                    )
                }


                Log.d(
                    TAG,
                    "Requesting connection to $endpointId"
                )


                connectionsClient
                    .requestConnection(
                        DEVICE_NAME,
                        endpointId,
                        connectionLifecycleCallback
                    )
                    .addOnSuccessListener {

                        Log.d(
                            TAG,
                            "Connection request sent: " +
                                    endpointId
                        )
                    }
                    .addOnFailureListener { error ->

                        synchronized(
                            pendingConnections
                        ) {

                            pendingConnections.remove(
                                endpointId
                            )
                        }

                        Log.e(
                            TAG,
                            "Connection request failed: " +
                                    endpointId,
                            error
                        )
                    }
            }


            override fun onEndpointLost(
                endpointId: String
            ) {

                Log.d(
                    TAG,
                    "Endpoint lost: $endpointId"
                )

                /*
                 * Do NOT immediately remove the connection
                 * here. Endpoint discovery loss and connection
                 * loss are different things in Nearby.
                 */
            }
        }




    fun start() {

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "STARTING BRIDGELINE NETWORK"
        )

        Log.d(
            TAG,
            if (isMasterNode) {
                "MODE: MASTER"
            } else {
                "MODE: RELAY"
            }
        )

        Log.d(
            TAG,
            "================================"
        )


        /*
         * Both MASTER and RELAY nodes advertise.
         */
        startAdvertising()


        /*
         * Both MASTER and RELAY nodes discover.
         */
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
                    "Advertising started successfully"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    TAG,
                    "Advertising failed",
                    error
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
                    "Discovery started successfully"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    TAG,
                    "Discovery failed",
                    error
                )
            }
    }


    fun sendPacket(
        packet: BridgelinePacket
    ) {

        val endpoints =
            synchronized(connectedEndpoints) {

                connectedEndpoints.toList()
            }


        if (endpoints.isEmpty()) {

            Log.e(
                TAG,
                "================================"
            )

            Log.e(
                TAG,
                "NO CONNECTED DEVICES"
            )

            Log.e(
                TAG,
                "Packet ${packet.id} was NOT sent."
            )

            Log.e(
                TAG,
                "Wait until CONNECTED appears in Logcat."
            )

            Log.e(
                TAG,
                "================================"
            )

            return
        }


        synchronized(seenPacketIds) {

            seenPacketIds.add(
                packet.id
            )
        }


        val json =
            packet.toJson()

        val payload =
            Payload.fromBytes(
                json.toByteArray(
                    Charsets.UTF_8
                )
            )


        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "SENDING PACKET"
        )

        Log.d(
            TAG,
            "ID: ${packet.id}"
        )

        Log.d(
            TAG,
            "TTL: ${packet.ttl}"
        )

        Log.d(
            TAG,
            "Targets: ${endpoints.size}"
        )

        Log.d(
            TAG,
            "================================"
        )


        connectionsClient
            .sendPayload(
                endpoints,
                payload
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "PACKET SENT SUCCESSFULLY: " +
                            packet.id
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    TAG,
                    "PACKET SEND FAILED: " +
                            packet.id,
                    error
                )
            }
    }



    private fun relayPacket(
        packet: BridgelinePacket,
        sourceEndpointId: String
    ) {

        if (packet.ttl <= 0) {

            Log.d(
                TAG,
                "TTL reached zero. Not relaying."
            )

            return
        }


        /*
         * Only send to connected devices other than
         * the device that sent this packet to us.
         */

        val relayEndpoints =
            synchronized(connectedEndpoints) {

                connectedEndpoints
                    .filter {
                        it != sourceEndpointId
                    }
            }


        if (relayEndpoints.isEmpty()) {

            Log.d(
                TAG,
                "No other connected relay nodes."
            )

            return
        }


        val json =
            packet.toJson()


        val payload =
            Payload.fromBytes(
                json.toByteArray(
                    Charsets.UTF_8
                )
            )


        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "RELAYING PACKET"
        )

        Log.d(
            TAG,
            "ID: ${packet.id}"
        )

        Log.d(
            TAG,
            "New TTL: ${packet.ttl}"
        )

        Log.d(
            TAG,
            "Relay targets: ${relayEndpoints.size}"
        )

        Log.d(
            TAG,
            "================================"
        )


        connectionsClient
            .sendPayload(
                relayEndpoints,
                payload
            )
            .addOnSuccessListener {

                Log.d(
                    TAG,
                    "PACKET RELAYED: ${packet.id}"
                )

                onRelay(packet)
            }
            .addOnFailureListener { error ->

                Log.e(
                    TAG,
                    "PACKET RELAY FAILED: ${packet.id}",
                    error
                )
            }
    }


    // =========================================================
    // STOP
    // =========================================================

    fun stop() {

        Log.d(
            TAG,
            "Stopping Bridgeline network"
        )


        connectionsClient
            .stopAdvertising()

        connectionsClient
            .stopDiscovery()

        connectionsClient
            .stopAllEndpoints()


        synchronized(connectedEndpoints) {
            connectedEndpoints.clear()
        }

        synchronized(pendingConnections) {
            pendingConnections.clear()
        }

        synchronized(seenPacketIds) {
            seenPacketIds.clear()
        }

        synchronized(relayedPacketIds) {
            relayedPacketIds.clear()
        }
    }
}