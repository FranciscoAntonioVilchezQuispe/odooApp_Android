package com.stackperu.odooapp.model

/**
 * Modelos de respuesta para servicios de consulta de identidad (DNI/RUC).
 */

data class DniResponse(
    val dni: String,
    val nombres: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val nombreCompleto: String
)

data class RucResponse(
    val ruc: String,
    val razonSocial: String,
    val estado: String,
    val condicion: String,
    val direccion: String,
    val ubigeo: String,
    val departamento: String,
    val provincia: String,
    val distrito: String
)
