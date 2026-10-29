package com.uth.api_rest.data.model

import com.google.gson.annotations.SerializedName

data class RetiroRequest(
    @SerializedName("ID_Producto")
    val idProducto: Int,

    @SerializedName("cantidad")
    val cantidad: Int,

    @SerializedName("motivo")
    val motivo: String,

    @SerializedName("ID_Usuario")
    val idUsuario: Int = 1
)
