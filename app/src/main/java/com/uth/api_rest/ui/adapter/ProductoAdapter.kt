package com.uth.api_rest.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uth.api_rest.R
import com.uth.api_rest.data.model.Producto
import com.uth.api_rest.databinding.ItemProductoBinding
import java.util.Locale

class ProductoAdapter(
    private val onEditClick: (Producto) -> Unit,
    private val onDeleteClick: (Producto) -> Unit,
) : ListAdapter<Producto, ProductoAdapter.ProductoViewHolder>(ProductoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductoViewHolder(
        private val binding: ItemProductoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(producto: Producto) {
            val context = binding.root.context

            binding.tvNombre.text = producto.nombre

            if (!producto.categoria.isNullOrBlank()) {
                binding.tvCategoria.text = producto.categoria
                binding.tvCategoria.visibility = View.VISIBLE
            } else {
                binding.tvCategoria.visibility = View.GONE
            }

            if (!producto.descripcion.isNullOrBlank()) {
                binding.tvDescripcion.text = producto.descripcion
                binding.tvDescripcion.visibility = View.VISIBLE
            } else {
                binding.tvDescripcion.visibility = View.GONE
            }

            binding.tvPrecio.text = String.format(Locale.getDefault(), "$%.2f", producto.precioVenta)

            val stockText = if (producto.stockActual > 0) {
                context.getString(R.string.stock_badge, producto.stockActual)
            } else {
                "Sin Stock"
            }
            binding.tvStock.text = stockText

            val threshold = if (producto.stockMinimo > 0) producto.stockMinimo else 10
            val chipColor = when {
                producto.stockActual > threshold -> "#4CAF50".toColorInt()
                (producto.stockActual in 1..threshold) -> "#FF9800".toColorInt()
                else -> "#F44336".toColorInt()
            }

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(chipColor)
            }
            binding.tvStock.background = drawable

            binding.btnEdit.setOnClickListener { onEditClick(producto) }
            binding.btnDelete.setOnClickListener { onDeleteClick(producto) }
        }
    }

    class ProductoDiffCallback : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(oldItem: Producto, newItem: Producto): Boolean {
            return oldItem.obtenerId() == newItem.obtenerId()
        }

        override fun areContentsTheSame(oldItem: Producto, newItem: Producto): Boolean {
            return oldItem == newItem
        }
    }
}
