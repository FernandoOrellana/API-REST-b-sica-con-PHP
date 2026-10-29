package com.uth.api_rest.data.repository

import android.content.Context
import android.util.Log
import com.uth.api_rest.data.api.RetrofitClient
import com.uth.api_rest.data.model.Categoria
import com.uth.api_rest.data.model.Producto
import com.uth.api_rest.data.model.RegistroIngreso
import com.uth.api_rest.data.model.RegistroRetiro
import com.uth.api_rest.data.model.Resource
import com.uth.api_rest.data.model.RetiroRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class ProductoRepository(private val context: Context) {

    private val apiService get() = RetrofitClient.getApiService(context)

    /**
     * Extrae el mensaje de error real que devuelve el servidor PHP.
     * Retrofit guarda el JSON de error en errorBody(), no en body().
     */
    private fun mensajeError(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = JSONObject(errorBody)
                val error = json.optString("error", "")
                if (error.isNotBlank()) {
                    error
                } else {
                    "Error HTTP ${response.code()}: ${response.message()}"
                }
            } else {
                "Error HTTP ${response.code()}: ${response.message()}"
            }
        } catch (e: Exception) {
            "Error HTTP ${response.code()}: ${response.message()}"
        }
    }

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
                Resource.Error(mensajeError(response))
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
                Resource.Error(mensajeError(response))
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
                Resource.Error(mensajeError(response))
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
                Resource.Error(mensajeError(response))
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
                Resource.Error(mensajeError(response))
            }
        } catch (e: Exception) {
            Resource.Error("Error al cargar categorías: ${e.localizedMessage}", e)
        }
    }

    suspend fun crearCategoria(nombreCategoria: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf("Categoria" to nombreCategoria)
            val response = apiService.crearCategoria(payload)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(body.error ?: "Error al guardar categoría")
                }
            } else {
                Resource.Error(mensajeError(response))
            }
        } catch (e: Exception) {
            Resource.Error("Error al crear categoría: ${e.localizedMessage}", e)
        }
    }

    suspend fun retirarStock(idProducto: Int, cantidad: Int, motivo: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = RetiroRequest(
                idProducto = idProducto,
                cantidad = cantidad,
                motivo = motivo,
                idUsuario = 1
            )
            Log.d("RETIRO", "Enviando: ID_Producto=$idProducto, cantidad=$cantidad, motivo=$motivo")
            val response = apiService.retirarStock(payload)
            val body = response.body()
            Log.d("RETIRO", "Respuesta HTTP ${response.code()} body=$body")
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(body.error ?: body.mensaje ?: "Error al realizar el retiro")
                }
            } else {
                Resource.Error(mensajeError(response))
            }
        } catch (e: Exception) {
            Log.e("RETIRO", "Excepción: ${e.localizedMessage}", e)
            Resource.Error("Error de conexión al retirar stock: ${e.localizedMessage}", e)
        }
    }

    suspend fun getIngresos(): Resource<List<RegistroIngreso>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getIngresos()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(body.data ?: emptyList())
                } else {
                    Resource.Error(body.error ?: "Error al obtener registro de ingresos")
                }
            } else {
                Resource.Error(mensajeError(response))
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión al obtener ingresos: ${e.localizedMessage}", e)
        }
    }

    suspend fun getRetiros(): Resource<List<RegistroRetiro>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRetiros()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    Resource.Success(body.data ?: emptyList())
                } else {
                    Resource.Error(body.error ?: "Error al obtener registro de retiros")
                }
            } else {
                Resource.Error(mensajeError(response))
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión al obtener retiros: ${e.localizedMessage}", e)
        }
    }
}
