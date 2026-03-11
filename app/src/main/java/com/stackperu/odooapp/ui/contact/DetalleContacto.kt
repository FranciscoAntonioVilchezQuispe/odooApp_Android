package com.stackperu.odooapp.ui.contact

import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.stackperu.odooapp.R
import com.stackperu.odooapp.databinding.DetalleContactoBinding
import com.stackperu.odooapp.model.Contact

/**
 * Actividad que funciona como vista Formulario (Form View) para mostrar
 * el detalle en profundidad de un solo contacto.
 */
class DetalleContacto : AppCompatActivity() {

    private lateinit var binding: DetalleContactoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetalleContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtenemos el objeto Contacto enviado desde el MainActivity
        val contact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("CONTACT", Contact::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("CONTACT") as? Contact
        }
        
        // Acción para volver atrás
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Llenamos la información en el formulario
        contact?.let {
            binding.etName.setText(it.name)
            binding.etEmail.setText(it.email ?: "No definido")
            binding.etPhone.setText(it.phone ?: "No definido")
            binding.etVat.setText(it.vat ?: "No definido")
            binding.etAddress.setText(it.address ?: "No definido")

            // Procesar y mostrar la imagen en Base64
            val base64Image = it.avatarBase64
            if (!base64Image.isNullOrEmpty()) {
                try {
                    val imageByteArray = Base64.decode(base64Image, Base64.DEFAULT)
                    Glide.with(this)
                        .asBitmap()
                        .load(imageByteArray)
                        .circleCrop()
                        .placeholder(R.drawable.avatar_placeholder)
                        .into(binding.ivAvatar)
                } catch (e: Exception) {
                    Glide.with(this)
                        .load(R.drawable.avatar_placeholder)
                        .circleCrop()
                        .into(binding.ivAvatar)
                }
            } else {
                Glide.with(this)
                    .load(R.drawable.avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivAvatar)
            }
        }
    }
}