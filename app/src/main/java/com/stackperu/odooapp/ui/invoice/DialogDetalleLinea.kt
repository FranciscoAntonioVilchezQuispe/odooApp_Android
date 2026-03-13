package com.stackperu.odooapp.ui.invoice

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stackperu.odooapp.databinding.DialogDetalleLineaBinding
import com.stackperu.odooapp.data.ProductRepository
import com.stackperu.odooapp.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior

class DialogDetalleLinea(
    private val isSale: Boolean,
    private val onLineAdded: (InvoiceLine) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogDetalleLineaBinding? = null
    private val binding get() = _binding!!

    private var selectedProduct: Product? = null
    private var selectedAccount: Account? = null
    private var selectedAnalytic: AnalyticAccount? = null
    private var selectedUom: Uom? = null
    private var selectedTaxes: List<Tax> = emptyList()

    private var searchJob: Job? = null
    private var suggestedProducts: List<Product> = emptyList()
    private var suggestedAccounts: List<Account> = emptyList()
    private var suggestedAnalytics: List<AnalyticAccount> = emptyList()

    private var allTaxes: List<Tax> = emptyList()
    private var allUoms: List<Uom> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ajustar ventana para que el teclado no cubra el contenido
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogDetalleLineaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hacer que el BottomSheet aparezca expandido por defecto
        val behavior = BottomSheetBehavior.from(view.parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        
        setupListeners()
        loadInitialData()
    }

    private fun setupListeners() {
        // Búsqueda de Productos
        binding.actvProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (selectedProduct != null && s.toString() != selectedProduct?.name) {
                    selectedProduct = null // Reset si el usuario borra/cambia
                }
                searchJob?.cancel()
                if (s != null && s.length >= 2 && selectedProduct == null) {
                    searchJob = lifecycleScope.launch {
                        delay(500)
                        val results = ProductRepository.buscarProductos(s.toString())
                        suggestedProducts = results
                        actualizarResultadosProductos(results)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.actvProduct.setOnItemClickListener { _, _, position, _ ->
            selectedProduct = suggestedProducts[position]
            actualizarCamposDesdeProducto(selectedProduct!!)
            calcularTotal()
        }

        binding.btnQuickAddProduct.setOnClickListener {
            val intent = android.content.Intent(requireContext(), FormularioProducto::class.java)
            startActivity(intent)
        }

        // Búsqueda de Cuentas
        binding.actvAccount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (selectedAccount != null && !s.toString().contains(selectedAccount!!.name)) {
                     selectedAccount = null
                }
                if (s != null && s.length >= 2 && selectedAccount == null) {
                    lifecycleScope.launch {
                        val results = ProductRepository.obtenerCuentas(s.toString())
                        suggestedAccounts = results
                        actualizarResultadosCuentas(results)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.actvAccount.setOnItemClickListener { _, _, position, _ ->
            selectedAccount = suggestedAccounts[position]
        }

        // Cálculos dinámicos
        binding.tietQuantity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { calcularTotal() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tietPrice.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { calcularTotal() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnCancel.setOnClickListener { dismiss() }
        
        binding.btnAddLine.setOnClickListener {
            val line = validarYCrearLinea()
            if (line != null) {
                onLineAdded(line)
                dismiss()
            }
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            // Cargar UdMs
            allUoms = ProductRepository.obtenerUdMs()
            val uomNames = allUoms.map { it.name }
            binding.actvUom.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, uomNames))
            binding.actvUom.setOnItemClickListener { _, _, position, _ ->
                selectedUom = allUoms[position]
            }
            // Seleccionar UNIDAD por defecto si existe
            val defaultUom = allUoms.find { it.name.lowercase().contains("unidad") } ?: allUoms.firstOrNull()
            if (defaultUom != null) {
                selectedUom = defaultUom
                binding.actvUom.setText(defaultUom.name, false)
            }

            // Cargar Impuestos
            val taxes = ProductRepository.obtenerImpuestos()
            allTaxes = taxes
            val taxNames = taxes.map { it.name }
            binding.actvTaxes.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, taxNames))
            binding.actvTaxes.setOnItemClickListener { _, _, position, _ ->
                selectedTaxes = listOf(taxes[position])
                calcularTotal()
            }

            // Seleccionar IGV 18% por defecto
            val igv18 = taxes.find { it.name.contains("18") || (it.amount ?: 0.0) == 18.0 }
            if (igv18 != null) {
                selectedTaxes = listOf(igv18)
                binding.actvTaxes.setText(igv18.name, false)
                calcularTotal()
            }

            // Cargar Cuenta por Defecto (7012100) si es venta
            if (isSale) {
                val defaultAccount = ProductRepository.buscarCuentaPorCodigo("7012100")
                if (defaultAccount != null) {
                    selectedAccount = defaultAccount
                    binding.actvAccount.setText("[${defaultAccount.code}] ${defaultAccount.name}", false)
                }
            }
        }
    }

    private fun actualizarResultadosProductos(productos: List<Product>) {
        val nombres = productos.map { 
            val ref = if (!it.default_code.isNullOrEmpty()) "[${it.default_code}] " else ""
            "$ref${it.name}"
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
        binding.actvProduct.setAdapter(adapter)
        binding.actvProduct.showDropDown()
    }

    private fun actualizarResultadosCuentas(cuentas: List<Account>) {
        val nombres = cuentas.map { "[${it.code}] ${it.name}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
        binding.actvAccount.setAdapter(adapter)
        binding.actvAccount.showDropDown()
    }

    private fun actualizarCamposDesdeProducto(product: Product) {
        binding.tietPrice.setText(product.lst_price?.toString() ?: "0.00")
        binding.actvProduct.setText(product.name, false)
        
        // Mapear cuenta desde el producto
        val rawAccount = if (isSale) product.property_account_income_id else product.property_account_expense_id
        
        if (rawAccount is List<*> && rawAccount.size >= 2) {
            val accountId = (rawAccount[0] as? Number)?.toInt() ?: 0
            val accountName = rawAccount[1] as? String ?: ""
            
            // Intentar extraer el código del nombre si Odoo lo manda junto, ej: "[7012100] Ventas"
            selectedAccount = Account(id = accountId, name = accountName, code = "")
            binding.actvAccount.setText(accountName, false)
        } else {
            // Si el producto no tiene cuenta, podríamos cargar una por defecto según el tipo
            // Pero de momento lo dejamos vacío para que el usuario elija
        }
    }

    private fun calcularTotal() {
        val qty = binding.tietQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val price = binding.tietPrice.text.toString().toDoubleOrNull() ?: 0.0
        val total = qty * price
        
        binding.tvLineTotal.text = "S/ %.2f".format(total)
    }

    private fun validarYCrearLinea(): InvoiceLine? {
        if (selectedProduct == null) {
            Toast.makeText(context, "Debe seleccionar un producto", Toast.LENGTH_SHORT).show()
            return null
        }
        
        val qty = binding.tietQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val price = binding.tietPrice.text.toString().toDoubleOrNull() ?: 0.0

        if (qty <= 0) {
            Toast.makeText(context, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show()
            return null
        }

        val total = qty * price
        val taxRate = selectedTaxes.sumOf { it.amount ?: 0.0 }
        val priceSubtotal = if (taxRate > 0) total / (1.0 + (taxRate / 100.0)) else total

        return InvoiceLine(
            product = selectedProduct,
            account = selectedAccount,
            analyticAccount = selectedAnalytic,
            quantity = qty,
            uom = selectedUom,
            priceUnit = price,
            taxes = selectedTaxes,
            priceSubtotal = priceSubtotal
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
