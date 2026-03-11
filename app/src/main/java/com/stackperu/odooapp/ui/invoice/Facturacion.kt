package com.stackperu.odooapp.ui.invoice

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.stackperu.odooapp.R
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.data.UserSession
import com.stackperu.odooapp.databinding.FacturacionBinding
import com.stackperu.odooapp.model.invoice.InvoiceLine
import com.stackperu.odooapp.utils.FormatterHelper

/**
 * InvoiceActivity es la maqueta visual para la creación de comprobantes electrónicos.
 * Sigue la paleta de colores de Odoo 19 y permite simular la selección de clientes y tipos.
 */
class Facturacion : AppCompatActivity() {

    private lateinit var binding: FacturacionBinding
    private var currentMoveType = "out_invoice" // Por defecto: Venta (Customer Invoice)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FacturacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setupToolbar() // Removed as per instruction
        currentMoveType = intent.getStringExtra("MOVE_TYPE") ?: "out_invoice"

        configurarTipoComprobante()
        configurarTipoOperacion()
        configurarLineasFactura()
        configurarSeleccionCliente()
        configurarAcciones()
        actualizarInfoUsuario()

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
            binding.tvCustomerLabel.text = getString(R.string.name_label) // Nombre / Razón Social
            binding.actvCustomer.hint = "Buscar Cliente (RUC/Nombre)"
            binding.btnQuickAddCustomer.text = "+ Nuevo Cliente"
        } else {
            binding.toggleOperationType.check(R.id.btnTypePurchase)
            binding.tvAppTitle.text = "Nueva Compra"
            binding.tvCustomerLabel.text = "Información del Proveedor"
            binding.actvCustomer.hint = "Buscar Proveedor (RUC/Nombre)"
            binding.btnQuickAddCustomer.text = "+ Nuevo Proveedor"
        }
    }

    private fun configurarTipoComprobante() {
        binding.toggleGroupType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnTypeFact -> {
                        Toast.makeText(this, "Tipo: Factura Electrónica", Toast.LENGTH_SHORT).show()
                    }
                    R.id.btnTypeBol -> {
                        Toast.makeText(this, "Tipo: Boleta de Venta", Toast.LENGTH_SHORT).show()
                    }
                    R.id.btnTypeNC -> {
                        Toast.makeText(this, "Tipo: Nota de Crédito", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun configurarLineasFactura() {
        val lineasDemo = listOf(
            InvoiceLine(1, "Suscripción Odoo Enterprise 1 Año", 1.0, 1200.0),
            InvoiceLine(2, "Servicio de Implementación Local", 5.0, 150.0)
        )
        
        binding.rvInvoiceLines.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvInvoiceLines.adapter = AdaptadorLineaFactura(lineasDemo)
        
        // Calcular totales usando FormatterHelper
        val subtotal = lineasDemo.sumOf { it.subtotal }
        val total = lineasDemo.sumOf { it.total }
        val igv = total - subtotal
        
        binding.tvSubtotal.text = FormatterHelper.formatearMoneda(subtotal)
        binding.tvIgv.text = FormatterHelper.formatearMoneda(igv)
        binding.tvTotal.text = FormatterHelper.formatearMoneda(total)
    }

    private fun configurarSeleccionCliente() {
        val clientesDemo = arrayOf("STACK PERU PARTNERS S.A.C - 20606123456", "JUAN PEREZ - 10456789012", "CLIENTES VARIOS - 0")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, clientesDemo)
        binding.actvCustomer.setAdapter(adapter)

        binding.btnQuickAddCustomer.setOnClickListener {
            Toast.makeText(this, "Abrir Wizard: Nuevo Cliente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarAcciones() {
        binding.btnAddProduct.setOnClickListener {
            Toast.makeText(this, "Abrir Buscador de Productos", Toast.LENGTH_SHORT).show()
        }

        binding.fabEmit.setOnClickListener {
            Toast.makeText(this, "Emitiendo Comprobante a SUNAT...", Toast.LENGTH_LONG).show()
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
