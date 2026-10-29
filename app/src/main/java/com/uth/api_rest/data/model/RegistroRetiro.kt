package com.uth.api_rest.data.model

import com.google.gson.annotations.SerializedName

data class RegistroRetiro(
    @SerializedName("ID")
    val id: Int? = null,

    @SerializedName("ID_Producto")
    val idProducto: Int? = null,

    @SerializedName("Producto")
    val productoNombre: String? = null,

    @SerializedName("StockRetirado")
    val stockRetirado: Int = 0,

    @SerializedName("Fecha")
    val fecha: String? = null,

    @SerializedName("Motivo")
    val motivo: String? = null
)
