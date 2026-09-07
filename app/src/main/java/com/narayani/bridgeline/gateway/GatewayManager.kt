package com.narayani.bridgeline.gateway

import android.util.Log
import com.narayani.bridgeline.Setup.NodeRoleManager
import com.narayani.bridgeline.model.Message
import com.narayani.bridgeline.model.MessageType

class GatewayManager(
    private val nodeRoleManager: NodeRoleManager
) {

    fun receiveMessage(message: Message): GatewayResult {

        return when (message.type) {

            MessageType.CRITICAL -> {

                Log.d(
                    "BRIDGELINE",
                    "Critical Emergency received"
                )

                // Critical emergency is shown on every phone
                GatewayResult.ShowCriticalEmergency(message)
            }

            MessageType.DETAILED -> {

                if (nodeRoleManager.isMaster()) {

                    Log.d(
                        "BRIDGELINE",
                        "Master Node: Detailed SOS received"
                    )

                    GatewayResult.ShowMessage(message)

                } else {

                    Log.d(
                        "BRIDGELINE",
                        "Relay Node: Forwarding detailed message"
                    )

                    GatewayResult.ForwardMessage(message)
                }
            }
        }
    }
}

sealed class GatewayResult {

    data class ShowMessage(
        val message: Message
    ) : GatewayResult()

    data class ShowCriticalEmergency(
        val message: Message
    ) : GatewayResult()

    data class ForwardMessage(
        val message: Message
    ) : GatewayResult()
}