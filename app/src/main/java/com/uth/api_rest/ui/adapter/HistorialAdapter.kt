package com.uth.api_rest.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.uth.api_rest.data.model.RegistroIngreso
import com.uth.api_rest.data.model.RegistroRetiro
import com.uth.api_rest.databinding.ItemRegistroBinding

sealed class HistorialItem {
    data class Ingreso(val item: RegistroIngreso) : HistorialItem()
    data class Retiro(val item: RegistroRetiro) : HistorialItem()
}

class HistorialAdapter : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    private val items = mutableListOf<HistorialItem>()

    fun submitList(list: List<HistorialItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = ItemRegistroBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HistorialViewHolder(
        private val binding: ItemRegistroBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistorialItem) {
            when (item) {
                is HistorialItem.Ingreso -> {
                    val ingreso = item.item
                    binding.tvProductoRegistro.text = ingreso.productoNombre ?: "Producto #${ingreso.idProducto}"
                    binding.tvCantidadRegistro.text = "+${ingreso.stockIngresado}"
                    binding.tvCantidadRegistro.setTextColor("#4CAF50".toColorInt())
                    binding.tvMotivoRegistro.text = "Motivo: ${ingreso.motivo ?: "Ingreso de Inventario"}"
                    binding.tvFechaRegistro.text = "Fecha: ${ingreso.fecha ?: "-"}"
                }
                is HistorialItem.Retiro -> {
                    val retiro = item.item
                    binding.tvProductoRegistro.text = retiro.productoNombre ?: "Producto #${retiro.idProducto}"
                    binding.tvCantidadRegistro.text = "-${retiro.stockRetirado}"
                    binding.tvCantidadRegistro.setTextColor("#F44336".toColorInt())
                    binding.tvMotivoRegistro.text = "Motivo: ${retiro.motivo ?: "Salida de Inventario"}"
                    binding.tvFechaRegistro.text = "Fecha: ${retiro.fecha ?: "-"}"
                }
            }
        }
    }
}
