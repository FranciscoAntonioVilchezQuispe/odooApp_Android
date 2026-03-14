package com.stackperu.odooapp.ui.invoice

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.databinding.ListaProductosBinding
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Kwargs
import com.stackperu.odooapp.model.OdooRequest
import com.stackperu.odooapp.model.Product
import kotlinx.coroutines.launch

/**
 * Actividad para listar y gestionar el catálogo de productos de Odoo.
 */
class ListaProductos : AppCompatActivity() {

    private lateinit var binding: ListaProductosBinding
    private lateinit var adapter: AdaptadorProducto
    private var productList = mutableListOf<Product>()

    private val lanzadorFormulario = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            obtenerProductos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ListaProductosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarInterfaz()
        obtenerProductos()
    }

    private fun configurarInterfaz() {
        binding.btnBack.setOnClickListener { finish() }

        adapter = AdaptadorProducto(productList) { producto ->
            val intent = Intent(this, FormularioProducto::class.java)
            intent.putExtra("PRODUCT", producto)
            lanzadorFormulario.launch(intent)
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                obtenerProductos(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) obtenerProductos("")
                return true
            }
        })

        binding.fabAddProduct.setOnClickListener {
            val intent = Intent(this, FormularioProducto::class.java)
            lanzadorFormulario.launch(intent)
        }
    }

    private fun obtenerProductos(busqueda: String = "") {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Solo productos que se pueden vender
                val baseFilter = listOf("sale_ok", "=", true)
                
                val dominio = if (busqueda.isNotEmpty()) {
                    listOf("&", baseFilter, listOf("name", "ilike", busqueda))
                } else {
                    listOf(baseFilter)
                }

                val params = CallKwParams(
                    model = AppConfig.MODEL_PRODUCT,
                    method = "search_read",
                    args = listOf(dominio),
                    kwargs = Kwargs(
                        fields = listOf("id", "name", "lst_price", "type", "qty_available", "default_code"),
                        limit = 80,
                        order = "name ASC"
                    )
                )

                val response = RetrofitClient.apiService.executeKwListProduct(OdooRequest(params = params))
                if (response.isSuccessful && response.body()?.result != null) {
                    productList = response.body()?.result?.toMutableList() ?: mutableListOf()
                    adapter.updateData(productList)
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al obtener productos", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
