package com.stackperu.odooapp.data

import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Contact
import com.stackperu.odooapp.model.Kwargs
import com.stackperu.odooapp.model.OdooRequest
import retrofit2.Response

/**
 * Repositorio para centralizar las operaciones de Contactos (res.partner) en Odoo.
 */
object ContactRepository {

    /**
     * Realiza una búsqueda de contactos en Odoo con filtros de rango y búsqueda por texto.
     * Usa la notación de prefijo plana para mayor compatibilidad.
     */
    suspend fun buscarContactos(
        query: String,
        rankField: String? = null, // "customer_rank" o "supplier_rank"
        limit: Int = 15,
        globalSearch: Boolean = false
    ): List<Contact> {
        return try {
            val domain = mutableListOf<Any>()
            
            if (!rankField.isNullOrEmpty() && !globalSearch) {
                // Filtro normal por cliente/proveedor
                if (query.isNotEmpty()) {
                    domain.add("&")
                    domain.add(listOf(rankField, ">", 0))
                    domain.add("|")
                    domain.add("|")
                    domain.add("|")
                    domain.add(listOf("name", "ilike", query))
                    domain.add(listOf("email", "ilike", query))
                    domain.add(listOf("vat", "ilike", query))
                    domain.add(listOf("phone", "ilike", query))
                } else {
                    domain.add(listOf(rankField, ">", 0))
                }
            } else if (query.isNotEmpty()) {
                // Búsqueda general (global)
                domain.add("|")
                domain.add("|")
                domain.add("|")
                domain.add(listOf("name", "ilike", query))
                domain.add(listOf("email", "ilike", query))
                domain.add(listOf("vat", "ilike", query))
                domain.add(listOf("phone", "ilike", query))
            }

            val params = CallKwParams(
                model = "res.partner",
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf(
                        "id", "name", "email", "phone", "vat", "street", 
                        "l10n_latam_identification_type_id", "customer_rank", "supplier_rank"
                    ),
                    limit = limit,
                    order = if (globalSearch && !rankField.isNullOrEmpty()) "$rankField DESC, name ASC" else "name ASC"
                )
            )

            val response = RetrofitClient.apiService.executeKwList(OdooRequest(params = params))
            if (response.isSuccessful && response.body()?.result != null) {
                response.body()?.result!!
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("OdooApp", "Error en ContactRepository.buscarContactos", e)
            emptyList()
        }
    }
}
