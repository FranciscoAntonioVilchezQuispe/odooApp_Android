package com.stackperu.odooapp.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import kotlin.random.Random

// --- ESTRUCTURAS JSON-RPC PARA ODOO 19 ---

data class OdooRequest<T>(
    val jsonrpc: String = "2.0",
    val id: Int = Random.nextInt(1, 10000),
    val params: T
)

data class CallKwParams(
    val model: String,
    val method: String,
    val args: List<Any>, 
    val kwargs: Kwargs = Kwargs()
)

data class Kwargs(
    val fields: List<String>? = null,
    val offset: Int? = null,
    val limit: Int? = null,
    val order: String? = null,
    val context: Map<String, Any>? = null
)

data class OdooResponse<T>(
    val jsonrpc: String?,
    val result: T?,
    val error: OdooError?
)

data class OdooError(
    val code: Int,
    val message: String,
    val data: ErrorData?
)

data class ErrorData(
    val name: String,
    val debug: String,
    val message: String
)

data class AuthParams(
    val db: String,
    val login: String,
    val password: String
)

data class AuthResult(
    val uid: Int,
    val session_id: String,
    val name: String,
    val username: String
)

// --- MODELOS DE DOMINIO DE LA APLICACIÓN ---

data class User(
    val id: Int,
    val name: String,
    var avatarUrl: String,
    val email: String
) : Serializable

data class Contact(
    val id: Int,
    val name: String,
    val email: String?,
    val phone: String?,
    val vat: String?,
    val street: String?,
    val state_id: Any?,
    val city_id: Any?,
    val l10n_pe_district: Any?,
    val l10n_latam_identification_type_id: Any?,
    val image_128: Any?,
    val customer_rank: Int? = 0,
    val supplier_rank: Int? = 0
) : Serializable {
    val avatarBase64: String?
        get() = if (image_128 is String) image_128 else null
}

data class State(
    val id: Int,
    val name: String,
    val code: String? = null
) : Serializable

data class City(
    val id: Int,
    val name: String,
    val state_id: Int? = null
) : Serializable

data class District(
    val id: Int,
    val name: String,
    val city_id: Int? = null
) : Serializable

data class DetractionType(
    val id: Int,
    val name: String,
    val code: String? = null,
    val percentage: Double? = 0.0,
    val amount: Double? = 0.0,
    val service_amount: Double? = 0.0
) : Serializable {
    // Retorna el primer valor numérico encontrado entre los campos posibles
    val effectivePercentage: Double
        get() = (percentage ?: 0.0).takeIf { it > 0 } 
            ?: (amount ?: 0.0).takeIf { it > 0 } 
            ?: (service_amount ?: 0.0)
}

data class Product(
    val id: Int,
    val name: String,
    val lst_price: Double?,
    val type: String?,
    val qty_available: Double? = 0.0,
    val default_code: String? = null,
    val property_account_income_id: Any? = null,
    val property_account_expense_id: Any? = null
) : Serializable

data class Currency(
    val id: Int,
    val name: String,
    val symbol: String? = null
) : Serializable

data class Account(
    val id: Int,
    val name: String,
    val code: String? = null
) : Serializable

data class Journal(
    val id: Int,
    val name: String,
    val code: String, // Serie ej: F001
    val type: String
) : Serializable

data class PaymentTerm(
    val id: Int,
    val name: String,
    val line_ids: List<Int>? = null
) : Serializable

data class AnalyticAccount(
    val id: Int,
    val name: String
) : Serializable

data class Uom(
    val id: Int,
    val name: String
) : Serializable

data class Tax(
    val id: Int,
    val name: String,
    val amount: Double? = 0.0
) : Serializable

/**
 * Representa una línea de factura en el formulario (Estado local antes de enviar a Odoo).
 */
data class InvoiceLine(
    var product: Product? = null,
    var account: Account? = null,
    var analyticAccount: AnalyticAccount? = null,
    var quantity: Double = 1.0,
    var uom: Uom? = null,
    var priceUnit: Double = 0.0,
    var taxes: List<Tax> = emptyList(),
    var priceSubtotal: Double = 0.0
) : Serializable
