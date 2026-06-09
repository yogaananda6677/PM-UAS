package ananda.yoga.projectuasmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityRegisterBinding
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnRegister.setOnClickListener { doRegister() }

        b.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun doRegister() {
        val name     = b.etName.text.toString().trim()
        val username = b.etUsername.text.toString().trim()
        val email    = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString().trim()
        val confirm  = b.etPasswordConfirm.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 8) {
            Toast.makeText(this, "Password minimal 8 karakter", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirm) {
            Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnRegister.isEnabled = false
        b.btnRegister.text = "Mendaftar..."

        val params = JSONObject().apply {
            put("name", name)
            put("username", username)
            put("email", email)
            put("role", "pelanggan")
            put("password", password)
            put("password_confirmation", confirm)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            ApiConfig.REGISTER,
            params,
            { response ->
                b.btnRegister.isEnabled = true
                b.btnRegister.text = "Daftar"

                Toast.makeText(this, "Registrasi berhasil! Silakan login.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            },
            { error ->
                b.btnRegister.isEnabled = true
                b.btnRegister.text = "Daftar"

                val errorBody = error.networkResponse?.data?.toString(Charsets.UTF_8)
                val message = try {
                    val json = JSONObject(errorBody ?: "")
                    val errors = json.optJSONObject("errors")
                    if (errors != null) {
                        val firstKey = errors.keys().next()
                        errors.getJSONArray(firstKey).getString(0)
                    } else {
                        json.optString("message", "Registrasi gagal")
                    }
                } catch (e: Exception) {
                    "Gagal terhubung ke server"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Accept"       to "application/json",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}