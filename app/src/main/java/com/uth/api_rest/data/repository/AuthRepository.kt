package com.uth.api_rest.data.repository

import android.content.Context
import com.uth.api_rest.data.api.RetrofitClient
import com.uth.api_rest.data.model.LoginRequest
import com.uth.api_rest.data.model.LoginResponse
import com.uth.api_rest.data.model.Resource
import com.uth.api_rest.data.prefs.AuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val apiService get() = RetrofitClient.getApiService(context)

    suspend fun login(usuario: String, password: String): Resource<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(usuario, password))
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    if (body.ok) {
                        Resource.Success(body.data ?: LoginResponse(null, null, null))
                    } else {
                        Resource.Error(body.error ?: "Credenciales incorrectas")
                    }
                } else if (response.code() == 401) {
                    Resource.Error("Usuario o contraseña incorrectos")
                } else {
                    Resource.Error("Error HTTP ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                Resource.Error(
                    "Error de conexión al iniciar sesión: ${e.localizedMessage ?: "No se pudo conectar a la API"}",
                    e
                )
            }
        }

    fun guardarSesion(token: String?, respuesta: LoginResponse?) {
        val tokenVal = token ?: return
        val usuario = respuesta?.usuario ?: return
        AuthSession.guardarSesion(context, tokenVal, usuario)
    }

    fun haySesion(): Boolean = AuthSession.haySesion(context)

    fun obtenerToken(): String? = AuthSession.obtenerToken(context)

    fun limpiarSesion() = AuthSession.limpiarSesion(context)
}
