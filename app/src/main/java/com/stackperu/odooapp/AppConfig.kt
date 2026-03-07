package com.stackperu.odooapp

/**
 * Archivo de configuración central de la aplicación.
 * Reúne las variables globales para conectarse a Odoo 19.
 */
object AppConfig {
    /**
     * URL base de la instancia de Odoo.
     * IMPORTANTE: 10.0.2.2 es la IP virtual que usa el emulador de Android para alcanzar el localhost de tu PC.
     * Si vas a probar en un celular físico (por USB o WiFi), debes poner la IP de tu PC en la red local.
     * Ejemplo: "http://192.168.1.25:8069"
     */
    const val BASE_URL = "http://10.0.2.2:8069"

    /**
     * Nombre EXACTO de la base de datos de PostgreSQL configurada en tu Odoo 19.
     */
    const val DATABASE_NAME = "odoo_db"
}