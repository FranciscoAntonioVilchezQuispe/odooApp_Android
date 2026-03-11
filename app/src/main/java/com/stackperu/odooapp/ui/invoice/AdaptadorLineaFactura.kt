package com.stackperu.odooapp.ui.invoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stackperu.odooapp.databinding.ItemLineaFacturaBinding
import com.stackperu.odooapp.model.invoice.InvoiceLine

class AdaptadorLineaFactura(private val lines: List<InvoiceLine>) :
    RecyclerView.Adapter<AdaptadorLineaFactura.ViewHolder>() {

    class ViewHolder(val binding: ItemLineaFacturaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLineaFacturaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = lines[position]
        holder.binding.tvProductTitle.text = line.productTitle
        holder.binding.tvQtyPrice.text = String.format("%.2f x S/ %.2f", line.quantity, line.priceUnit)
        holder.binding.tvLineSubtotal.text = String.format("S/ %.2f (inc. IGV)", line.total)
        holder.binding.tvTax.text = line.taxDescription
    }

    override fun getItemCount() = lines.size
}
