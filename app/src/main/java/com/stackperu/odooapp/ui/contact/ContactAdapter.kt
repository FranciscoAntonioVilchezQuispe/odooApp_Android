package com.stackperu.odooapp.ui.contact

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stackperu.odooapp.R
import com.stackperu.odooapp.model.Contact

/**
 * Adaptador que gestiona la presentación de los contactos en Malla (Kanban) o Lista vertical.
 */
class ContactAdapter(
    private var contacts: List<Contact>,
    private var isKanbanView: Boolean,
    private val onItemClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    /**
     * Alterna el diseño del item entre tarjeta Kanban y fila de Lista.
     */
    fun setViewMode(isKanban: Boolean) {
        this.isKanbanView = isKanban
        notifyDataSetChanged()
    }

    /**
     * Actualiza la información obtenida desde Odoo.
     */
    fun updateData(newContacts: List<Contact>) {
        this.contacts = newContacts
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (isKanbanView) R.layout.item_contact_kanban else R.layout.item_contact_list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)

        fun bind(contact: Contact) {
            tvName.text = contact.name
            tvEmail.text = contact.email ?: "Sin correo"
            tvPhone.text = contact.phone ?: "Sin teléfono"

            // Odoo devuelve las imágenes codificadas en Base64.
            // Si la imagen existe, la decodificamos y la mostramos con Glide.
            val base64Image = contact.avatarBase64
            if (!base64Image.isNullOrEmpty()) {
                try {
                    val imageByteArray = Base64.decode(base64Image, Base64.DEFAULT)
                    Glide.with(itemView.context)
                        .asBitmap()
                        .load(imageByteArray)
                        .circleCrop()
                        .placeholder(R.drawable.avatar_placeholder)
                        .into(ivAvatar)
                } catch (e: Exception) {
                    // Si falla la decodificación, ponemos el placeholder
                    Glide.with(itemView.context)
                        .load(R.drawable.avatar_placeholder)
                        .circleCrop()
                        .into(ivAvatar)
                }
            } else {
                // Si el contacto no tiene imagen
                Glide.with(itemView.context)
                    .load(R.drawable.avatar_placeholder)
                    .circleCrop()
                    .into(ivAvatar)
            }

            // Detectar clic en la tarjeta
            itemView.setOnClickListener {
                onItemClick(contact)
            }
        }
    }
}