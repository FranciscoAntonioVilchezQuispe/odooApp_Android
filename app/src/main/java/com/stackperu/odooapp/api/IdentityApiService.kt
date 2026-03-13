package com.stackperu.odooapp.api

import com.stackperu.odooapp.model.DniResponse
import com.stackperu.odooapp.model.RucResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * Interfaz para la consulta de identidad SUNAT/RENIEC.
 */
interface IdentityApiService {

    @GET("dni/{numero}")
    suspend fun buscarDni(
        @Path("numero") numero: String,
        @Header("Authorization") token: String
    ): Response<DniResponse>

    @GET("ruc/{numero}")
    suspend fun buscarRuc(
        @Path("numero") numero: String,
        @Header("Authorization") token: String
    ): Response<RucResponse>
}
