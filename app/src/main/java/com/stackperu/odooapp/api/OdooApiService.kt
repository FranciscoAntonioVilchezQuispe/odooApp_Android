package com.stackperu.odooapp.api

import com.stackperu.odooapp.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interfaz que define los endpoints de Odoo 19 para la aplicación.
 */
interface OdooApiService {

    @POST("/web/session/authenticate")
    suspend fun login(@Body request: OdooRequest<AuthParams>): Response<OdooResponse<AuthResult>>

    @POST("/web/session/destroy")
    suspend fun logout(@Body request: OdooRequest<Any> = OdooRequest(params = emptyMap<String, Any>())): Response<OdooResponse<Any>>

    /**
     * Ejecuta una llamada genérica al ORM de Odoo (search_read, write, create, etc).
     */
    @POST("/web/dataset/call_kw")
    suspend fun executeKw(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<Any>>

    /**
     * Ejecuta una llamada genérica que devuelve una lista de contactos (específico para search_read).
     */
    @POST("/web/dataset/call_kw")
    suspend fun executeKwList(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<Contact>>>
}
