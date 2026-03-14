package com.stackperu.odooapp.ui.login

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.stackperu.odooapp.R
import com.stackperu.odooapp.api.RetrofitClient
import com.stackperu.odooapp.data.UserSession
import com.stackperu.odooapp.databinding.EditarPerfilBinding
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.OdooRequest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Actividad para editar la información del usuario logueado (Nombre e Imagen).
 * Se comunica con el modelo 'res.users' de Odoo vía API.
 */
class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: EditarPerfilBinding
    private var selectedImageBase64: String? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processSelectedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = UserSession.currentUser
        if (user == null) {
            finish()
            return
        }

        refreshUserInfo()

        // Eventos
        binding.btnBack.setOnClickListener { finish() }
        
        binding.fabSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etName.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateUserInOdoo(user.id, newName, selectedImageBase64)
            }
        }
    }

    private fun refreshUserInfo() {
        val user = UserSession.currentUser ?: return
        
        // Información en la barra superior
        binding.tvUserName.text = user.name
        
        // Campos editables
        binding.etName.setText(user.name)
        binding.etEmail.setText(user.email?.takeIf { it != "false" })
        
        val sessionCookie = RetrofitClient.cookieJar.getSessionCookieValue()
        val avatarUrl = "${RetrofitClient.BASE_URL}/web/image?model=res.users&id=${user.id}&field=image_128"
        
        val glideUrl = com.bumptech.glide.load.model.GlideUrl(avatarUrl, com.bumptech.glide.load.model.LazyHeaders.Builder()
            .addHeader("Cookie", "session_id=$sessionCookie")
            .build())

        // Avatar principal (grande)
        Glide.with(this)
            .load(glideUrl)
            .circleCrop()
            .skipMemoryCache(true)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .placeholder(R.drawable.avatar_placeholder)
            .into(binding.ivEditAvatar)

        // Avatar de la barra (pequeño)
        Glide.with(this)
            .load(glideUrl)
            .circleCrop()
            .skipMemoryCache(true)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .placeholder(R.drawable.avatar_placeholder)
            .into(binding.ivUserAvatar)
    }

    private fun processSelectedImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            // Redimensionar para evitar que el JSON sea demasiado pesado
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val byteArray = outputStream.toByteArray()
            
            selectedImageBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            
            Glide.with(this)
                .load(byteArray)
                .circleCrop()
                .into(binding.ivEditAvatar)
                
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Realiza la llamada 'write' al modelo res.users de Odoo.
     * Se cambió para verificar el resultado JSON-RPC real y asegurar persistencia en Postgres.
     */
    private fun updateUserInOdoo(userId: Int, name: String, imageBase64: String?) {
        binding.saveProgressBar.visibility = View.VISIBLE
        binding.btnSaveProfile.isEnabled = false

        lifecycleScope.launch {
            try {
                // Preparamos los campos a actualizar en Odoo
                val updateData = mutableMapOf<String, Any>("name" to name)
                if (imageBase64 != null) {
                    updateData["image_1920"] = imageBase64
                }

                // Estructura call_kw para el método 'write'
                val params = CallKwParams(
                    model = AppConfig.MODEL_USER,
                    method = "write",
                    args = listOf(listOf(userId), updateData)
                )
                
                // Usamos executeKw (Any) ya que 'write' devuelve un Booleano, no una lista de Contactos
                val response = RetrofitClient.apiService.executeKw(OdooRequest(params = params))
                
                if (response.isSuccessful) {
                    val odooResult = response.body()?.result
                    val odooError = response.body()?.error

                    // En Odoo, el método 'write' devuelve true si se guardó en la DB de Postgres
                    if (odooResult == true) {
                        Toast.makeText(this@EditarPerfil, "Perfil guardado en Odoo con éxito", Toast.LENGTH_SHORT).show()
                        
                        // Actualizamos la sesión local para que el cambio sea inmediato en la UI
                        UserSession.currentUser = UserSession.currentUser?.copy(name = name)
                        
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else if (odooError != null) {
                        val msg = "Error de Odoo: ${odooError.message}"
                        Log.e("OdooApp", "Debug: ${odooError.data?.debug}")
                        Toast.makeText(this@EditarPerfil, msg, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@EditarPerfil, "Odoo rechazó el cambio", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EditarPerfil, "Error de red: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al actualizar", e)
                Toast.makeText(this@EditarPerfil, "Error crítico: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.saveProgressBar.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true
            }
        }
    }
}
