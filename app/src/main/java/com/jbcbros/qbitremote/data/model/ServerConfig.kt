package com.jbcbros.qbitremote.data.model

data class ServerConfig(
    val id: String = "",
    val nickname: String = "",
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val ssl: Boolean = false
) {
    fun baseUrl(): String {
        val scheme = if (ssl) "https" else "http"
        return "$scheme://${host}:${port}"
    }
}
