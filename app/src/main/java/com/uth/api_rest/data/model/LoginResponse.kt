package com.uth.api_rest.data.model

data class LoginResponse(
    val token: String?,
    val usuario: Usuario?,
    val mensaje: String?
)
