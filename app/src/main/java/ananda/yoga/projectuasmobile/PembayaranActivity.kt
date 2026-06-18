package ananda.yoga.projectuasmobile

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityPembayaranBinding
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class PembayaranActivity : AppCompatActivity() {

    private lateinit var b: ActivityPembayaranBinding

    private lateinit var token: String

    private var idTransaksi = ""
    private var nomorPs = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityPembayaranBinding.inflate(layoutInflater)
        setContentView(b.root)

        val pref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        token = pref.getString("token", "") ?: ""

        idTransaksi = intent.getStringExtra("id_transaksi") ?: ""
        nomorPs = intent.getStringExtra("nomor_ps") ?: ""

        b.tvInfoPembayaran.text =
            "Playstation : $nomorPs\n\nID Transaksi : $idTransaksi"

        b.btnBayar.setOnClickListener {
            bayarCash()
        }

        b.btnKembali.setOnClickListener {
            finish()
        }
    }

    private fun bayarCash() {

        b.btnBayar.isEnabled = false
        b.btnBayar.text = "Memproses..."

        val body = JSONObject()
        body.put("metode_pembayaran", "cash")

        val request = object : JsonObjectRequest(
            Method.PATCH,
            ApiConfig.BAYAR_TRANSAKSI(idTransaksi),
            body,

            { response ->

                b.tvStatus.text = "MENUNGGU VALIDASI"
                b.tvStatus.setTextColor(
                    getColor(android.R.color.holo_orange_dark)
                )

                b.btnBayar.isEnabled = false
                b.btnBayar.text = "Menunggu Validasi"

                Toast.makeText(
                    this,
                    response.optString(
                        "message",
                        "Silakan lakukan pembayaran ke kasir."
                    ),
                    Toast.LENGTH_LONG
                ).show()

            },

            { error ->

                b.btnBayar.isEnabled = true
                b.btnBayar.text = "Bayar Sekarang"

                val msg = try {

                    JSONObject(
                        String(error.networkResponse?.data ?: ByteArray(0))
                    ).getString("message")

                } catch (e: Exception) {

                    "Gagal melakukan pembayaran"

                }

                Toast.makeText(
                    this,
                    msg,
                    Toast.LENGTH_LONG
                ).show()

            }

        ) {

            override fun getHeaders(): MutableMap<String, String> {

                return hashMapOf(
                    "Authorization" to "Bearer $token",
                    "Accept" to "application/json",
                    "Content-Type" to "application/json"
                )

            }

        }

        Volley.newRequestQueue(this).add(request)

    }

}