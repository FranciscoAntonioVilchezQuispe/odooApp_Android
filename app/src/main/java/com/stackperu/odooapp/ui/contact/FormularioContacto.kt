package com.stackperu.odooapp.ui.contact

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import com.stackperu.odooapp.databinding.FormularioContactoBinding
import com.stackperu.odooapp.model.CallKwParams
import com.stackperu.odooapp.model.Contact
import com.stackperu.odooapp.model.OdooRequest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Actividad para crear o editar contactos en Odoo 19.
 */
class FormularioContacto : AppCompatActivity() {

    private lateinit var binding: FormularioContactoBinding
    private var contact: Contact? = null
    private var selectedImageBase64: String? = null

    private val lanzadorSelectorImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { procesarImagenSeleccionada(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FormularioContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtenemos el contacto si estamos en modo edición
        contact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("CONTACT", Contact::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("CONTACT") as? Contact
        }

        configurarInterfaz()
        actualizarInfoUsuario()
    }

    private fun configurarInterfaz() {
        if (contact != null) {
            binding.tvTitle.text = "Editar Contacto"
            binding.etName.setText(contact?.name)
            binding.etEmail.setText(contact?.email)
            binding.etPhone.setText(contact?.phone)
            binding.etVat.setText(contact?.vat)

            // Cargar imagen actual
            val base64 = contact?.avatarBase64
            if (!base64.isNullOrEmpty()) {
                val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                Glide.with(this).load(imageBytes).circleCrop().into(binding.ivContactAvatar)
            } else {
                // Si no hay Base64 (porque optimizamos el listado), cargamos por URL
                val sessionCookie = RetrofitClient.cookieJar.getSessionCookieValue()
                if (sessionCookie.isNotEmpty()) {
                    val avatarUrl = "${RetrofitClient.BASE_URL}/web/image?model=res.partner&id=${contact?.id}&field=image_128"
                    Glide.with(this).load(com.bumptech.glide.load.model.GlideUrl(avatarUrl, com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("Cookie", "session_id=$sessionCookie")
                        .build()))
                        .circleCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.avatar_placeholder)
                        .into(binding.ivContactAvatar)
                }
            }
        } else {
            binding.tvTitle.text = "Nuevo Contacto"
        }

        binding.btnBack.setOnClickListener { finish() }
        
        binding.fabSelectContactImage.setOnClickListener {
            lanzadorSelectorImagen.launch("image/*")
        }

        binding.btnSaveContact.setOnClickListener {
            guardarContacto()
        }
    }

    private fun procesarImagenSeleccionada(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val byteArray = outputStream.toByteArray()
            selectedImageBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            
            Glide.with(this).load(byteArray).circleCrop().into(binding.ivContactAvatar)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
        }
    }
    private fun guardarContacto() {
        val nombre = binding.etName.text.toString().trim()
        val correo = binding.etEmail.text.toString().trim()
        val telefono = binding.etPhone.text.toString().trim()
        val documento = binding.etVat.text.toString().trim()

        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        binding.saveProgressBar.visibility = View.VISIBLE
        binding.btnSaveContact.isEnabled = false

        lifecycleScope.launch {
            try {
                val data = mutableMapOf<String, Any>(
                    "name" to nombre,
                    "email" to correo,
                    "phone" to telefono,
                    "vat" to documento
                )
                if (selectedImageBase64 != null) {
                    data["image_1920"] = selectedImageBase64!!
                }

                val params: CallKwParams
                if (contact != null) {
                    // MODO EDICIÓN: método 'write'
                    params = CallKwParams(
                        model = "res.partner",
                        method = "write",
                        args = listOf(listOf(contact!!.id), data)
                    )
                } else {
                    // MODO CREACIÓN: método 'create'
                    params = CallKwParams(
                        model = "res.partner",
                        method = "create",
                        args = listOf(data)
                    )
                }

                val response = RetrofitClient.apiService.executeKw(OdooRequest(params = params))
                val odooResponse = response.body()

                if (response.isSuccessful && odooResponse != null) {
                    if (odooResponse.error != null) {
                        // Error detallado devuelto por Odoo
                        val errorMsg = odooResponse.error.message
                        val debugInfo = odooResponse.error.data?.debug ?: ""
                        Log.e("OdooApp", "Error de Odoo: $errorMsg\nDebug: $debugInfo")
                        
                        val displayMsg = if (debugInfo.isNotEmpty()) {
                            // Si hay rastro de error largo, solo mostramos el inicio o el mensaje base
                            "$errorMsg: ${debugInfo.take(100)}..."
                        } else {
                            errorMsg
                        }
                        Toast.makeText(this@FormularioContacto, "Error de Odoo: $displayMsg", Toast.LENGTH_LONG).show()
                    } else {
                        // Éxito: Verificamos el resultado según el método
                        val result = odooResponse.result
                        val isSuccess = if (contact != null) {
                            // 'write' devuelve true en éxito
                            result == true
                        } else {
                            // 'create' devuelve el ID (Int o Long)
                            result != null && (result is Number)
                        }

                        if (isSuccess) {
                            Toast.makeText(this@FormularioContacto, "Contacto guardado con éxito", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@FormularioContacto, "No se pudo confirmar el guardado", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@FormularioContacto, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OdooApp", "Error al guardar contacto", e)
                Toast.makeText(this@FormularioContacto, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.saveProgressBar.visibility = View.GONE
                binding.btnSaveContact.isEnabled = true
            }
        }
    }

    private fun actualizarInfoUsuario() {
        val usuario = com.stackperu.odooapp.data.UserSession.currentUser ?: return
        binding.tvUserNameSmall.text = usuario.name
        val sessionCookie = RetrofitClient.cookieJar.getSessionCookieValue()
        if (sessionCookie.isNotEmpty()) {
            val avatarUrl = "${RetrofitClient.BASE_URL}/web/image?model=res.users&id=${usuario.id}&field=image_128"
            Glide.with(this).load(com.bumptech.glide.load.model.GlideUrl(avatarUrl, com.bumptech.glide.load.model.LazyHeaders.Builder()
                .addHeader("Cookie", "session_id=$sessionCookie")
                .build()))
                .circleCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .placeholder(R.drawable.avatar_placeholder)
                .into(binding.ivUserAvatarSmall)
        }
    }
}