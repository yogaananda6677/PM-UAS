package ananda.yoga.projectuasmobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityLoginBinding
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnLogin.setOnClickListener { doLogin() }

        b.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun doLogin() {
        val username = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username dan password wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnLogin.isEnabled = false
        b.btnLogin.text = "Memuat..."

        val params = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            ApiConfig.LOGIN,
            params,
            { response ->
                b.btnLogin.isEnabled = true
                b.btnLogin.text = "Masuk"

                // Ambil token & data user dari response
                val token = response.optString("token", "")
                val user  = response.optJSONObject("user")

                if (token.isNotEmpty() && user != null) {
                    val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putBoolean("is_login", true)
                        putString("token", token)
                        putString("id_user",  user.optString("id_user"))
                        putString("nama",     user.optString("name"))
                        putString("username", user.optString("username"))
                        putString("email",    user.optString("email"))
                        putString("role",     user.optString("role"))
                        apply()
                    }

                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(this, "Login gagal, coba lagi", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                b.btnLogin.isEnabled = true
                b.btnLogin.text = "Masuk"

                // Parse error message dari Laravel
                val errorBody = error.networkResponse?.data?.toString(Charsets.UTF_8)
                val message = try {
                    JSONObject(errorBody ?: "").optString("message", "Login gagal")
                } catch (e: Exception) {
                    "Gagal terhubung ke server"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        ) {
            // Tambah header Content-Type dan Accept untuk Laravel
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