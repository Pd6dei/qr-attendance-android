package com.example.qrverificationapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.qrverificationapp.R
import com.example.qrverificationapp.data.QrRequest
import com.example.qrverificationapp.data.QrResponse
import com.example.qrverificationapp.data.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty()) {
                etUsername.error = "Username required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password required"
                return@setOnClickListener
            }

            login(username, password)
        }
    }

    private fun login(username: String, password: String) {
        lifecycleScope.launch {
            try {
                val request = QrRequest(
                    userName = username,
                    password = password
                )

                val response: QrResponse =
                    RetrofitClient.api.login(request)

                if (response.status == "OK") {

                    // ✅ Save session
                    getSharedPreferences("APP_SESSION", MODE_PRIVATE)
                        .edit()
                        .putString("USER_NAME", username)
                        .apply()

                    // ✅ PASS USERNAME TO SCANNER
                    val intent =
                        Intent(this@LoginActivity, ScannerActivity::class.java)
                    intent.putExtra("USER_NAME", username)
                    startActivity(intent)

                    finish()

                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        response.message ?: "Invalid username or password",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@LoginActivity,
                    "Server not reachable. Check network or IP.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
