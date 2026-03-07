package com.stackperu.odooapp.api

import com.stackperu.odooapp.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interfaz que define los endpoints (rutas) de Odoo 19 para la aplicación.
 * Odoo utiliza peticiones POST bajo el estándar JSON-RPC 2.0.
 */
interface OdooApiService {

    /**
     * Autentica al usuario y retorna la cookie de sesión (session_id).
     */
    @POST("/web/session/authenticate")
    suspend fun login(@Body request: OdooRequest<AuthParams>): Response<OdooResponse<AuthResult>>

    /**
     * Destruye la sesión en el servidor de Odoo (Cerrar sesión).
     */
    @POST("/web/session/destroy")
    suspend fun logout(@Body request: OdooRequest<Any> = OdooRequest(params = emptyMap<String, Any>())): Response<OdooResponse<Any>>

    /**
     * Ejecuta métodos del ORM de Odoo. 
     * Se usa para invocar 'search_read' sobre modelos como 'res.partner'.
     */
    @POST("/web/dataset/call_kw")
    suspend fun getContacts(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<Contact>>>
}