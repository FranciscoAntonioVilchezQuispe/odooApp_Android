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

    /**
     * Ejecuta una llamada genérica que devuelve una lista de productos.
     */
    @POST("/web/dataset/call_kw")
    suspend fun executeKwListProduct(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<Product>>>

    /**
     * Devuelve una lista de monedas.
     */
    @POST("/web/dataset/call_kw")
    suspend fun executeKwListCurrency(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<Currency>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListAccount(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<Account>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListAnalytic(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<AnalyticAccount>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListUom(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<Uom>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListTax(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<Tax>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListJournal(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<Journal>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListPaymentTerm(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<PaymentTerm>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListState(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<State>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListCity(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<City>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListDistrict(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<District>>>

    @POST("/web/dataset/call_kw")
    suspend fun executeKwListDetractionType(@Body request: OdooRequest<CallKwParams>): Response<OdooResponse<List<DetractionType>>>
}
