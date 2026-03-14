package com.stackperu.odooapp.api

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ConnectionPool
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.stackperu.odooapp.AppConfig
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor de Cookies para Odoo 19.
 */
class SessionCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, Cookie>()

    override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
        for (cookie in newCookies) {
            Log.d("OdooApp", "Cookie Recibida: ${cookie.name} = ${cookie.value}")
            cookieStore[cookie.name] = cookie
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = cookieStore.values.toList()
        Log.d("OdooApp", "Enviando ${cookies.size} cookies a ${url.encodedPath}")
        return cookies
    }

    fun getSessionCookieValue(): String {
        return cookieStore["session_id"]?.value ?: ""
    }

    fun clearSession() {
        Log.d("OdooApp", "Limpiando almacén de cookies")
        cookieStore.clear()
    }
}

object RetrofitClient {
    const val BASE_URL = AppConfig.BASE_URL

    val cookieJar = SessionCookieJar()

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (AppConfig.ENABLE_NETWORK_LOGS) 
                HttpLoggingInterceptor.Level.BODY 
            else 
                HttpLoggingInterceptor.Level.NONE
        })
        .protocols(listOf(Protocol.HTTP_1_1)) // HTTP/1.1 es esencial para Odoo
        .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
        .connectTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val apiService: OdooApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OdooApiService::class.java)
    }

    val identityApiService: IdentityApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.IDENTITY_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IdentityApiService::class.java)
    }

    val sunatApiService: SunatApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.sunat.gob.pe/")
            .client(okHttpClient)
            .build()
            .create(SunatApiService::class.java)
    }
}
