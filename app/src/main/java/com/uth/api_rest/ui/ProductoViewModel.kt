package com.uth.api_rest.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uth.api_rest.data.model.Categoria
import com.uth.api_rest.data.model.Producto
import com.uth.api_rest.data.model.RegistroIngreso
import com.uth.api_rest.data.model.RegistroRetiro
import com.uth.api_rest.data.model.Resource
import com.uth.api_rest.data.repository.ProductoRepository
import kotlinx.coroutines.launch

class ProductoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProductoRepository(application)

    private val _productosState = MutableLiveData<Resource<List<Producto>>>()
    val productosState: LiveData<Resource<List<Producto>>> = _productosState

    private val _operacionState = MutableLiveData<Resource<String>?>()
    val operacionState: LiveData<Resource<String>?> = _operacionState

    private val _categoriasState = MutableLiveData<List<Categoria>>()
    val categoriasState: LiveData<List<Categoria>> = _categoriasState

    private val _ingresosState = MutableLiveData<Resource<List<RegistroIngreso>>>()
    val ingresosState: LiveData<Resource<List<RegistroIngreso>>> = _ingresosState

    private val _retirosState = MutableLiveData<Resource<List<RegistroRetiro>>>()
    val retirosState: LiveData<Resource<List<RegistroRetiro>>> = _retirosState

    private var listaCompletaProductos: List<Producto> = emptyList()
    var currentFilter: String = ""
        private set

    init {
        cargarCategorias()
    }

    fun cargarProductos() {
        _productosState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getProductos()
            if (result is Resource.Success) {
                listaCompletaProductos = result.data
                aplicarFiltro(currentFilter)
            } else {
                _productosState.value = result
            }
        }
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            val result = repository.getCategorias()
            if (result is Resource.Success) {
                _categoriasState.value = result.data
            }
        }
    }

    fun crearCategoria(nombreCategoria: String) {
        if (nombreCategoria.isBlank()) {
            _operacionState.value = Resource.Error("El nombre de la categoría no puede estar vacío")
            return
        }
        _operacionState.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.crearCategoria(nombreCategoria)) {
                is Resource.Success -> {
                    _operacionState.value = Resource.Success("Categoría '$nombreCategoria' creada exitosamente")
                    cargarCategorias()
                }
                is Resource.Error -> {
                    _operacionState.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun buscarProductos(query: String) {
        currentFilter = query
        aplicarFiltro(query)
    }

    private fun aplicarFiltro(query: String) {
        if (query.isBlank()) {
            _productosState.value = Resource.Success(listaCompletaProductos)
        } else {
            val filtrados = listaCompletaProductos.filter { p ->
                p.nombre.contains(query, ignoreCase = true) ||
                        (p.categoria?.contains(query, ignoreCase = true) == true) ||
                        (p.descripcion?.contains(query, ignoreCase = true) == true)
            }
            _productosState.value = Resource.Success(filtrados)
        }
    }

    fun crearProducto(producto: Producto) {
        _operacionState.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.crearProducto(producto)) {
                is Resource.Success -> {
                    _operacionState.value = Resource.Success("Producto registrado exitosamente")
                    cargarProductos()
                }
                is Resource.Error -> {
                    _operacionState.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun actualizarProducto(producto: Producto) {
        _operacionState.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.actualizarProducto(producto)) {
                is Resource.Success -> {
                    _operacionState.value = Resource.Success("Producto actualizado exitosamente")
                    cargarProductos()
                }
                is Resource.Error -> {
                    _operacionState.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun retirarStock(producto: Producto, cantidadARetirar: Int, motivo: String = "Salida de Inventario") {
        if (cantidadARetirar <= 0) {
            _operacionState.value = Resource.Error("La cantidad a retirar debe ser mayor a cero")
            return
        }
        if (cantidadARetirar > producto.stockActual) {
            _operacionState.value = Resource.Error("Stock insuficiente para realizar el retiro (Disponible: ${producto.stockActual})")
            return
        }

        val idProd = producto.obtenerId() ?: return

        _operacionState.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.retirarStock(idProd, cantidadARetirar, motivo)) {
                is Resource.Success -> {
                    _operacionState.value = Resource.Success("Se retiraron $cantidadARetirar unidades de ${producto.nombre}")
                    cargarProductos()
                }
                is Resource.Error -> {
                    _operacionState.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun cargarIngresos() {
        _ingresosState.value = Resource.Loading
        viewModelScope.launch {
            _ingresosState.value = repository.getIngresos()
        }
    }

    fun cargarRetiros() {
        _retirosState.value = Resource.Loading
        viewModelScope.launch {
            _retirosState.value = repository.getRetiros()
        }
    }

    fun eliminarProducto(id: Int) {
        _operacionState.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.eliminarProducto(id)) {
                is Resource.Success -> {
                    _operacionState.value = Resource.Success("Producto eliminado exitosamente")
                    cargarProductos()
                }
                is Resource.Error -> {
                    _operacionState.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetOperacionState() {
        _operacionState.value = null
    }
}
