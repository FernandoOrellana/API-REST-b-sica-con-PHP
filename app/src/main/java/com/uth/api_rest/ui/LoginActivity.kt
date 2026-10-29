package com.uth.api_rest.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import com.uth.api_rest.MainActivity
import com.uth.api_rest.databinding.ActivityLoginBinding
import com.uth.api_rest.data.model.Resource
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya hay sesión activa, ir directo al menú principal
        if (viewModel.haySesion()) {
            irAMain()
            return
        }

        binding.btnLogin.setOnClickListener {
            val usuario = (binding.etUsuario.text?.toString() ?: "").trim()
            val password = (binding.etPassword.text?.toString() ?: "").trim()

            if (usuario.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Ingresa usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(usuario, password)
        }

        viewModel.loginState.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                    irAMain()
                }
                is Resource.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun irAMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
