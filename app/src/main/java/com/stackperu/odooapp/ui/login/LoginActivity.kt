package com.stackperu.odooapp.ui.login

import android.content.Intent
import android.os.Bundle
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
import com.stackperu.odooapp.model.OdooRequest
import com.stackperu.odooapp.model.User
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

        // Acción al presionar el botón de inicio de sesión
        binding.btnLogin.setOnClickListener {
            // El nombre de la base de datos ahora se toma del archivo AppConfig central
            val databaseName = AppConfig.DATABASE_NAME 

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(databaseName, email, password)
            } else {
                Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Ejecuta la petición de Login estructurando los datos en formato JSON-RPC de Odoo.
     */
    private fun performLogin(db: String, email: String, pass: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                // Preparamos los parámetros de la solicitud
                val authParams = AuthParams(db = db, login = email, password = pass)
                val request = OdooRequest(params = authParams)
                
                // Realizamos la solicitud real a la URL configurada
                val response = RetrofitClient.apiService.login(request)
                
                if (response.isSuccessful && response.body()?.result != null) {
                    val result = response.body()!!.result!!
                    
                    // Almacenamos el usuario en sesión en la memoria de la App
                    val user = User(
                        id = result.uid,
                        name = result.name,
                        avatarUrl = "", // Odoo no devuelve el avatar en el login, se descarga usando la url y cookie
                        email = result.username
                    )
                    
                    UserSession.currentUser = user
                    
                    // Redirigimos a la ventana principal
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = response.body()?.error?.data?.message ?: "Credenciales incorrectas o BD inválida"
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error de red: \${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }
}