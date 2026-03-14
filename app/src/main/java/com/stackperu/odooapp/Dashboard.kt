package com.stackperu.odooapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.data.UserSession
import com.stackperu.odooapp.databinding.DashboardBinding
import com.stackperu.odooapp.ui.contact.ListaContactos
import com.stackperu.odooapp.ui.invoice.Facturacion
import com.stackperu.odooapp.ui.login.EditarPerfil
import com.stackperu.odooapp.ui.login.Login
import kotlinx.coroutines.launch

/**
 * MainActivity funciona como el Dashboard principal con accesos rápidos.
 */
class Dashboard : AppCompatActivity() {

    private lateinit var binding: DashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (UserSession.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        binding = DashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBarraSuperior()
        configurarAcciones()
    }

    private fun configurarBarraSuperior() {
        actualizarInfoUsuario()
        binding.ivUserAvatar.setOnClickListener { mostrarMenuPerfilUsuario() }
    }

    private fun configurarAcciones() {
        binding.cardClients.setOnClickListener {
            startActivity(Intent(this, com.stackperu.odooapp.ui.contact.ListaClientes::class.java))
        }

        binding.cardNewProduct.setOnClickListener {
            startActivity(Intent(this, com.stackperu.odooapp.ui.invoice.ListaProductos::class.java))
        }

        binding.cardAgenda.setOnClickListener {
            startActivity(Intent(this, com.stackperu.odooapp.ui.contact.ListaContactos::class.java))
        }

        binding.cardSale.setOnClickListener {
            val intent = Intent(this, Facturacion::class.java)
            intent.putExtra("MOVE_TYPE", AppConfig.INVOICE_TYPE_SALE)
            startActivity(intent)
        }

        binding.cardPurchase.setOnClickListener {
            val intent = Intent(this, Facturacion::class.java)
            intent.putExtra("MOVE_TYPE", "in_invoice")
            startActivity(intent)
        }
    }

    private fun actualizarInfoUsuario() {
        val usuario = UserSession.currentUser ?: return
        binding.tvUserName.text = usuario.name
        val sessionCookie = RetrofitClient.cookieJar.getSessionCookieValue()
        if (sessionCookie.isNotEmpty()) {
            val avatarUrl = "${RetrofitClient.BASE_URL}/web/image?model=res.users&id=${usuario.id}&field=image_128"
            Glide.with(this).load(GlideUrl(avatarUrl, LazyHeaders.Builder().addHeader("Cookie", "session_id=$sessionCookie").build()))
                .circleCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .placeholder(R.drawable.avatar_placeholder).into(binding.ivUserAvatar)
        }
    }

    private fun mostrarMenuPerfilUsuario() {
        val usuario = UserSession.currentUser ?: return
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialogo_perfil_usuario)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val ivAvatar = dialog.findViewById<ImageView>(R.id.ivUserAvatarDialog)
        dialog.findViewById<TextView>(R.id.tvUserNameDialog).text = usuario.name
        dialog.findViewById<TextView>(R.id.tvUserEmailDialog).text = usuario.email?.takeIf { it != "false" } ?: ""
        
        val sessionCookie = RetrofitClient.cookieJar.getSessionCookieValue()
        val avatarUrl = "${RetrofitClient.BASE_URL}/web/image?model=res.users&id=${usuario.id}&field=image_128"
        Glide.with(this).load(GlideUrl(avatarUrl, LazyHeaders.Builder().addHeader("Cookie", "session_id=$sessionCookie").build()))
            .circleCrop().placeholder(R.drawable.avatar_placeholder).into(ivAvatar)

        dialog.findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, EditarPerfil::class.java))
        }

        dialog.findViewById<View>(R.id.btnLogoutDialog).setOnClickListener {
            dialog.dismiss()
            cerrarSesion()
        }
        dialog.show()
    }

    private fun cerrarSesion() {
        lifecycleScope.launch {
            try { RetrofitClient.apiService.logout() } catch (e: Exception) {}
            UserSession.clear()
            RetrofitClient.cookieJar.clearSession()
            val intent = Intent(this@Dashboard, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarInfoUsuario()
    }
}
