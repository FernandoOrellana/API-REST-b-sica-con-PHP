package com.uth.api_rest.data.model

import com.google.gson.annotations.SerializedName

data class Categoria(
    @SerializedName("ID_categoria")
    val id: Int,

    @SerializedName("Categoria")
    val nombre: String
)
