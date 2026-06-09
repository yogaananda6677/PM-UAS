package ananda.yoga.projectuasmobile.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.R
import ananda.yoga.projectuasmobile.config.ApiConfig
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class GantiPasswordFragment : Fragment(R.layout.fragment_ganti_password) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etCurrent = view.findViewById<EditText>(R.id.etCurrentPassword)
        val etNew     = view.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave   = view.findViewById<Button>(R.id.btnSave)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnSave.setOnClickListener {
            val current = etCurrent.text.toString().trim()
            val newPass = etNew.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            when {
                current.isEmpty() || newPass.isEmpty() || confirm.isEmpty() -> {
                    Toast.makeText(requireContext(), "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                newPass.length < 8 -> {
                    Toast.makeText(requireContext(), "Password minimal 8 karakter", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                newPass != confirm -> {
                    Toast.makeText(requireContext(), "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            btnSave.isEnabled = false
            btnSave.text = "Menyimpan..."

            val sharedPref = requireActivity()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val token = sharedPref.getString("token", "")

            val params = JSONObject().apply {
                put("current_password", current)
                put("password", newPass)
                put("password_confirmation", confirm)
            }

            val request = object : JsonObjectRequest(
                Method.PUT,
                ApiConfig.UPDATE_PASSWORD,
                params,
                { response ->
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan"
                    Toast.makeText(
                        requireContext(),
                        response.optString("message", "Password berhasil diubah"),
                        Toast.LENGTH_SHORT
                    ).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                },
                { error ->
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan"
                    val errorBody = error.networkResponse?.data?.toString(Charsets.UTF_8)
                    val message = try {
                        JSONObject(errorBody ?: "").optString("message", "Gagal mengubah password")
                    } catch (e: Exception) {
                        "Gagal terhubung ke server"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return hashMapOf(
                        "Accept"        to "application/json",
                        "Content-Type"  to "application/json",
                        "Authorization" to "Bearer $token"
                    )
                }
            }

            Volley.newRequestQueue(requireContext()).add(request)
        }
    }
}