package com.stackperu.odooapp.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/**
 * FormatterHelper centraliza el formateo de datos siguiendo los estándares de Perú (es-PE).
 */
object FormatterHelper {

    private val localePeru = Locale("es", "PE")

    /**
     * Formatea un valor double a la moneda nacional del Perú (Soles - S/).
     */
    fun formatearMoneda(monto: Double): String {
        val formato = NumberFormat.getCurrencyInstance(localePeru)
        // Odoo suele omitir el símbolo si usamos solo currency, forzamos S/ si es necesario
        return formato.format(monto).replace("$", "S/ ")
    }

    /**
     * Formatea una fecha al estándar peruano: día/mes/año.
     */
    fun formatearFecha(fecha: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", localePeru)
        return sdf.format(fecha)
    }

    /**
     * Formatea una fecha en formato string (YYYY-MM-DD de Odoo) al estándar peruano.
     */
    fun formatearFechaOdoo(fechaString: String?): String {
        if (fechaString.isNullOrEmpty()) return ""
        return try {
            val formatoOdoo = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val fecha = formatoOdoo.parse(fechaString)
            if (fecha != null) formatearFecha(fecha) else fechaString
        } catch (e: Exception) {
            fechaString
        }
    }

    /**
     * Formatea números decimales con el estándar local (punto decimal, coma de miles).
     */
    fun formatearDecimal(numero: Double): String {
        val formato = NumberFormat.getNumberInstance(localePeru)
        formato.minimumFractionDigits = 2
        formato.maximumFractionDigits = 2
        return formato.format(numero)
    }
}
