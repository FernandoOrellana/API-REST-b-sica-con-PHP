package com.uth.api_rest.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("ok")
    val ok: Boolean = false,

    @SerializedName("total")
    val total: Int? = null,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("mensaje")
    val mensaje: String? = null
)
