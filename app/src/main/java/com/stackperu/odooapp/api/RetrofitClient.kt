package com.stackperu.odooapp.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.stackperu.odooapp.AppConfig
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor de Cookies en memoria para guardar el session_id generado por Odoo 19.
 * Usando ConcurrentHashMap para evitar problemas de sincronización en peticiones súper rápidas.
 */
class SessionCookieJar : CookieJar {
    // Usamos un mapa concurrente para asegurarnos que la escritura y lectura
    // en hilos de fondo diferentes (corrutinas) sea atómica y segura.
    private val cookieStore = ConcurrentHashMap<String, Cookie>()

    /**
     * Guarda las cookies que responde Odoo (en especial session_id).
     */
    override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
        for (cookie in newCookies) {
            cookieStore[cookie.name()] = cookie
        }
    }

    /**
     * Inyecta las cookies guardadas en cada nueva petición.
     */
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore.values.toList()
    }

    /**
     * Obtiene el valor exacto de la cookie session_id (usado para cargar imágenes).
     */
    fun getSessionCookieValue(): String {
        return cookieStore["session_id"]?.value() ?: ""
    }

    /**
     * Limpia todas las cookies almacenadas (Cerrar Sesión).
     */
    fun clearSession() {
        cookieStore.clear()
    }
}

/**
 * Cliente Singleton (Instancia única) para configurar Retrofit y conectarse a Odoo.
 */
object RetrofitClient {
    const val BASE_URL = AppConfig.BASE_URL

    val cookieJar = SessionCookieJar()

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
}