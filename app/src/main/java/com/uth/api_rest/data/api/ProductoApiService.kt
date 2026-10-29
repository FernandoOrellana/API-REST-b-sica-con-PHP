package com.uth.api_rest.data.api

import com.uth.api_rest.data.model.ApiResponse
import com.uth.api_rest.data.model.Categoria
import com.uth.api_rest.data.model.Producto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ProductoApiService {

    @GET("api/productos")
    suspend fun getProductos(): Response<ApiResponse<List<Producto>>>

    @GET("api/productos")
    suspend fun getProductoById(@Query("id") id: Int): Response<ApiResponse<List<Producto>>>

    @POST("api/productos")
    suspend fun crearProducto(@Body producto: Producto): Response<ApiResponse<Unit>>

    @PUT("api/productos")
    suspend fun actualizarProducto(@Body producto: Producto): Response<ApiResponse<Unit>>

    @DELETE("api/productos")
    suspend fun eliminarProducto(@Query("id") id: Int): Response<ApiResponse<Unit>>

    @GET("api/categorias")
    suspend fun getCategorias(): Response<ApiResponse<List<Categoria>>>
}
