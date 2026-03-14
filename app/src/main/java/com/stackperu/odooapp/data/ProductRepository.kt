package com.stackperu.odooapp.data

import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.model.*
import android.util.Log
import com.stackperu.odooapp.AppConfig

/**
 * Repositorio para gestionar Productos y sus catálogos asociados (Cuentas, UdM, Impuestos).
 */
object ProductRepository {

    private val defaultContext = mapOf("lang" to AppConfig.DEFAULT_LANGUAGE_CODE)

    suspend fun buscarProductos(query: String, limit: Int = AppConfig.SEARCH_RESULTS_LIMIT): List<Product> {
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
                model = AppConfig.MODEL_PRODUCT,
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf(
                        "id", "name", "lst_price", "type", "qty_available", "default_code",
                        "property_account_income_id", "property_account_expense_id"
                    ),
                    limit = limit,
                    context = defaultContext
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
            domain.add(listOf("deprecated", "=", false))
            if (query.isNotEmpty()) {
                domain.add(0, "&")
            }

            val params = CallKwParams(
                model = AppConfig.MODEL_ACCOUNT,
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "code"),
                    limit = AppConfig.DEFAULT_PAGE_LIMIT,
                    context = defaultContext
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
                model = AppConfig.MODEL_TAX,
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "amount"),
                    limit = 15,
                    context = defaultContext
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
                model = AppConfig.MODEL_UOM,
                method = "search_read",
                args = emptyList(),
                kwargs = Kwargs(
                    fields = listOf("id", "name"),
                    limit = AppConfig.DEFAULT_PAGE_LIMIT,
                    context = defaultContext
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
                model = AppConfig.MODEL_ACCOUNT,
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "code"),
                    limit = 1,
                    context = defaultContext
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
                model = AppConfig.MODEL_JOURNAL,
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "code", "type"),
                    context = defaultContext
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
                model = AppConfig.MODEL_PAYMENT_TERM,
                method = "search_read",
                args = emptyList(),
                kwargs = Kwargs(
                    fields = listOf("id", "name"),
                    context = defaultContext
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
                model = AppConfig.MODEL_INVOICE,
                method = "create",
                args = listOf(params)
            )
            val response = RetrofitClient.apiService.executeKw(OdooRequest(params = callParams))
            if (response.isSuccessful) {
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

    suspend fun obtenerRegiones(): List<State> {
        return try {
            val domain = listOf(listOf("country_id.code", "=", AppConfig.DEFAULT_COUNTRY_CODE))
            val params = CallKwParams(
                model = AppConfig.MODEL_STATE,
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(fields = listOf("id", "name", "code"), order = "name ASC")
            )
            val response = RetrofitClient.apiService.executeKwListState(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo regiones", e)
            emptyList()
        }
    }

    suspend fun obtenerProvincias(stateId: Int): List<City> {
        return try {
            val domain = listOf(listOf("state_id", "=", stateId))
            val params = CallKwParams(
                model = AppConfig.MODEL_CITY,
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(fields = listOf("id", "name", "state_id"), order = "name ASC")
            )
            val response = RetrofitClient.apiService.executeKwListCity(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo provincias", e)
            emptyList()
        }
    }

    suspend fun obtenerDistritos(cityId: Int): List<District> {
        return try {
            val domain = listOf(listOf("city_id", "=", cityId))
            val params = CallKwParams(
                model = AppConfig.MODEL_DISTRICT,
                method = "search_read",
                args = listOf(domain),
                kwargs = Kwargs(fields = listOf("id", "name", "city_id"), order = "name ASC")
            )
            val response = RetrofitClient.apiService.executeKwListDistrict(OdooRequest(params = params))
            response.body()?.result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo distritos", e)
            emptyList()
        }
    }

    suspend fun obtenerTiposDetraccion(): List<DetractionType> {
        return try {
            val result = llamarObtenerDetracciones(AppConfig.MODEL_DETRACTION)
            if (result.isNotEmpty()) return result

            llamarObtenerDetracciones("l10n_pe.detraction.type")
        } catch (e: Exception) {
            Log.e("OdooApp", "Error obteniendo tipos detracción", e)
            emptyList()
        }
    }

    private suspend fun llamarObtenerDetracciones(modelName: String): List<DetractionType> {
        return try {
            Log.d("OdooApp", "Consultando detracciones en modelo: $modelName")
            val params = CallKwParams(
                model = modelName,
                method = "search_read",
                args = listOf(emptyList<Any>()),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "code", "percentage"),
                    order = "code ASC",
                    context = defaultContext
                )
            )
            val response = RetrofitClient.apiService.executeKwListDetractionType(OdooRequest(params = params))
            val list = response.body()?.result ?: emptyList()
            Log.d("OdooApp", "Modelo $modelName devolvió ${list.size} registros")
            list
        } catch (e: Exception) {
            Log.w("OdooApp", "Fallo consulta en modelo $modelName: ${e.message}")
            emptyList()
        }
    }
}
