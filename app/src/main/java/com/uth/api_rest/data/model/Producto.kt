package com.uth.api_rest.data.model

import com.google.gson.annotations.SerializedName

data class Producto(
    @SerializedName("ID")
    val id: Int? = null,

    @SerializedName("ID_Producto")
    val idProducto: Int? = null,

    @SerializedName("Producto")
    val nombre: String = "",

    @SerializedName("ID_Categoria")
    val idCategoria: Int = 1,

    @SerializedName("Categoria")
    val categoria: String? = null,

    @SerializedName("PCompra")
    val precioCompra: Double = 0.0,

    @SerializedName("PVenta")
    val precioVenta: Double = 0.0,

    @SerializedName("StockActual")
    val stockActual: Int = 0,

    @SerializedName("StockMinimo")
    val stockMinimo: Int = 0,

    @SerializedName("Descripcion")
    val descripcion: String? = null,

    @SerializedName("NombreImagen")
    val nombreImagen: String? = null,

    @SerializedName("FeUltim")
    val feUltim: String? = null,

    @SerializedName("Activo")
    val activo: Int = 1,

    @SerializedName("MostarEmpleado")
    val mostrarEmpleado: Int = 1
) {
    fun obtenerId(): Int? = id ?: idProducto
}
