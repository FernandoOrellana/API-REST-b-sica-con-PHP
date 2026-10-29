package com.uth.api_rest.data.repository

import android.content.Context
import com.uth.api_rest.data.api.RetrofitClient
import com.uth.api_rest.data.model.Categoria
import com.uth.api_rest.data.model.Producto
import com.uth.api_rest.data.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductoRepository(private val context: Context) {

    private val apiService get() = RetrofitClient.getApiService(context)

    suspend fun getProductos(): Resource<List<Producto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProductos()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(body.data ?: emptyList())
                } else {
                    Resource.Error(body.error ?: "Error al obtener productos")
                }
            } else {
                Resource.Error("Error HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.localizedMessage ?: "No se pudo conectar a la API"}", e)
        }
    }

    suspend fun crearProducto(producto: Producto): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.crearProducto(producto)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(body.error ?: "Error al guardar producto")
                }
            } else {
                Resource.Error("Error HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión al crear producto: ${e.localizedMessage}", e)
        }
    }

    suspend fun actualizarProducto(producto: Producto): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.actualizarProducto(producto)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(body.error ?: "Error al actualizar producto")
                }
            } else {
                Resource.Error("Error HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión al actualizar producto: ${e.localizedMessage}", e)
        }
    }

    suspend fun eliminarProducto(id: Int): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.eliminarProducto(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(body.error ?: "Error al eliminar producto")
                }
            } else {
                Resource.Error("Error HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión al eliminar producto: ${e.localizedMessage}", e)
        }
    }

    suspend fun getCategorias(): Resource<List<Categoria>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCategorias()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(body.data ?: emptyList())
                } else {
                    Resource.Error(body.error ?: "Error al obtener categorías")
                }
            } else {
                Resource.Error("Error HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error al cargar categorías: ${e.localizedMessage}", e)
        }
    }
}
