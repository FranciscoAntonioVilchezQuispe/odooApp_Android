package com.stackperu.odooapp.data

import com.stackperu.odooapp.model.User

/**
 * Gestor de sesión simple en memoria para almacenar los detalles del usuario actual.
 */
object UserSession {
    var currentUser: User? = null

    /**
     * Limpia la sesión del usuario al hacer logout.
     */
    fun clear() {
        currentUser = null
    }
}