package com.stackperu.odooapp.model

import java.io.Serializable

// --- ESTRUCTURAS JSON-RPC PARA ODOO 19 ---

/**
 * Envoltorio general para cualquier petición hacia Odoo JSON-RPC.
 */
data class OdooRequest<T>(
    val jsonrpc: String = "2.0",
    val params: T
)

/**
 * Parámetros de autenticación requeridos por /web/session/authenticate.
 */
data class AuthParams(
    val db: String,
    val login: String,
    val password: String
)

/**
 * Parámetros requeridos para ejecutar métodos del ORM en /web/dataset/call_kw.
 * Por ejemplo: env['res.partner'].search_read(domain, fields)
 */
data class CallKwParams(
    val model: String,
    val method: String,
    val args: List<Any> = emptyList(), // Generalmente contiene el dominio/filtros
    val kwargs: Kwargs = Kwargs()
)

/**
 * Argumentos nombrados requeridos por Odoo (como fields, offset, limit).
 */
data class Kwargs(
    val fields: List<String> = emptyList(),
    val offset: Int = 0,
    val limit: Int = 100,
    val order: String = "id DESC"
)

/**
 * Envoltorio general para capturar la respuesta de Odoo.
 */
data class OdooResponse<T>(
    val jsonrpc: String?,
    val result: T?,
    val error: OdooError?
)

/**
 * Captura errores arrojados por el servidor de Odoo (por ejemplo, Acceso Denegado).
 */
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

/**
 * Datos del usuario que Odoo devuelve tras un login exitoso.
 */
data class AuthResult(
    val uid: Int,
    val session_id: String,
    val name: String,
    val username: String
)

// --- MODELOS DE DOMINIO DE LA APLICACIÓN ---

/**
 * Modelo que representa al Usuario actual logueado.
 */
data class User(
    val id: Int,
    val name: String,
    val avatarUrl: String,
    val email: String
) : Serializable

/**
 * Modelo que representa un Contacto (res.partner) obtenido de Odoo.
 */
data class Contact(
    val id: Int,
    val name: String,
    val email: String?,
    val phone: String?,
    val vat: String?,
    val address: String?,
    // Odoo puede devolver el campo imagen como falso (booleano) si está vacío o String si tiene Base64.
    // Por eso usamos Any? y luego lo validamos con la propiedad calculada.
    val image_128: Any? 
) : Serializable {

    /**
     * Extrae el texto Base64 real de la imagen, o nulo si Odoo devuelve "false".
     */
    val avatarBase64: String?
        get() = if (image_128 is String) image_128 else null
}