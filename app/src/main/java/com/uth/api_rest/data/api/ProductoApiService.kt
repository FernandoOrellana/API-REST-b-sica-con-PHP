package com.uth.api_rest.data.api

import com.uth.api_rest.data.model.ApiResponse
import com.uth.api_rest.data.model.Categoria
import com.uth.api_rest.data.model.Producto
import com.uth.api_rest.data.model.LoginRequest
import com.uth.api_rest.data.model.LoginResponse
import com.uth.api_rest.data.model.RegistroIngreso
import com.uth.api_rest.data.model.RegistroRetiro
import com.uth.api_rest.data.model.RetiroRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ProductoApiService {

    @POST("api/login")
    suspend fun login(@Body login: LoginRequest): Response<ApiResponse<LoginResponse>>

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

    @POST("api/categorias")
    suspend fun crearCategoria(@Body categoria: Map<String, String>): Response<ApiResponse<Unit>>

    @POST("api/retirar")
    suspend fun retirarStock(@Body body: RetiroRequest): Response<ApiResponse<Unit>>

    @GET("api/ingresos")
    suspend fun getIngresos(): Response<ApiResponse<List<RegistroIngreso>>>

    @GET("api/retiros")
    suspend fun getRetiros(): Response<ApiResponse<List<RegistroRetiro>>>
}
