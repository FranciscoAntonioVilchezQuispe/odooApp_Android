package com.stackperu.odooapp.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface SunatApiService {
    @GET
    suspend fun getTipoCambio(@Url url: String): Response<ResponseBody>
}
