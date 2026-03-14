package com.stackperu.odooapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stackperu.odooapp.AppConfig
import com.stackperu.odooapp.Dashboard
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.data.UserSession
import com.stackperu.odooapp.databinding.LoginBinding
import com.stackperu.odooapp.model.AuthParams
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Contact
import com.stackperu.odooapp.model.Kwargs
import com.stackperu.odooapp.model.OdooRequest
import com.stackperu.odooapp.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity de Login que maneja la autenticación del usuario.
 */
class Login : AppCompatActivity() {

    private lateinit var binding: LoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val databaseName = AppConfig.DATABASE_NAME 
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Limpiamos cualquier rastro de sesiones anteriores antes de intentar una nueva
                RetrofitClient.cookieJar.clearSession()
                UserSession.clear()
                
                performLoginAndFetchInitialData(databaseName, email, password)
            } else {
                Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLoginAndFetchInitialData(db: String, email: String, pass: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val authParams = AuthParams(db = db, login = email, password = pass)
                val loginRequest = OdooRequest(params = authParams)
                
                // Petición de Login
                val loginResponse = RetrofitClient.apiService.login(loginRequest)
                
                if (loginResponse.isSuccessful && loginResponse.body()?.result != null) {
                    val result = loginResponse.body()!!.result!!
                    
                    val user = User(
                        id = result.uid,
                        name = result.name,
                        avatarUrl = "", 
                        email = result.username
                    )
                    UserSession.currentUser = user
                    
                    // Notificación de éxito solicitada por el usuario
                    Toast.makeText(this@Login, "Sesión iniciada correctamente para ${user.name}", Toast.LENGTH_SHORT).show()
                    
                    // IMPORTANTE: Damos tiempo a que el sistema asiente la nueva Cookie en memoria
                    // y que el servidor Odoo cierre la conexión anterior de forma limpia
                    delay(1500) 
                    
                    fetchInitialContactsAndNavigate()

                } else {
                    val errorMsg = loginResponse.body()?.error?.data?.message ?: "Error al validar credenciales"
                    Log.e("OdooApp", "Login Fallido: $errorMsg")
                    Toast.makeText(this@Login, errorMsg, Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Excepción en Login", e)
                val msg = if (e is java.net.ProtocolException) "Error de protocolo (Keep-Alive). Intente de nuevo." else e.message 
                Toast.makeText(this@Login, "Error: $msg", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private suspend fun fetchInitialContactsAndNavigate() {
        try {
            val callKwParams = CallKwParams(
                model = AppConfig.MODEL_PARTNER,
                method = "search_read",
                args = listOf(emptyList<Any>()), 
                kwargs = Kwargs(
                    fields = listOf("id", "name", "email", "phone", "vat"),
                    offset = 0,
                    limit = 100,
                    order = "id DESC"
                )
            )
            val request = OdooRequest(params = callKwParams)

            val response = RetrofitClient.apiService.executeKw(request)
            
            if (response.isSuccessful && response.body()?.result != null) {
                // Convertimos el resultado de Any a List<Contact> manualmente
                // usando Gson para asegurar la compatibilidad
                val gson = com.google.gson.Gson()
                val jsonResult = gson.toJson(response.body()!!.result)
                val type = object : com.google.gson.reflect.TypeToken<List<Contact>>() {}.type
                val contactList: List<Contact> = gson.fromJson(jsonResult, type)
                
                val intent = Intent(this@Login, Dashboard::class.java).apply {
                    putExtra("INITIAL_CONTACTS", ArrayList(contactList))
                }
                startActivity(intent)
                finish() 
            } else {
                val odooError = response.body()?.error
                val msg = odooError?.message ?: "Error desconocido al cargar datos"
                Log.e("OdooApp", "Error tras login: $msg")
                Toast.makeText(this@Login, "Autenticado, pero error al cargar datos: $msg", Toast.LENGTH_LONG).show()
                
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        } catch (e: Exception) {
             Log.e("OdooApp", "Excepción cargando datos", e)
             Toast.makeText(this@Login, "Fallo al obtener datos: ${e.message}", Toast.LENGTH_LONG).show()
             binding.progressBar.visibility = View.GONE
             binding.btnLogin.isEnabled = true
        }
    }
}
