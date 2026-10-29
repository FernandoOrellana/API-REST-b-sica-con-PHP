package com.uth.api_rest

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.uth.api_rest.data.api.RetrofitClient
import com.uth.api_rest.data.model.Categoria
import com.uth.api_rest.data.model.Producto
import com.uth.api_rest.data.model.Resource
import com.uth.api_rest.databinding.ActivityMainBinding
import com.uth.api_rest.databinding.DialogConfigUrlBinding
import com.uth.api_rest.databinding.DialogHistorialBinding
import com.uth.api_rest.databinding.DialogNuevaCategoriaBinding
import com.uth.api_rest.databinding.DialogProductoBinding
import com.uth.api_rest.databinding.DialogRetirarBinding
import com.uth.api_rest.ui.ProductoViewModel
import com.uth.api_rest.ui.adapter.HistorialAdapter
import com.uth.api_rest.ui.adapter.HistorialItem
import com.uth.api_rest.ui.adapter.ProductoAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var adapter: ProductoAdapter

    private var listaCategorias: List<Categoria> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupListeners()
        observeViewModel()

        viewModel.cargarProductos()
        viewModel.cargarCategorias()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    viewModel.cargarProductos()
                    viewModel.cargarCategorias()
                    true
                }
                R.id.action_ver_registros -> {
                    mostrarDialogoHistorial()
                    true
                }
                R.id.action_config_url -> {
                    mostrarDialogoConfigUrl()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductoAdapter(
            onEditClick = { producto -> mostrarDialogoProducto(producto) },
            onDeleteClick = { producto -> mostrarDialogoConfirmarEliminar(producto) },
            onWithdrawClick = { producto -> mostrarDialogoRetirarStock(producto) }
        )
        binding.rvProductos.layoutManager = LinearLayoutManager(this)
        binding.rvProductos.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.buscarProductos(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.cargarProductos()
            viewModel.cargarCategorias()
        }

        binding.fabAdd.setOnClickListener {
            mostrarDialogoProducto(null)
        }
    }

    private fun observeViewModel() {
        viewModel.categoriasState.observe(this) { categorias ->
            listaCategorias = categorias
        }

        viewModel.productosState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressIndicator.visibility = View.VISIBLE
                    }
                }
                is Resource.Success -> {
                    binding.progressIndicator.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    val productos = resource.data
                    adapter.submitList(productos)

                    if (productos.isEmpty()) {
                        binding.emptyStateLayout.visibility = View.VISIBLE
                        binding.tvEmptyState.text = if (viewModel.currentFilter.isNotBlank()) {
                            getString(R.string.empty_search)
                        } else {
                            getString(R.string.empty_products)
                        }
                    } else {
                        binding.emptyStateLayout.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.progressIndicator.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG)
                        .setAction("Reintentar") { viewModel.cargarProductos() }
                        .show()
                }
            }
        }

        viewModel.operacionState.observe(this) { resource ->
            if (resource == null) return@observe

            when (resource) {
                is Resource.Loading -> {
                    binding.progressIndicator.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressIndicator.visibility = View.GONE
                    Toast.makeText(this, resource.data, Toast.LENGTH_SHORT).show()
                    viewModel.resetOperacionState()
                }
                is Resource.Error -> {
                    binding.progressIndicator.visibility = View.GONE
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                    viewModel.resetOperacionState()
                }
            }
        }
    }

    private fun mostrarDialogoProducto(productoExistente: Producto?) {
        val dialogBinding = DialogProductoBinding.inflate(LayoutInflater.from(this))

        val nombresCategorias = if (listaCategorias.isNotEmpty()) {
            listaCategorias.map { it.nombre }
        } else {
            listOf("General")
        }

        val catAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresCategorias)
        dialogBinding.actvCategoria.setAdapter(catAdapter)

        if (productoExistente != null) {
            dialogBinding.tvDialogTitle.text = getString(R.string.title_editar_producto)
            dialogBinding.etNombre.setText(productoExistente.nombre)
            dialogBinding.actvCategoria.setText(productoExistente.categoria ?: nombresCategorias.firstOrNull() ?: "", false)
            dialogBinding.etPCompra.setText(productoExistente.precioCompra.toString())
            dialogBinding.etPVenta.setText(productoExistente.precioVenta.toString())
            dialogBinding.etStockActual.setText(productoExistente.stockActual.toString())
            dialogBinding.etStockMinimo.setText(productoExistente.stockMinimo.toString())
            dialogBinding.etDescripcion.setText(productoExistente.descripcion ?: "")
        } else {
            dialogBinding.tvDialogTitle.text = getString(R.string.title_nuevo_producto)
            dialogBinding.actvCategoria.setText(nombresCategorias.firstOrNull() ?: "", false)
        }

        dialogBinding.btnNuevaCategoria.setOnClickListener {
            mostrarDialogoNuevaCategoria { nuevaCatNombre ->
                viewModel.crearCategoria(nuevaCatNombre)
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.btn_guardar), null)
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .create()

        dialog.setOnShowListener {
            val btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnGuardar.setOnClickListener {
                val nombre = dialogBinding.etNombre.text?.toString()?.trim() ?: ""
                val categoriaSeleccionada = dialogBinding.actvCategoria.text?.toString()?.trim() ?: ""
                val pCompraStr = dialogBinding.etPCompra.text?.toString()?.trim() ?: "0"
                val pVentaStr = dialogBinding.etPVenta.text?.toString()?.trim() ?: "0"
                val stockActualStr = dialogBinding.etStockActual.text?.toString()?.trim() ?: "0"
                val stockMinimoStr = dialogBinding.etStockMinimo.text?.toString()?.trim() ?: "0"
                val descripcion = dialogBinding.etDescripcion.text?.toString()?.trim() ?: ""

                var esValido = true

                if (nombre.isEmpty()) {
                    dialogBinding.tilNombre.error = getString(R.string.error_campo_requerido)
                    esValido = false
                } else {
                    dialogBinding.tilNombre.error = null
                }

                if (esValido) {
                    val catEncontrada = listaCategorias.find { it.nombre.equals(categoriaSeleccionada, ignoreCase = true) }
                    val selectedCategoriaId = catEncontrada?.id ?: 1

                    val pCompra = pCompraStr.toDoubleOrNull() ?: 0.0
                    val pVenta = pVentaStr.toDoubleOrNull() ?: 0.0
                    val stockActual = stockActualStr.toIntOrNull() ?: 0
                    val stockMinimo = stockMinimoStr.toIntOrNull() ?: 0

                    val targetId = productoExistente?.obtenerId()

                    val nuevoProducto = Producto(
                        id = targetId,
                        idProducto = targetId,
                        nombre = nombre,
                        idCategoria = selectedCategoriaId,
                        categoria = categoriaSeleccionada,
                        precioCompra = pCompra,
                        precioVenta = pVenta,
                        stockActual = stockActual,
                        stockMinimo = stockMinimo,
                        descripcion = descripcion
                    )

                    if (productoExistente == null) {
                        viewModel.crearProducto(nuevoProducto)
                    } else {
                        viewModel.actualizarProducto(nuevoProducto)
                    }
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun mostrarDialogoNuevaCategoria(onCategoriaCreada: (String) -> Unit) {
        val dialogBinding = DialogNuevaCategoriaBinding.inflate(LayoutInflater.from(this))

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.btn_guardar)) { _, _ ->
                val nombreCat = dialogBinding.etNombreCategoria.text?.toString()?.trim() ?: ""
                if (nombreCat.isNotEmpty()) {
                    onCategoriaCreada(nombreCat)
                }
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun mostrarDialogoRetirarStock(producto: Producto) {
        val dialogBinding = DialogRetirarBinding.inflate(LayoutInflater.from(this))

        dialogBinding.tvInfoProductoStock.text = getString(
            R.string.msg_stock_disponible,
            producto.nombre,
            producto.stockActual
        )

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.btn_retirar), null)
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .create()

        dialog.setOnShowListener {
            val btnRetirar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnRetirar.setOnClickListener {
                val cantidadStr = dialogBinding.etCantidadRetirar.text?.toString()?.trim() ?: ""
                val motivo = dialogBinding.etMotivoRetiro.text?.toString()?.trim() ?: "Salida de Inventario"
                val cantidad = cantidadStr.toIntOrNull() ?: 0

                if (cantidad <= 0) {
                    dialogBinding.tilCantidadRetirar.error = getString(R.string.error_campo_requerido)
                } else if (cantidad > producto.stockActual) {
                    dialogBinding.tilCantidadRetirar.error = getString(
                        R.string.error_stock_insuficiente,
                        producto.stockActual
                    )
                } else {
                    dialogBinding.tilCantidadRetirar.error = null
                    viewModel.retirarStock(producto, cantidad, motivo)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun mostrarDialogoHistorial() {
        val dialogBinding = DialogHistorialBinding.inflate(LayoutInflater.from(this))
        val historialAdapter = HistorialAdapter()

        dialogBinding.rvHistorial.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvHistorial.adapter = historialAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("Cerrar", null)
            .create()

        viewModel.retirosState.observe(this) { resource ->
            if (resource is Resource.Success) {
                val retirosItems = resource.data.map { HistorialItem.Retiro(it) }
                historialAdapter.submitList(retirosItems)
                dialogBinding.tvEmptyHistorial.visibility = if (retirosItems.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.ingresosState.observe(this) { resource ->
            if (resource is Resource.Success) {
                val ingresosItems = resource.data.map { HistorialItem.Ingreso(it) }
                historialAdapter.submitList(ingresosItems)
                dialogBinding.tvEmptyHistorial.visibility = if (ingresosItems.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        dialogBinding.toggleGroupHistorial.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnTabRetiros) {
                    viewModel.cargarRetiros()
                } else if (checkedId == R.id.btnTabIngresos) {
                    viewModel.cargarIngresos()
                }
            }
        }

        viewModel.cargarRetiros()

        dialog.show()
    }

    private fun mostrarDialogoConfirmarEliminar(producto: Producto) {
        val id = producto.obtenerId() ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_confirm_delete))
            .setMessage(getString(R.string.msg_confirm_delete, producto.nombre))
            .setPositiveButton(getString(R.string.btn_eliminar)) { _, _ ->
                viewModel.eliminarProducto(id)
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun mostrarDialogoConfigUrl() {
        val dialogBinding = DialogConfigUrlBinding.inflate(LayoutInflater.from(this))
        val currentUrl = RetrofitClient.getBaseUrl(this)
        dialogBinding.etBaseUrl.setText(currentUrl)

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.btn_guardar)) { _, _ ->
                val newUrl = dialogBinding.etBaseUrl.text?.toString()?.trim() ?: ""
                if (newUrl.isNotEmpty()) {
                    RetrofitClient.setBaseUrl(this, newUrl)
                    Toast.makeText(this, "URL actualizada: ${RetrofitClient.getBaseUrl(this)}", Toast.LENGTH_SHORT).show()
                    viewModel.cargarProductos()
                    viewModel.cargarCategorias()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }
}
