package com.stackperu.odooapp.ui.contact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.stackperu.odooapp.R
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.data.UserSession
import com.stackperu.odooapp.databinding.ListaContactosBinding
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Contact
import com.stackperu.odooapp.model.Kwargs
import com.stackperu.odooapp.model.OdooRequest
import kotlinx.coroutines.launch

class ListaContactos : AppCompatActivity() {

    private lateinit var binding: ListaContactosBinding
    private lateinit var contactAdapter: AdaptadorContacto
    private var contactList: MutableList<Contact> = mutableListOf()
    
    private var ordenActual = "id DESC" 
    private var ultimaBusqueda = ""

    private val lanzadorFormulario = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            obtenerContactos(ultimaBusqueda)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ListaContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBarraSuperior()
        configurarRecyclerView()
        configurarSelectoresVista()
        configurarBusqueda()
        configurarAcciones()

        obtenerContactos()
    }

    private fun configurarBarraSuperior() {
        actualizarInfoUsuario()
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun configurarRecyclerView() {
        contactAdapter = AdaptadorContacto(contactList, isKanbanView = true) { contacto ->
            val intent = Intent(this, DetalleContacto::class.java)
            intent.putExtra("CONTACT", contacto)
            lanzadorFormulario.launch(intent)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = contactAdapter
    }

    private fun configurarBusqueda() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                obtenerContactos(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) obtenerContactos("")
                return true
            }
        })
    }

    private fun configurarAcciones() {
        binding.fabAddContact.setOnClickListener {
            val intent = Intent(this, FormularioContacto::class.java)
            lanzadorFormulario.launch(intent)
        }

        binding.btnSort.setOnClickListener {
            alternarOrden()
        }
    }

    private fun alternarOrden() {
        ordenActual = if (ordenActual == "name ASC") "name DESC" else "name ASC"
        val etiqueta = if(ordenActual == "name ASC") "A-Z" else "Z-A"
        binding.btnSort.text = etiqueta
        obtenerContactos(ultimaBusqueda)
    }

    private fun obtenerContactos(busqueda: String = "") {
        ultimaBusqueda = busqueda
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val dominio = if (busqueda.isNotEmpty()) {
                    listOf("|", "|",
                        listOf("name", "ilike", busqueda),
                        listOf("email", "ilike", busqueda),
                        listOf("phone", "ilike", busqueda))
                } else { emptyList<Any>() }

                val callKwParams = CallKwParams(
                    model = "res.partner",
                    method = "search_read",
                    args = listOf(dominio), 
                    kwargs = Kwargs(
                        fields = listOf("id", "name", "email", "phone", "vat"),
                        offset = 0,
                        limit = 100,
                        order = ordenActual
                    )
                )

                val response = RetrofitClient.apiService.executeKwList(OdooRequest(params = callKwParams))
                val odooResponse = response.body()

                if (response.isSuccessful && odooResponse != null) {
                    if (odooResponse.error != null) {
                        Log.e("OdooApp", "Error: ${odooResponse.error.message}")
                    } else {
                        contactList = odooResponse.result?.toMutableList() ?: mutableListOf()
                        contactAdapter.updateData(contactList)
                        binding.tvPivotView.text = "Resumen (Pivot):\nTotal de Contactos: ${contactList.size}"
                    }
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun configurarSelectoresVista() {
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
        }
    }

    private fun actualizarInfoUsuario() {
        val usuario = UserSession.currentUser ?: return
        binding.tvUserName.text = usuario.name
        val sessionCookie = RetrofitClient.cookieJar.getSessionCookieValue()
        if (sessionCookie.isNotEmpty()) {
            val avatarUrl = "${RetrofitClient.BASE_URL}/web/image?model=res.users&id=${usuario.id}&field=image_128"
            Glide.with(this).load(GlideUrl(avatarUrl, LazyHeaders.Builder().addHeader("Cookie", "session_id=$sessionCookie").build()))
                .circleCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .placeholder(R.drawable.avatar_placeholder).into(binding.ivUserAvatar)
        }
    }
}
