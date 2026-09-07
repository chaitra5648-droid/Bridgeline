package com.narayani.bridgeline.Setup

class NodeRoleManager {

    private var role: NodeRole? = null

    fun setRole(newRole: NodeRole) {
        role = newRole
    }

    fun getRole(): NodeRole? {
        return role
    }

    fun isMaster(): Boolean {
        return role == NodeRole.MASTER
    }

    fun isRelay(): Boolean {
        return role == NodeRole.RELAY
    }
}