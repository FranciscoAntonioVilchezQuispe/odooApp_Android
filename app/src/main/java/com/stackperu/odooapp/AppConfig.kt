package com.stackperu.odooapp

/**
 * Archivo de configuración central de la aplicación.
 * Reúne las variables globales para conectarse a Odoo 19 y reglas de negocio.
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
     */
    const val SAVE_AS_DRAFT = true

    // --- CONFIGURACIÓN DE RED ---
    const val NETWORK_TIMEOUT_SECONDS = 60L
    const val ENABLE_NETWORK_LOGS = true // Cambiar a false en versiones de producción (release)

    // --- CONFIGURACIÓN DE LOCALIZACIÓN Y ODOO ---
    const val DEFAULT_LANGUAGE_CODE = "es_PE"
    const val DEFAULT_COUNTRY_CODE = "PE"
    const val DEFAULT_CURRENCY_CODE = "PEN"
    
    /**
     * Umbral para detracciones en Perú (S/ 700.00).
     */
    const val DETRACTION_THRESHOLD = 700.0

    // --- LÍMITES DE PAGINACIÓN ---
    const val DEFAULT_PAGE_LIMIT = 50
    const val SEARCH_RESULTS_LIMIT = 20

    // --- NOMBRES TÉCNICOS DE MODELOS ODOO 19 ---
    const val MODEL_INVOICE = "account.move"
    const val MODEL_PARTNER = "res.partner"
    const val MODEL_PRODUCT = "product.product"
    const val MODEL_DETRACTION = "l10n_pe.withhold.type"
    const val MODEL_JOURNAL = "account.journal"
    const val MODEL_PAYMENT_TERM = "account.payment.term"
    const val MODEL_ACCOUNT = "account.account"
    const val MODEL_TAX = "account.tax"
    const val MODEL_USER = "res.users"
    const val MODEL_CURRENCY = "res.currency"
    const val MODEL_UOM = "uom.uom"
    const val MODEL_IDENTIFICATION_TYPE = "l10n_latam.identification.type"
    const val MODEL_STATE = "res.country.state"
    const val MODEL_CITY = "res.city"
    const val MODEL_DISTRICT = "l10n_pe.res.city.district"

    // --- MAGICS STRINGS DE FACTURACIÓN Y NEGOCIO ---
    const val INVOICE_TYPE_SALE = "out_invoice"
    const val INVOICE_TYPE_PURCHASE = "in_invoice"
    
    /**
     * Porcentaje de IGV general en Perú usado como Default
     */
    const val DEFAULT_TAX_PERCENTAGE = 18.0
}
