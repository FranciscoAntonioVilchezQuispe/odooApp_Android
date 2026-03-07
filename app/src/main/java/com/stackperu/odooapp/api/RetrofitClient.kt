package com.stackperu.odooapp.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.stackperu.odooapp.AppConfig
import java.util.concurrent.TimeUnit

/**
 * Gestor de Cookies en memoria para guardar el session_id generado por Odoo 19.
 * Esto asegura que todas las peticiones a la API mantengan la sesión activa.
 */
class SessionCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    /**
     * Guarda las cookies que responde Odoo (en especial session_id).
     */
    override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
        val iterator = newCookies.iterator()
        while (iterator.hasNext()) {
            val cookie = iterator.next()
            // Reemplazamos la cookie antigua si tiene el mismo nombre
            cookies.removeAll { it.name() == cookie.name() }
            cookies.add(cookie)
        }
    }

    /**
     * Inyecta las cookies guardadas en cada nueva petición.
     */
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies
    }

    /**
     * Obtiene el valor exacto de la cookie session_id (usado para cargar imágenes).
     */
    fun getSessionCookieValue(): String {
        return cookies.find { it.name() == "session_id" }?.value() ?: ""
    }

    /**
     * Limpia todas las cookies almacenadas (Cerrar Sesión).
     */
    fun clearSession() {
        cookies.clear()
    }
}

/**
 * Cliente Singleton (Instancia única) para configurar Retrofit y conectarse a Odoo.
 */
object RetrofitClient {
    // La URL ahora se trae desde el archivo AppConfig
    const val BASE_URL = AppConfig.BASE_URL

    val cookieJar = SessionCookieJar()

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar) // Asigna el manejador de cookies
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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