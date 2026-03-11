package com.stackperu.odooapp.model.invoice

import java.io.Serializable

data class InvoiceLine(
    val id: Int,
    val productTitle: String,
    val quantity: Double,
    val priceUnit: Double,
    val taxDescription: String = "IGV 18%"
) : Serializable {
    val total: Double get() = quantity * priceUnit * 1.18
    val subtotal: Double get() = quantity * priceUnit
}
