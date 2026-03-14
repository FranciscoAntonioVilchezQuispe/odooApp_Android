package com.stackperu.odooapp.ui.invoice

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.databinding.FormularioProductoBinding
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.OdooRequest
import kotlinx.coroutines.launch

/**
 * Actividad para la creación de nuevos productos/servicios en Odoo 19.
 */
class FormularioProducto : AppCompatActivity() {

    private lateinit var binding: FormularioProductoBinding
    private var productoActual: com.stackperu.odooapp.model.Product? = null
    private var uomsMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FormularioProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productoActual = intent.getSerializableExtra("PRODUCT") as? com.stackperu.odooapp.model.Product

        configurarInterfaz()
        
        if (productoActual != null) {
            cargarDatosProducto(productoActual!!)
        }
    }

    private fun cargarDatosProducto(producto: com.stackperu.odooapp.model.Product) {
        binding.tvAppTitle.text = "Editar Producto / Servicio"
        binding.etName.setText(producto.name)
        binding.etPrice.setText(producto.lst_price.toString())
        binding.etInternalRef.setText(producto.default_code ?: "")
        
        // Asumiendo que agregamos barcode al modelo Product más adelante si es necesario, 
        // de momento solo Referencia Interna que ya está en el modelo.
        
        val label = when(producto.type) {
            "consu" -> "Consumible"
            "service" -> "Servicio"
            "product" -> "Producto Almacenable"
            else -> "Consumible"
        }
        binding.actvType.setText(label, false)
        binding.btnSaveProduct.text = "Actualizar Producto"
    }

    private fun configurarInterfaz() {
        binding.btnBack.setOnClickListener { finish() }
        
        cargarUdMs()

        // Configurar tipos de producto (mapeo interno para Odoo)
        val tiposOdoo = mapOf(
            "Consumible" to "consu",
            "Servicio" to "service",
            "Producto Almacenable" to "product"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tiposOdoo.keys.toList())
        binding.actvType.setAdapter(adapter)

        binding.btnSaveProduct.setOnClickListener {
            guardarProducto(tiposOdoo)
        }
    }

    private fun cargarUdMs() {
        lifecycleScope.launch {
            try {
                val results = com.stackperu.odooapp.data.ProductRepository.obtenerUdMs()
                val names = results.map { 
                    uomsMap[it.name] = it.id
                    it.name 
                }
                val adapter = ArrayAdapter(this@FormularioProducto, android.R.layout.simple_dropdown_item_1line, names)
                binding.actvUom.setAdapter(adapter)
                
                // Seleccionar 'Unidad' por defecto si es nuevo
                if (productoActual == null) {
                    val unidad = names.find { it.lowercase().contains("unidad") } ?: names.firstOrNull()
                    if (unidad != null) {
                        binding.actvUom.setText(unidad, false)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OdooApp", "Error cargando UdMs", e)
            }
        }
    }

    private fun guardarProducto(tiposMap: Map<String, String>) {
        val name = binding.etName.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val typeLabel = binding.actvType.text.toString()
        val typeValue = tiposMap[typeLabel] ?: "consu"
        val internalRef = binding.etInternalRef.text.toString().trim()
        val barcode = binding.etBarcode.text.toString().trim()
        val uomName = binding.actvUom.text.toString()
        val uomId = uomsMap[uomName]
        val description = binding.etDescription.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Nombre y Precio son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull() ?: 0.0

        binding.pbSaving.visibility = View.VISIBLE
        binding.btnSaveProduct.isEnabled = false

        lifecycleScope.launch {
            try {
                val data = mutableMapOf<String, Any>(
                    "name" to name,
                    "lst_price" to price,
                    "type" to typeValue,
                    "sale_ok" to true,
                    "purchase_ok" to true,
                    "default_code" to internalRef
                )
                
                if (uomId != null) {
                    data["uom_id"] = uomId
                    data["uom_po_id"] = uomId // Usualmente misma UdM para compra
                }
                
                if (description.isNotEmpty()) {
                    data["description_sale"] = description
                }
                
                if (barcode.isNotEmpty()) {
                    data["barcode"] = barcode
                }

                val esEdicion = productoActual != null
                val metodo = if (esEdicion) "write" else "create"
                
                val args = if (esEdicion) {
                    listOf(listOf(productoActual!!.id), data)
                } else {
                    listOf(data)
                }

                val params = CallKwParams(
                    model = AppConfig.MODEL_PRODUCT,
                    method = metodo,
                    args = args
                )

                val response = RetrofitClient.apiService.executeKw(OdooRequest(params = params))
                if (response.isSuccessful && response.body()?.error == null) {
                    val mensaje = if (esEdicion) "Producto actualizado" else "Producto creado con éxito"
                    Toast.makeText(this@FormularioProducto, mensaje, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val error = response.body()?.error?.message ?: "Error desconocido"
                    Toast.makeText(this@FormularioProducto, "Error de Odoo: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FormularioProducto, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.pbSaving.visibility = View.GONE
                binding.btnSaveProduct.isEnabled = true
            }
        }
    }
}
