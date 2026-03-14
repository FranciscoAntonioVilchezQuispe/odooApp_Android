package com.stackperu.odooapp

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatDelegate

/**
 * Clase Application para configurar comportamientos globales de la app.
 * Implementa un modo de pantalla completa inmersivo para todas las actividades.
 */
class OdooApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Forzar el modo claro (Light Mode) en toda la aplicación.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // 2. Registrar un listener global para aplicar pantalla completa a todas las pantallas
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Solo configuramos que el contenido se extienda detrás de las barras aquí
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.window.setDecorFitsSystemWindows(false)
                }
            }

            override fun onActivityStarted(activity: Activity) {
                // Aplicamos el ocultamiento cuando la actividad inicia para evitar el NullPointerException
                aplicarPantallaCompleta(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                aplicarPantallaCompleta(activity)
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * Configura la actividad para que oculte la barra de estado y de navegación
     * de forma persistente (Modo Immersivo).
     */
    private fun aplicarPantallaCompleta(activity: Activity) {
        val window = activity.window ?: return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
            }
        } catch (e: Exception) {
            // Silencioso: si falla en alguna actividad específica, no cerramos la app
        }
    }
}
