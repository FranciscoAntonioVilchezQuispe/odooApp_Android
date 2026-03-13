package com.stackperu.odooapp.ui.contact

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.databinding.ListaClientesBinding
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Contact
import com.stackperu.odooapp.model.Kwargs
import com.stackperu.odooapp.model.OdooRequest
import kotlinx.coroutines.launch

/**
 * Mantenimiento dedicado a Clientes y Proveedores (Filtra por customer_rank > 0 o supplier_rank > 0).
 */
class ListaClientes : AppCompatActivity() {

    private lateinit var binding: ListaClientesBinding
    private lateinit var adapter: AdaptadorContacto
    private var clientList = mutableListOf<Contact>()

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            obtenerClientes()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ListaClientesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarInterfaz()
        obtenerClientes()
    }

    private fun configurarInterfaz() {
        binding.btnBack.setOnClickListener { finish() }

        adapter = AdaptadorContacto(clientList, isKanbanView = true) { cliente ->
            val intent = Intent(this, FormularioCliente::class.java)
            intent.putExtra("CONTACT", cliente)
            resultLauncher.launch(intent)
        }

        binding.rvClients.layoutManager = GridLayoutManager(this, 2)
        binding.rvClients.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                obtenerClientes(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) obtenerClientes("")
                return true
            }
        })

        binding.fabAddClient.setOnClickListener {
            val intent = Intent(this, FormularioCliente::class.java)
            resultLauncher.launch(intent)
        }
    }

    private fun obtenerClientes(busqueda: String = "") {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Usamos el repositorio centralizado
                clientList = com.stackperu.odooapp.data.ContactRepository.buscarContactos(
                    query = busqueda,
                    rankField = "customer_rank", // Solo clientes en esta vista (o proveedores según contexto)
                    limit = 100
                ).toMutableList()
                
                adapter.updateData(clientList)
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al cargar clientes", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
