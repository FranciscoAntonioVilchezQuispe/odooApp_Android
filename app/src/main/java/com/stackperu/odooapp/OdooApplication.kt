package com.stackperu.odooapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

/**
 * Clase Application para configurar comportamientos globales de la app.
 */
class OdooApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Forzar el modo claro (Light Mode) en toda la aplicación.
        // Esto evita que el "Modo Oscuro" del sistema (especialmente en Honor/Huawei) 
        // altere los colores de fondo y texto.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // 2. Asegurarnos de que NO se apliquen colores dinámicos (Material You).
        // Aunque no se llame a DynamicColors.applyToActivitiesIfAvailable, algunos dispositivos
        // intentan forzarlo si detectan un tema Material 3.
        // Si en algún momento quisieras activarlo, usarías: DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
