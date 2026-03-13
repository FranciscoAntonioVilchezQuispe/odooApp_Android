package com.stackperu.odooapp.data

import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.model.*
import android.util.Log

/**
 * Repositorio para gestionar Productos y sus catálogos asociados (Cuentas, UdM, Impuestos).
 */
object ProductRepository {

    suspend fun buscarProductos(query: String, limit: Int = 15): List<Product> {
        return try {
            val domain = if (query.isNotEmpty()) {
                listOf("|", "|", 
                    listOf("name", "ilike", query), 
                    listOf("default_code", "ilike", query),
                    listOf("barcode", "ilike", query))
            } else {
                emptyList<Any>()
            }

            val params = CallKwParams(
                model = "product.product",
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf(
                        "id", "name", "lst_price", "type", "qty_available", "default_code",
                        "property_account_income_id", "property_account_expense_id"
                    ),
                    limit = limit,
                    context = mapOf("lang" to "es_PE")
                )
            )

            val response = RetrofitClient.apiService.executeKwListProduct(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error buscando productos", e)
            emptyList()
        }
    }

    suspend fun obtenerCuentas(query: String = ""): List<Account> {
        return try {
            val domain = mutableListOf<Any>()
            if (query.isNotEmpty()) {
                domain.add("|")
                domain.add(listOf("name", "ilike", query))
                domain.add(listOf("code", "ilike", query))
            }
            // Filtrar solo cuentas que se pueden usar en asientos (deprecated=false)
            // Odoo 19: deprecated es común
            domain.add(listOf("deprecated", "=", false))
            if (query.isNotEmpty()) {
                domain.add(0, "&")
            }

            val params = CallKwParams(
                model = "account.account",
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "code"),
                    limit = 50,
                    context = mapOf("lang" to "es_PE")
                )
            )

            val response = RetrofitClient.apiService.executeKwListAccount(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo cuentas", e)
            emptyList()
        }
    }

    suspend fun obtenerImpuestos(type: String = "sale"): List<Tax> {
        return try {
            val domain = listOf(listOf("type_tax_use", "=", type), listOf("active", "=", true))
            val params = CallKwParams(
                model = "account.tax",
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "amount"),
                    limit = 10,
                    context = mapOf("lang" to "es_PE")
                )
            )
            val response = RetrofitClient.apiService.executeKwListTax(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo impuestos", e)
            emptyList()
        }
    }

    suspend fun obtenerUdMs(): List<Uom> {
        return try {
            val params = CallKwParams(
                model = "uom.uom",
                method = "search_read",
                args = emptyList(),
                kwargs = Kwargs(
                    fields = listOf("id", "name"),
                    limit = 50,
                    context = mapOf("lang" to "es_PE")
                )
            )
            val response = RetrofitClient.apiService.executeKwListUom(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo UdMs", e)
            emptyList()
        }
    }

    suspend fun buscarCuentaPorCodigo(codigo: String): Account? {
        return try {
            val domain = listOf(listOf("code", "=", codigo), listOf("deprecated", "=", false))
            val params = CallKwParams(
                model = "account.account",
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "code"),
                    limit = 1,
                    context = mapOf("lang" to "es_PE")
                )
            )
            val response = RetrofitClient.apiService.executeKwListAccount(OdooRequest(params = params))
            response.body()?.result?.firstOrNull()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error buscando cuenta por código", e)
            null
        }
    }

    suspend fun obtenerDiarios(type: String = "sale"): List<Journal> {
        return try {
            val domain = listOf(listOf("type", "=", type), listOf("active", "=", true))
            val params = CallKwParams(
                model = "account.journal",
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "code", "type"),
                    context = mapOf("lang" to "es_PE")
                )
            )
            val response = RetrofitClient.apiService.executeKwListJournal(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo diarios", e)
            emptyList()
        }
    }

    suspend fun obtenerPlazosPago(): List<PaymentTerm> {
        return try {
            val params = CallKwParams(
                model = "account.payment.term",
                method = "search_read",
                args = emptyList(),
                kwargs = Kwargs(
                    fields = listOf("id", "name"),
                    context = mapOf("lang" to "es_PE")
                )
            )
            val response =
                RetrofitClient.apiService.executeKwListPaymentTerm(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo plazos pago", e)
            emptyList()
        }
    }
    suspend fun crearFactura(params: Map<String, Any>): Int? {
        return try {
            val callParams = CallKwParams(
                model = "account.move",
                method = "create",
                args = listOf(params)
            )
            val response = RetrofitClient.apiService.executeKw(OdooRequest(params = callParams))
            if (response.isSuccessful) {
                // Odoo devuelve el ID del registro creado como Double o Int en el campo 'result'
                val result = response.body()?.result
                (result as? Number)?.toInt() ?: (result as? List<*>)?.firstOrNull()?.let { (it as? Number)?.toInt() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("OdooApp", "Error creando factura en Odoo", e)
            null
        }
    }
}
