package com.stackperu.odooapp

/**
 * Archivo de configuración central de la aplicación.
 * Reúne las variables globales para conectarse a Odoo 19.
 */
object AppConfig {
    /**
     * URL base de la instancia de Odoo de producción.
     */
    const val BASE_URL = "https://intranet.stackperupartners.com"

    /**
     * Nombre de la base de datos de Odoo.
     */
    const val DATABASE_NAME = "STACK"

    /**
     * Configuración para Consulta de Identidad (SUNAT/RENIEC).
     */
    const val IDENTITY_API_URL = "https://api.apisperu.com/v1/"
    const val IDENTITY_API_TOKEN = "" // EL USUARIO DEBE PROVEER ESTE TOKEN

    /**
     * URL para tipo de cambio SUNAT (Formato TXT).
     */
    const val SUNAT_TC_URL = "https://www.sunat.gob.pe/a/txt/tipoCambio.txt"

    /**
     * Define si las facturas se guardan como borrador ('draft') al ser creadas desde la app.
     * En PRD, se recomienda dejar en true para validación previa antes de confirmar en Odoo.
     * Si es false, Odoo aplicará su comportamiento por defecto según la configuración del diario.
     */
    const val SAVE_AS_DRAFT = true
}