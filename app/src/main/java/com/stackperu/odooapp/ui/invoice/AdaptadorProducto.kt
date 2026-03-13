package com.stackperu.odooapp.ui.invoice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stackperu.odooapp.R
import com.stackperu.odooapp.model.Product
import com.stackperu.odooapp.utils.FormatterHelper

/**
 * Adaptador para visualizar productos en una lista simple.
 */
class AdaptadorProducto(
    private var products: List<Product>,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<AdaptadorProducto.ProductoViewHolder>() {

    class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(android.R.id.text1)
        val tvDetails: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val p = products[position]
        holder.tvName.text = p.name
        val precio = FormatterHelper.formatearMoneda(p.lst_price ?: 0.0)
        val stock = if(p.type == "product") " | Stock: ${p.qty_available}" else ""
        holder.tvDetails.text = "Precio: $precio$stock"
        
        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = products.size

    fun updateData(newData: List<Product>) {
        products = newData
        notifyDataSetChanged()
    }
}
