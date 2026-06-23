package com.jbcbros.qbitremote.data.model

data class LoginResult(
    val ok: Boolean,
    val status: Int = 0,
    val body: String = "",
    val error: String? = null
)
