package com.stackperu.odooapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stackperu.odooapp.AppConfig
import com.stackperu.odooapp.MainActivity
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.data.UserSession
import com.stackperu.odooapp.databinding.ActivityLoginBinding
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
 * Modificada para comunicarse nativamente con Odoo 19 usando JSON-RPC.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val databaseName = AppConfig.DATABASE_NAME 
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
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
                // 1. PREPARAMOS PARÁMETROS DE LOGIN
                val authParams = AuthParams(db = db, login = email, password = pass)
                val loginRequest = OdooRequest(params = authParams)
                
                // 2. HACEMOS LA SOLICITUD DE LOGIN
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
                    
                    // SOLUCIÓN CLAVE AL PROBLEMA DE TIMING:
                    // Obligamos a la corrutina a suspenderse brevemente.
                    // Esto garantiza que OkHttp/Retrofit termine de ejecutar el callback
                    // interno de "saveFromResponse" en el SessionCookieJar antes de lanzar
                    // la siguiente petición HTTP.
                    delay(500) // Medio segundo de espera para que la cookie asiente en memoria
                    
                    // 3. AHORA SÍ, PEDIMOS LOS CONTACTOS
                    fetchInitialContactsAndNavigate()

                } else {
                    val errorMsg = loginResponse.body()?.error?.data?.message ?: "Credenciales incorrectas o BD inválida"
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al loguearse", e)
                Toast.makeText(this@LoginActivity, "Error de red: \${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private suspend fun fetchInitialContactsAndNavigate() {
        try {
            val callKwParams = CallKwParams(
                model = "res.partner",
                method = "search_read",
                args = listOf(emptyList<Any>()), 
                kwargs = Kwargs(
                    fields = listOf("id", "name", "email", "phone", "vat", "image_128")
                )
            )
            val request = OdooRequest(params = callKwParams)

            val response = RetrofitClient.apiService.getContacts(request)
            
            if (response.isSuccessful && response.body()?.result != null) {
                val contactList = response.body()!!.result!!
                
                val serializableList = ArrayList(contactList)
                
                val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                    putExtra("INITIAL_CONTACTS", serializableList)
                }
                startActivity(intent)
                finish() 
            } else {
                val odooError = response.body()?.error
                val errorMsg = if (odooError != null) {
                    "Error Odoo al traer datos: \${odooError.message}"
                } else {
                    "Error HTTP al traer datos: \${response.code()}"
                }
                Log.e("OdooApp", errorMsg)
                Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        } catch (e: Exception) {
             Log.e("OdooApp", "Excepción al traer contactos", e)
             Toast.makeText(this@LoginActivity, "Excepción obteniendo datos: \${e.message}", Toast.LENGTH_LONG).show()
             binding.progressBar.visibility = View.GONE
             binding.btnLogin.isEnabled = true
        }
    }
}