package com.uth.api_rest.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uth.api_rest.data.model.LoginResponse
import com.uth.api_rest.data.model.Resource
import com.uth.api_rest.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    private val _loginState = MutableLiveData<Resource<LoginResponse>?>()
    val loginState: LiveData<Resource<LoginResponse>?> = _loginState

    fun login(usuario: String, password: String) {
        _loginState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.login(usuario, password)
            _loginState.value = result
            if (result is Resource.Success) {
                repository.guardarSesion(result.data?.token, result.data)
            }
        }
    }

    fun haySesion(): Boolean = repository.haySesion()

    fun cerrarSesion() = repository.limpiarSesion()
}
