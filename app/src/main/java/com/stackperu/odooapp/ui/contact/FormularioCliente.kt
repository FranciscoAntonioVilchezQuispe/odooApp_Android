package com.stackperu.odooapp.ui.contact

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stackperu.odooapp.AppConfig
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.databinding.FormularioClienteBinding
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Kwargs
import com.stackperu.odooapp.model.OdooRequest
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.os.Build
import kotlinx.coroutines.launch

/**
 * Actividad para la gestión de Clientes y Proveedores con integración SUNAT/RENIEC.
 */
class FormularioCliente : AppCompatActivity() {

    private lateinit var binding: FormularioClienteBinding
    private var tiposIdentificacion = mutableMapOf<String, Int>()
    private var regiones = mutableMapOf<String, Int>()
    private var provincias = mutableMapOf<String, Int>()
    private var distritos = mutableMapOf<String, Int>()
    private var clienteActual: com.stackperu.odooapp.model.Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FormularioClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarModoPantallaCompleta()

        clienteActual = intent.getSerializableExtra("CONTACT") as? com.stackperu.odooapp.model.Contact

        configurarInterfaz()
        cargarTiposIdentificacion()
        cargarRegiones()
    }

    private fun configurarInterfaz() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSearchDoc.setOnClickListener {
            validarYBuscarDocumento()
        }

        binding.btnSave.setOnClickListener {
            guardarCliente()
        }

        binding.btnPromoteContact.setOnClickListener {
            promoverContactoExistente()
        }
    }

    private fun cargarTiposIdentificacion() {
        lifecycleScope.launch {
            try {
                val params = CallKwParams(
                    model = AppConfig.MODEL_IDENTIFICATION_TYPE,
                    method = "search_read",
                    args = listOf(listOf<Any>()),
                    kwargs = Kwargs(fields = listOf("id", "name"))
                )
                
                val response = RetrofitClient.apiService.executeKwList(OdooRequest(params = params))
                if (response.isSuccessful && response.body()?.result != null) {
                    val list = response.body()?.result ?: emptyList()
                    val names = mutableListOf<String>()
                    list.forEach { 
                        val name = it.name
                        tiposIdentificacion[name] = it.id
                        names.add(name)
                    }
                    
                    val adapter = ArrayAdapter(this@FormularioCliente, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvDocType.setAdapter(adapter)

                    // Si es edición, seleccionamos el tipo correcto
                    if (clienteActual != null) {
                        cargarDatosCliente(clienteActual!!)
                    }
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al cargar tipos de identificación", e)
            }
        }
    }

    private fun cargarRegiones() {
        lifecycleScope.launch {
            try {
                val results = com.stackperu.odooapp.data.ProductRepository.obtenerRegiones()
                val names = results.map { 
                    regiones[it.name] = it.id
                    it.name 
                }
                val adapter = ArrayAdapter(this@FormularioCliente, android.R.layout.simple_dropdown_item_1line, names)
                binding.actvRegion.setAdapter(adapter)
                
                binding.actvRegion.setOnItemClickListener { _, _, position, _ ->
                    val regionId = regiones[names[position]] ?: return@setOnItemClickListener
                    cargarProvincias(regionId)
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al cargar regiones", e)
            }
        }
    }

    private fun cargarProvincias(regionId: Int) {
        binding.actvProvince.setText("")
        binding.actvDistrict.setText("")
        binding.actvProvince.isEnabled = false
        binding.actvDistrict.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val results = com.stackperu.odooapp.data.ProductRepository.obtenerProvincias(regionId)
                provincias.clear()
                val names = results.map { 
                    provincias[it.name] = it.id
                    it.name 
                }
                val adapter = ArrayAdapter(this@FormularioCliente, android.R.layout.simple_dropdown_item_1line, names)
                binding.actvProvince.setAdapter(adapter)
                binding.actvProvince.isEnabled = true
                
                binding.actvProvince.setOnItemClickListener { _, _, position, _ ->
                    val provinceId = provincias[names[position]] ?: return@setOnItemClickListener
                    cargarDistritos(provinceId)
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al cargar provincias", e)
            }
        }
    }

    private fun cargarDistritos(provinceId: Int) {
        binding.actvDistrict.setText("")
        binding.actvDistrict.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val results = com.stackperu.odooapp.data.ProductRepository.obtenerDistritos(provinceId)
                distritos.clear()
                val names = results.map { 
                    distritos[it.name] = it.id
                    it.name 
                }
                val adapter = ArrayAdapter(this@FormularioCliente, android.R.layout.simple_dropdown_item_1line, names)
                binding.actvDistrict.setAdapter(adapter)
                binding.actvDistrict.isEnabled = true
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al cargar distritos", e)
            }
        }
    }

    private fun cargarDatosCliente(contacto: com.stackperu.odooapp.model.Contact) {
        binding.tvAppTitle.text = "Editar Cliente / Proveedor"
        binding.etName.setText(contacto.name)
        binding.etVat.setText(contacto.vat?.takeIf { it != "false" } ?: "")
        binding.etEmail.setText(contacto.email?.takeIf { it != "false" } ?: "")
        binding.etPhone.setText(contacto.phone?.takeIf { it != "false" } ?: "")
        binding.etAddress.setText(contacto.street?.takeIf { it != "false" } ?: "")
        
        // Manejo del tipo de identificación (Odoo devuelve [id, name] o id)
        val typeInfo = contacto.l10n_latam_identification_type_id
        if (typeInfo is List<*> && typeInfo.size >= 2) {
            val typeName = typeInfo[1].toString()
            binding.actvDocType.setText(typeName, false)
        }

        binding.btnSearchDoc.isEnabled = false 
        binding.btnSave.text = "Actualizar Cambios"
    }

    private fun validarYBuscarDocumento() {
        val vat = binding.etVat.text.toString().trim()
        if (vat.isEmpty()) {
            binding.tilVat.error = "Ingrese un número"
            return
        }
        binding.tilVat.error = null

        binding.pbSearch.visibility = View.VISIBLE
        binding.btnSearchDoc.isEnabled = false

        lifecycleScope.launch {
            try {
                // 1. Validar existencia en Odoo
                val paramsSearch = CallKwParams(
                    model = AppConfig.MODEL_PARTNER,
                    method = "search_read",
                    args = listOf(listOf(listOf("vat", "=", vat))),
                    kwargs = Kwargs(
                        fields = listOf("id", "name", "email", "phone", "street", "customer_rank", "supplier_rank"),
                        limit = 1
                    )
                )

                val responseSearch = RetrofitClient.apiService.executeKwList(OdooRequest(params = paramsSearch))
                val partners = responseSearch.body()?.result ?: emptyList()

                if (partners.isNotEmpty()) {
                    val p = partners[0]
                    val cRank = p.customer_rank ?: 0
                    val sRank = p.supplier_rank ?: 0

                    if (cRank == 0 && sRank == 0) {
                        // Es un contacto genérico, mostrar aviso de promoción
                        binding.cardInfoContacto.visibility = View.VISIBLE
                        binding.btnSave.isEnabled = false
                        clienteActual = p // Guardamos la referencia para promoverlo si el usuario acepta
                        
                        binding.etName.setText(p.name)
                        binding.etEmail.setText(p.email?.takeIf { it != "false" } ?: "")
                        binding.etPhone.setText(p.phone?.takeIf { it != "false" } ?: "")
                        binding.etAddress.setText(p.street?.takeIf { it != "false" } ?: "")
                    } else {
                        // Ya es cliente o proveedor
                        Toast.makeText(this@FormularioCliente, "Ya existe en Odoo: ${p.name}", Toast.LENGTH_LONG).show()
                        binding.cardInfoContacto.visibility = View.GONE
                        binding.etName.setText(p.name)
                        binding.etEmail.setText(p.email?.takeIf { it != "false" } ?: "")
                        binding.etPhone.setText(p.phone?.takeIf { it != "false" } ?: "")
                        binding.etAddress.setText(p.street?.takeIf { it != "false" } ?: "")
                        binding.btnSave.isEnabled = false
                    }
                } else {
                    // 2. Búsqueda real en SUNAT/RENIEC
                    binding.cardInfoContacto.visibility = View.GONE
                    realizarBusquedaIdentidadReal(vat)
                }
            } catch (e: Exception) {
                Toast.makeText(this@FormularioCliente, "Error al validar: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.pbSearch.visibility = View.GONE
                binding.btnSearchDoc.isEnabled = true
            }
        }
    }

    private fun realizarBusquedaIdentidadReal(vat: String) {
        val token = "Bearer ${AppConfig.IDENTITY_API_TOKEN}"
        lifecycleScope.launch {
            try {
                if (vat.length == 8) {
                    val response = RetrofitClient.identityApiService.buscarDni(vat, token)
                    if (response.isSuccessful && response.body() != null) {
                        val dni = response.body()!!
                        binding.etName.setText(dni.nombreCompleto)
                        binding.etAddress.setText("") // DNI usualmente no trae dirección fiscal
                        binding.btnSave.isEnabled = true
                        Toast.makeText(this@FormularioCliente, "DNI encontrado", Toast.LENGTH_SHORT).show()
                    }
                } else if (vat.length == 11) {
                    val response = RetrofitClient.identityApiService.buscarRuc(vat, token)
                    if (response.isSuccessful && response.body() != null) {
                        val ruc = response.body()!!
                        binding.etName.setText(ruc.razonSocial)
                        binding.etAddress.setText(ruc.direccion) // Dirección de SUNAT
                        binding.btnSave.isEnabled = true
                        Toast.makeText(this@FormularioCliente, "RUC encontrado (SUNAT)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error consulta externa", e)
            }
        }
    }

    private fun promoverContactoExistente() {
        val contacto = clienteActual ?: return
        
        lifecycleScope.launch {
            binding.pbSearch.visibility = View.VISIBLE
            binding.btnPromoteContact.isEnabled = false
            
            try {
                val data = mapOf(
                    "customer_rank" to 1,
                    "supplier_rank" to 1
                )
                val params = CallKwParams(
                    model = AppConfig.MODEL_PARTNER,
                    method = "write",
                    args = listOf(listOf(contacto.id), data)
                )
                
                val response = RetrofitClient.apiService.executeKw(OdooRequest(params = params))
                if (response.isSuccessful && response.body()?.error == null) {
                    Toast.makeText(this@FormularioCliente, "¡Contacto actualizado a Cliente/Proveedor!", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val error = response.body()?.error?.message ?: "Error al actualizar"
                    Toast.makeText(this@FormularioCliente, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FormularioCliente, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                binding.pbSearch.visibility = View.GONE
                binding.btnPromoteContact.isEnabled = true
            }
        }
    }

    private fun guardarCliente() {
        val name = binding.etName.text.toString().trim()
        val vat = binding.etVat.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val typeName = binding.actvDocType.text.toString()
        val typeId = tiposIdentificacion[typeName]

        if (name.isEmpty() || vat.isEmpty()) {
            Toast.makeText(this, "Datos obligatorios faltantes", Toast.LENGTH_SHORT).show()
            return
        }

        binding.pbSearch.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val data = mutableMapOf<String, Any>(
                    "name" to name,
                    "vat" to vat,
                    "email" to email,
                    "phone" to phone,
                    "street" to address, // Dirección en Odoo
                    "customer_rank" to 1
                )
                
                // Ubicación (Estado, Provincia, Distrito)
                val regionName = binding.actvRegion.text.toString()
                val provinceName = binding.actvProvince.text.toString()
                val districtName = binding.actvDistrict.text.toString()
                
                regiones[regionName]?.let { data["state_id"] = it }
                provincias[provinceName]?.let { data["city_id"] = it }
                distritos[districtName]?.let { data["l10n_pe_district"] = it }
                
                if (typeId != null) {
                    data["l10n_latam_identification_type_id"] = typeId
                }

                val esEdicion = clienteActual != null
                val metodo = if (esEdicion) "write" else "create"
                val args = if (esEdicion) listOf(listOf(clienteActual!!.id), data) else listOf(data)

                val params = CallKwParams(model = AppConfig.MODEL_PARTNER, method = metodo, args = args)
                val response = RetrofitClient.apiService.executeKw(OdooRequest(params = params))
                
                if (response.isSuccessful && response.body()?.error == null) {
                    Toast.makeText(this@FormularioCliente, "Operación exitosa", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val error = response.body()?.error?.message ?: "Error al guardar"
                    Toast.makeText(this@FormularioCliente, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FormularioCliente, "Error de red", Toast.LENGTH_SHORT).show()
            } finally {
                binding.pbSearch.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun configurarModoPantallaCompleta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}
