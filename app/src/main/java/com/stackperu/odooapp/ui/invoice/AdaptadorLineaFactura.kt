package com.stackperu.odooapp.ui.invoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stackperu.odooapp.databinding.ItemLineaFacturaBinding
import com.stackperu.odooapp.model.InvoiceLine

class AdaptadorLineaFactura(
    private val lines: MutableList<InvoiceLine>,
    private val onLineRemoved: (Int) -> Unit
) : RecyclerView.Adapter<AdaptadorLineaFactura.ViewHolder>() {

    class ViewHolder(val binding: ItemLineaFacturaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLineaFacturaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = lines[position]
        holder.binding.tvProductTitle.text = line.product?.name ?: "Producto sin nombre"
        holder.binding.tvQtyPrice.text = String.format("%.2f x S/ %.2f", line.quantity, line.priceUnit)
        
        // Mostramos el total de la línea (cantidad * precio unitario)
        holder.binding.tvLineSubtotal.text = String.format("S/ %.2f", line.quantity * line.priceUnit)
        
        val taxText = if (line.taxes.isNotEmpty()) {
            line.taxes.joinToString { it.name }
        } else {
            "Sin impuestos"
        }
        holder.binding.tvTax.text = taxText

        holder.itemView.setOnLongClickListener {
            onLineRemoved(position)
            true
        }
    }

    override fun getItemCount() = lines.size

    fun updateData(newLines: List<InvoiceLine>) {
        lines.clear()
        lines.addAll(newLines)
        notifyDataSetChanged()
    }
}
