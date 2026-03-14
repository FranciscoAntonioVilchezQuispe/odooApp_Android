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
class AdaptadorContacto(
    private var contacts: List<Contact>,
    private var isKanbanView: Boolean,
    private val onItemClick: (Contact) -> Unit
) : RecyclerView.Adapter<AdaptadorContacto.ContactViewHolder>() {

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
        return if (isKanbanView) R.layout.item_contacto_kanban else R.layout.item_contacto_lista
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
            tvEmail.text = contact.email?.takeIf { it != "false" && it.isNotBlank() } ?: "Sin correo"
            tvPhone.text = contact.phone?.takeIf { it != "false" && it.isNotBlank() } ?: "Sin teléfono"

            // Cargamos la imagen desde la URL de Odoo para mayor estabilidad
            val sessionCookie = com.stackperu.odooapp.api.RetrofitClient.cookieJar.getSessionCookieValue()
            if (sessionCookie.isNotEmpty()) {
                val avatarUrl = "${com.stackperu.odooapp.api.RetrofitClient.BASE_URL}/web/image?model=res.partner&id=${contact.id}&field=image_128"
                Glide.with(itemView.context)
                    .load(com.bumptech.glide.load.model.GlideUrl(avatarUrl, com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("Cookie", "session_id=$sessionCookie")
                        .build()))
                    .circleCrop()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(ivAvatar)
            } else {
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