package com.stackperu.odooapp.api

import com.stackperu.odooapp.model.AuthParams
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Kwargs
import com.stackperu.odooapp.model.OdooRequest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * Prueba para diagnosticar el error 'unexpected end of stream'.
 * Ejecuta esta prueba en Android Studio haciendo clic en el icono de 'Play' al lado de la clase.
 */
class OdooNetworkTest {

    @Test
    fun testLoginAndFetchContacts() = runBlocking {
        println("Iniciando prueba de red...")
        
        // Configuramos una URL alternativa si estamos en la PC y no en el emulador
        val testUrl = "http://localhost:8069" 
        println("Usando URL de prueba: $testUrl (Asegúrate de que Odoo sea accesible aquí)")

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(testUrl)
            .client(RetrofitClient.cookieJar.let { jar ->
                okhttp3.OkHttpClient.Builder()
                    .cookieJar(RetrofitClient.cookieJar)
                    .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                    })
                    .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
                    .connectionPool(okhttp3.ConnectionPool(0, 1, java.util.concurrent.TimeUnit.NANOSECONDS))
                    .build()
            })
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(OdooApiService::class.java)
        
        try {
            // 1. Intentar Login
            println("Paso 1: Intentando Autenticación...")
            val loginParams = AuthParams(
                db = "odoo_db", 
                login = "vilchez@hotmail.com",
                password = "admin" 
            )
            val authResponse = apiService.login(OdooRequest(params = loginParams))
            
            assertTrue("El login falló: ${authResponse.errorBody()?.string()}", authResponse.isSuccessful)
            println("Login exitoso. Result: ${authResponse.body()?.result?.name}")

            // 2. Intentar obtener contactos (donde falla la app)
            println("Paso 2: Intentando obtener contactos...")
            val callKwParams = CallKwParams(
                model = "res.partner",
                method = "search_read",
                args = listOf(emptyList<Any>()),
                kwargs = Kwargs(
                    fields = listOf("id", "name", "email")
                )
            )
            
            val contactsResponse = apiService.executeKwList(OdooRequest(params = callKwParams))
            
            if (contactsResponse.isSuccessful) {
                println("¡ÉXITO! Se obtuvieron ${contactsResponse.body()?.result?.size} contactos.")
            } else {
                println("FALLO: Odoo devolvió error ${contactsResponse.code()}")
                println("Error Body: ${contactsResponse.body()?.error?.message}")
            }
            
            assertNotNull("La respuesta de contactos no debería ser nula", contactsResponse.body())
            
        } catch (e: Exception) {
            println("--- ERROR DE PROTOCOLO DETECTADO ---")
            println("Clase de excepción: ${e.javaClass.name}")
            println("Mensaje: ${e.message}")
            e.printStackTrace()
            fail("Se produjo una excepción de red: ${e.message}")
        }
    }
}
