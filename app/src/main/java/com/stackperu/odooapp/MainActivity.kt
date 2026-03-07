package com.stackperu.odooapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.data.UserSession
import com.stackperu.odooapp.databinding.ActivityMainBinding
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Contact
import com.stackperu.odooapp.model.Kwargs
import com.stackperu.odooapp.model.OdooRequest
import com.stackperu.odooapp.ui.contact.ContactAdapter
import com.stackperu.odooapp.ui.contact.ContactDetailActivity
import com.stackperu.odooapp.ui.login.LoginActivity
import kotlinx.coroutines.launch

/**
 * MainActivity maneja la vista principal: muestra los contactos en modo Kanban, Lista o Pivot.
 * Ajustado para Odoo 19 con soporte para descargar el avatar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private var contactList: List<Contact> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si la sesión existe
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopBar()
        setupRecyclerView()
        setupViewSelectors()

        fetchContacts()
    }

    /**
     * Configura la barra superior: nombre, avatar de usuario y botón de cerrar sesión.
     */
    private fun setupTopBar() {
        val user = UserSession.currentUser
        binding.tvUserName.text = user?.name ?: "Usuario"

        // Para obtener el avatar del usuario logueado en Odoo necesitamos usar la cookie de session_id
        val sessionCookie = RetrofitClient.cookieJar.getSessionCookieValue()
        if (user != null && sessionCookie.isNotEmpty()) {
            val avatarUrl = "\${RetrofitClient.BASE_URL}/web/image?model=res.users&id=\${user.id}&field=image_128"
            val glideUrl = GlideUrl(
                avatarUrl,
                LazyHeaders.Builder()
                    .addHeader("Cookie", "session_id=\$sessionCookie")
                    .build()
            )

            Glide.with(this)
                .load(glideUrl)
                .circleCrop()
                .placeholder(R.drawable.avatar_placeholder)
                .into(binding.ivUserAvatar)
        }

        // Configurar botón de Cerrar Sesión
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    /**
     * Ejecuta el cierre de sesión tanto en la memoria de la app como en el servidor Odoo.
     */
    private fun performLogout() {
        lifecycleScope.launch {
            try {
                // Notificar al backend de Odoo que destruya la sesión
                RetrofitClient.apiService.logout()
            } catch (e: Exception) {
                Log.e("OdooApp", "Error cerrando sesión en servidor: \${e.message}")
            } finally {
                // Limpiamos los datos locales sin importar lo que responda el servidor
                UserSession.clear()
                RetrofitClient.cookieJar.clearSession()

                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    /**
     * Inicializa el RecyclerView para mostrar la lista de contactos.
     */
    private fun setupRecyclerView() {
        // Vista por defecto: Malla (Kanban)
        contactAdapter = ContactAdapter(contactList, isKanbanView = true) { contact ->
            // Al hacer clic en el item, va a la vista formulario
            val intent = Intent(this, ContactDetailActivity::class.java)
            intent.putExtra("CONTACT", contact)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = contactAdapter
    }

    /**
     * Configura los botones para alternar entre Kanban, Lista y Pivot.
     */
    private fun setupViewSelectors() {
        binding.btnKanban.setOnClickListener {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvPivotView.visibility = View.GONE
            binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
            contactAdapter.setViewMode(true)
        }

        binding.btnList.setOnClickListener {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvPivotView.visibility = View.GONE
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            contactAdapter.setViewMode(false)
        }

        binding.btnPivot.setOnClickListener {
            binding.recyclerView.visibility = View.GONE
            binding.tvPivotView.visibility = View.VISIBLE
            val totalContacts = contactList.size
            binding.tvPivotView.text = "Resumen (Pivot):\nTotal de Contactos: \$totalContacts"
        }
    }

    /**
     * Obtiene los contactos (res.partner) usando la ruta moderna call_kw de Odoo 19.
     */
    private fun fetchContacts() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Preparamos los parámetros del ORM: env['res.partner'].search_read([], ['name', ...])
                val callKwParams = CallKwParams(
                    model = "res.partner",
                    method = "search_read",
                    args = listOf(emptyList<Any>()), // Dominio vacío = traer todos los contactos
                    kwargs = Kwargs(
                        // Añadimos 'image_128' para traernos el avatar en Base64
                        fields = listOf("id", "name", "email", "phone", "vat", "image_128")
                    )
                )
                val request = OdooRequest(params = callKwParams)

                val response = RetrofitClient.apiService.getContacts(request)
                
                if (response.isSuccessful && response.body()?.result != null) {
                    val list = response.body()!!.result!!
                    contactList = list
                    contactAdapter.updateData(list)
                    
                    if (list.isEmpty()) {
                         Toast.makeText(this@MainActivity, "No hay contactos registrados", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val odooError = response.body()?.error
                    if (odooError != null) {
                        val errorMsg = "Error Odoo: \${odooError.message} - \${odooError.data?.message}"
                        Log.e("OdooApp", errorMsg)
                        Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                    } else {
                        val httpError = "Error HTTP: \${response.code()} \${response.message()}"
                        Log.e("OdooApp", httpError)
                        Toast.makeText(this@MainActivity, httpError, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Excepción al traer contactos", e)
                Toast.makeText(this@MainActivity, "Error de red: \${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}