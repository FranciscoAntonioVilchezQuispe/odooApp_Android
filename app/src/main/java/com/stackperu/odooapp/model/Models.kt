package com.stackperu.odooapp.model

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
    val address: String?,
    val image_128: Any? 
) : Serializable {
    val avatarBase64: String?
        get() = if (image_128 is String) image_128 else null
}
