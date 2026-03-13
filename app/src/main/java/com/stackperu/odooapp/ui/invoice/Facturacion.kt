package com.stackperu.odooapp.ui.invoice

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.stackperu.odooapp.R
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.data.UserSession
import com.stackperu.odooapp.databinding.FacturacionBinding
import com.stackperu.odooapp.model.*
import com.stackperu.odooapp.model.Currency
import com.stackperu.odooapp.utils.FormatterHelper
import com.stackperu.odooapp.utils.FormatterHelper.initials
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * Facturacion centraliza la creación de facturas de venta y compra.
 * Integra búsqueda en Odoo, monedas, fechas y tipo de cambio SUNAT.
 */
class Facturacion : AppCompatActivity() {

    private lateinit var binding: FacturacionBinding
    private var currentMoveType = "out_invoice" // Por defecto: Venta
    private var searchJob: Job? = null
    private val calendar = Calendar.getInstance()
    private var contactoSeleccionado: com.stackperu.odooapp.model.Contact? = null
    private var listaContactosSugeridos: List<com.stackperu.odooapp.model.Contact> = emptyList()

    private var journals: List<Journal> = emptyList()
    private var selectedJournal: Journal? = null
    private var paymentTerms: List<PaymentTerm> = emptyList()
    private var selectedPaymentTerm: PaymentTerm? = null

    private var monedasDisponibles: List<Currency> = emptyList()
    private var monedaSeleccionada: Currency? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FacturacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentMoveType = intent.getStringExtra("MOVE_TYPE") ?: "out_invoice"

        configurarTipoComprobante()
        configurarTipoOperacion()
        configurarLineasFactura()
        configurarSeleccionCliente()
        configurarSelectorMoneda()
        configurarSelectorFecha()
        configurarDiariosYPlazos()
        configurarAcciones()
        actualizarInfoUsuario()
        cargarTipoCambioSunat()

        // Aplicar modo inicial
        aplicarModoOperacion(currentMoveType)

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun actualizarInfoUsuario() {
        val usuario = UserSession.currentUser ?: return
        binding.tvUserName.text = usuario.name
        val sessionCookie = RetrofitClient.cookieJar.getSessionCookieValue()
        if (sessionCookie.isNotEmpty()) {
            val avatarUrl = "${RetrofitClient.BASE_URL}/web/image?model=res.users&id=${usuario.id}&field=image_128"
            Glide.with(this).load(com.bumptech.glide.load.model.GlideUrl(avatarUrl, com.bumptech.glide.load.model.LazyHeaders.Builder()
                .addHeader("Cookie", "session_id=$sessionCookie")
                .build()))
                .circleCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .placeholder(R.drawable.avatar_placeholder)
                .into(binding.ivUserAvatar)
        }
    }

    private fun configurarTipoOperacion() {
        binding.toggleOperationType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val nuevoModo = if (checkedId == R.id.btnTypeSale) "out_invoice" else "in_invoice"
                aplicarModoOperacion(nuevoModo)
            }
        }
    }

    private fun aplicarModoOperacion(modo: String) {
        currentMoveType = modo
        if (modo == "out_invoice") {
            binding.toggleOperationType.check(R.id.btnTypeSale)
            binding.tvAppTitle.text = "Nueva Venta"
            binding.tvCustomerLabel.text = "Información del Cliente"
            binding.tilCustomer.hint = "Buscar Cliente (RUC/Nombre)"
            binding.btnQuickAddCustomer.text = "+ Nuevo Cliente"
        } else {
            binding.toggleOperationType.check(R.id.btnTypePurchase)
            binding.tvAppTitle.text = "Nueva Compra"
            binding.tvCustomerLabel.text = "Información del Proveedor"
            binding.tilCustomer.hint = "Buscar Proveedor (RUC/Nombre)"
            binding.btnQuickAddCustomer.text = "+ Nuevo Proveedor"
        }
        // Limpiar búsqueda al cambiar modo
        binding.actvCustomer.setText("")
        cargarDiarios()
        actualizarVisibilidadReferencia()
    }

    private fun configurarDiariosYPlazos() {
        cargarPlazosPago()
        
        binding.actvJournal.setOnItemClickListener { _, _, position, _ ->
            selectedJournal = journals.getOrNull(position)
        }

        binding.actvPaymentTerm.setOnItemClickListener { _, _, position, _ ->
            selectedPaymentTerm = paymentTerms.getOrNull(position)
        }
    }

    private fun cargarDiarios() {
        val odooType = if (currentMoveType == "out_invoice") "sale" else "purchase"
        lifecycleScope.launch {
            try {
                val results = com.stackperu.odooapp.data.ProductRepository.obtenerDiarios(odooType)
                journals = results
                val names = results.map { "${it.name.initials()} - [${it.code}]" }
                val adapter = ArrayAdapter(this@Facturacion, android.R.layout.simple_dropdown_item_1line, names)
                binding.actvJournal.setAdapter(adapter)
                
                // Auto-seleccionar según tipo actual
                seleccionarDiarioPorTipo()
            } catch (e: Exception) {
                Toast.makeText(this@Facturacion, "Error cargando diarios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarPlazosPago() {
        lifecycleScope.launch {
            try {
                val results = com.stackperu.odooapp.data.ProductRepository.obtenerPlazosPago()
                paymentTerms = results
                val names = results.map { it.name }
                val adapter = ArrayAdapter(this@Facturacion, android.R.layout.simple_dropdown_item_1line, names)
                binding.actvPaymentTerm.setAdapter(adapter)
                
                // Intentar seleccionar 'Inmediato' o 'Contado' por defecto
                val contado = results.find { it.name.lowercase().contains("contado") || it.name.lowercase().contains("inmediato") } ?: results.firstOrNull()
                if (contado != null) {
                    selectedPaymentTerm = contado
                    binding.actvPaymentTerm.setText(contado.name, false)
                }
            } catch (e: Exception) {
                // Silencioso o log
            }
        }
    }

    private fun actualizarVisibilidadReferencia() {
        val isNC = binding.toggleGroupType.checkedButtonId == R.id.btnTypeNC
        binding.tilReference.visibility = if (isNC) View.VISIBLE else View.GONE
        
        // Automatizar selección de diario al cambiar tipo
        if (journals.isNotEmpty()) {
            seleccionarDiarioPorTipo()
        }
    }

    private fun seleccionarDiarioPorTipo() {
        val typeButtonId = binding.toggleGroupType.checkedButtonId
        val targetPrefix = when (typeButtonId) {
            R.id.btnTypeFact -> "F"
            R.id.btnTypeBol -> "B"
            R.id.btnTypeNC -> "NC"
            else -> ""
        }

        val match = journals.find { 
            it.code.uppercase().startsWith(targetPrefix) || 
            it.name.uppercase().contains(if (targetPrefix == "NC") "NOTA" else targetPrefix)
        } ?: journals.firstOrNull()

        if (match != null) {
            selectedJournal = match
            val position = journals.indexOf(match)
            if (position >= 0) {
                val name = "${match.name.initials()} - [${match.code}] "
                binding.actvJournal.setText(name, false)
            }
        }
    }

    private fun configurarTipoComprobante() {
        binding.toggleGroupType.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                actualizarVisibilidadReferencia()
            }
        }
    }

    private val listaLineasFactura = mutableListOf<com.stackperu.odooapp.model.InvoiceLine>()
    private lateinit var adaptadorLineas: AdaptadorLineaFactura

    private fun configurarLineasFactura() {
        adaptadorLineas = AdaptadorLineaFactura(listaLineasFactura) { position ->
            listaLineasFactura.removeAt(position)
            adaptadorLineas.notifyItemRemoved(position)
            actualizarTotales()
        }
        
        binding.rvInvoiceLines.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvInvoiceLines.adapter = adaptadorLineas

        binding.btnAddProduct.setOnClickListener {
            val dialog = DialogDetalleLinea(currentMoveType == "out_invoice") { nuevaLinea ->
                listaLineasFactura.add(nuevaLinea)
                adaptadorLineas.notifyItemInserted(listaLineasFactura.size - 1)
                actualizarTotales()
            }
            dialog.show(supportFragmentManager, "DialogDetalleLinea")
        }
    }

    private fun actualizarTotales() {
        var subtotal = 0.0
        var total = 0.0
        
        listaLineasFactura.forEach { linea ->
            total += linea.priceUnit * linea.quantity
            subtotal += linea.priceSubtotal
        }
        
        val igv = total - subtotal
        
        binding.tvSubtotal.text = FormatterHelper.formatearMoneda(subtotal)
        binding.tvIgv.text = FormatterHelper.formatearMoneda(igv)
        binding.tvTotal.text = FormatterHelper.formatearMoneda(total)
    }

    private fun configurarSeleccionCliente() {
        binding.actvCustomer.threshold = 1
        
        // Al seleccionar un item de la lista
        binding.actvCustomer.setOnItemClickListener { _, _, position, _ ->
            if (position < listaContactosSugeridos.size) {
                val contacto = listaContactosSugeridos[position]
                val rank = if (currentMoveType == "out_invoice") contacto.customer_rank ?: 0 else contacto.supplier_rank ?: 0
                
                if (rank == 0) {
                    mostrarDialogoConfirmacionPromocion(contacto) {
                        contactoSeleccionado = contacto
                        actualizarVistaContactoSeleccionado()
                    }
                } else {
                    contactoSeleccionado = contacto
                    actualizarVistaContactoSeleccionado()
                }
            }
        }

        binding.actvCustomer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                // Si el usuario borra todo, reseteamos el contacto seleccionado
                if (s.isNullOrEmpty()) {
                    contactoSeleccionado = null
                }
                
                if (s != null && s.length >= 1 && contactoSeleccionado == null) {
                    searchJob = lifecycleScope.launch {
                        delay(600)
                        buscarContactosEnOdoo(s.toString())
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnQuickAddCustomer.setOnClickListener {
            startActivity(Intent(this, com.stackperu.odooapp.ui.contact.FormularioCliente::class.java))
        }
    }

    private fun actualizarVistaContactoSeleccionado() {
        contactoSeleccionado?.let {
            val texto = "${it.name}${if (!it.vat.isNullOrEmpty()) " - ${it.vat}" else ""}"
            binding.actvCustomer.setText(texto, false)
            binding.actvCustomer.clearFocus()
            Toast.makeText(this, "Seleccionado: ${it.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buscarContactosEnOdoo(query: String) {
        lifecycleScope.launch {
            try {
                val rankField = if (currentMoveType == "out_invoice") "customer_rank" else "supplier_rank"
                listaContactosSugeridos = com.stackperu.odooapp.data.ContactRepository.buscarContactos(
                    query = query,
                    rankField = rankField,
                    globalSearch = true
                )
                
                val nombres = listaContactosSugeridos.map { 
                    val rank = if (currentMoveType == "out_invoice") it.customer_rank ?: 0 else it.supplier_rank ?: 0
                    val prefix = if (rank == 0) "(Contacto) " else ""
                    "$prefix${it.name}${if (!it.vat.isNullOrEmpty()) " - ${it.vat}" else ""}" 
                }
                
                runOnUiThread {
                    val adapter = ArrayAdapter(this@Facturacion, android.R.layout.simple_dropdown_item_1line, nombres)
                    binding.actvCustomer.setAdapter(adapter)
                    
                    if (binding.actvCustomer.hasFocus() && query.isNotEmpty()) {
                        binding.actvCustomer.showDropDown()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OdooApp", "Error en búsqueda de contactos", e)
            }
        }
    }

    private fun mostrarDialogoConfirmacionPromocion(contacto: Contact, onConfirm: () -> Unit) {
        val tipo = if (currentMoveType == "out_invoice") "Cliente" else "Proveedor"
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Promover a $tipo")
            .setMessage("El contacto '${contacto.name}' actualmente es un contacto genérico. ¿Deseas promoverlo a $tipo para esta factura?")
            .setPositiveButton("Sí, promover") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("No, buscar otro") { _, _ ->
                binding.actvCustomer.setText("")
                contactoSeleccionado = null
            }
            .setCancelable(false)
            .show()
    }

    private fun cargarTipoCambioSunat() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.sunatApiService.getTipoCambio(com.stackperu.odooapp.AppConfig.SUNAT_TC_URL)
                if (response.isSuccessful && response.body() != null) {
                    val rawText = response.body()!!.string()
                    // Formato esperado: 12/03/2026|3.419|3.427|
                    val parts = rawText.split("|")
                    if (parts.size >= 3) {
                        val compra = parts[1]
                        val venta = parts[2]
                        runOnUiThread {
                            binding.tvExchangeRateValue.text = "Compra: $compra | Venta: $venta"
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OdooApp", "Error cargando T/C SUNAT", e)
                runOnUiThread {
                    binding.tvExchangeRateValue.text = "Error al obtener T/C"
                }
            }
        }
    }

    private fun configurarSelectorMoneda() {
        // Cargar monedas desde Odoo
        lifecycleScope.launch {
            try {
                val params = CallKwParams(
                    model = "res.currency",
                    method = "search_read",
                    args = listOf(listOf(listOf("active", "=", true))),
                    kwargs = Kwargs(fields = listOf("id", "name", "symbol"), limit = 20)
                )
                val response = RetrofitClient.apiService.executeKwListCurrency(OdooRequest(params = params))
                if (response.isSuccessful && response.body()?.result != null) {
                    monedasDisponibles = response.body()?.result!!
                    val codigos = monedasDisponibles.map { it.name }.toTypedArray()
                    val adapter = ArrayAdapter(this@Facturacion, android.R.layout.simple_dropdown_item_1line, codigos)
                    binding.actvCurrency.setAdapter(adapter)
                    
                    binding.actvCurrency.setOnItemClickListener { _, _, position, _ ->
                        monedaSeleccionada = monedasDisponibles[position]
                    }

                    // PEN por defecto
                    val pen = monedasDisponibles.find { it.name == "PEN" }
                    if (pen != null) {
                        monedaSeleccionada = pen
                        binding.actvCurrency.setText("PEN", false)
                    } else if (monedasDisponibles.isNotEmpty()) {
                        monedaSeleccionada = monedasDisponibles[0]
                        binding.actvCurrency.setText(monedasDisponibles[0].name, false)
                    }
                }
            } catch (e: Exception) {
                val codigos = arrayOf("PEN", "USD")
                val adapter = ArrayAdapter(this@Facturacion, android.R.layout.simple_dropdown_item_1line, codigos)
                binding.actvCurrency.setAdapter(adapter)
                binding.actvCurrency.setOnItemClickListener { _, _, position, _ ->
                    // Aunque falle la API, si tenemos monedas locales predecibles (id puede variar)
                    // Por ahora el fallback es limitado, pero al menos no rompe la UI
                }
                binding.actvCurrency.setText("PEN", false)
            }
        }
    }

    private fun configurarSelectorFecha() {
        // Fecha actual por defecto
        binding.tietDate.setText(FormatterHelper.formatearFecha(calendar.time))
        
        binding.tietDate.setOnClickListener {
            val dpd = DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                binding.tietDate.setText(FormatterHelper.formatearFecha(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            dpd.show()
        }
    }

    private fun configurarAcciones() {
        binding.fabEmit.setOnClickListener {
            emitirFactura()
        }
    }

    private fun emitirFactura() {
        // 1. Validaciones básicas
        val contacto = contactoSeleccionado
        if (contacto == null) {
            Toast.makeText(this, "Debe seleccionar un cliente/proveedor", Toast.LENGTH_SHORT).show()
            return
        }

        if (listaLineasFactura.isEmpty()) {
            Toast.makeText(this, "Debe agregar al menos un producto", Toast.LENGTH_SHORT).show()
            return
        }

        val diario = selectedJournal
        if (diario == null) {
            Toast.makeText(this, "Debe seleccionar un diario/serie", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Preparar datos
        lifecycleScope.launch {
            binding.fabEmit.isEnabled = false
            Toast.makeText(this@Facturacion, "Preparando envío a Odoo...", Toast.LENGTH_SHORT).show()

            try {
                // Formatear líneas para Odoo: (0, 0, { values })
                val invoiceLines = listaLineasFactura.map { line ->
                    listOf(0, 0, mapOf(
                        "product_id" to line.product?.id,
                        "quantity" to line.quantity,
                        "price_unit" to line.priceUnit,
                        "account_id" to line.account?.id,
                        "tax_ids" to listOf(listOf(6, 0, line.taxes.map { it.id }))
                    ))
                }

                // Obtener fecha en formato YYYY-MM-DD
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateStr = sdf.format(calendar.time)

                val payload = mutableMapOf<String, Any>(
                    "move_type" to currentMoveType,
                    "partner_id" to contacto.id,
                    "journal_id" to diario.id,
                    "invoice_date" to dateStr,
                    "currency_id" to (monedaSeleccionada?.id ?: 0),
                    "invoice_line_ids" to invoiceLines,
                    "narration" to binding.tietNotes.text.toString()
                )

                // Si está configurado para guardar como borrador
                if (com.stackperu.odooapp.AppConfig.SAVE_AS_DRAFT) {
                    payload["state"] = "draft"
                }

                // Agregar plazos de pago si existe
                selectedPaymentTerm?.let {
                    payload["invoice_payment_term_id"] = it.id
                }

                // Si es NC y hay referencia
                if (currentMoveType.contains("refund") || binding.toggleGroupType.checkedButtonId == R.id.btnTypeNC) {
                    val ref = binding.tietReference.text.toString()
                    if (ref.isNotEmpty()) {
                        payload["ref"] = ref
                    }
                }

                val newId = com.stackperu.odooapp.data.ProductRepository.crearFactura(payload)

                if (newId != null) {
                    Toast.makeText(this@Facturacion, "¡Factura creada con éxito (ID: $newId)!", Toast.LENGTH_LONG).show()
                    // Limpiar y salir o refrescar
                    delay(1500)
                    finish()
                } else {
                    Toast.makeText(this@Facturacion, "Error al crear la factura en Odoo", Toast.LENGTH_LONG).show()
                    binding.fabEmit.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error en flujo de emisión", e)
                Toast.makeText(this@Facturacion, "Ocurrió un error inesperado", Toast.LENGTH_SHORT).show()
                binding.fabEmit.isEnabled = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
