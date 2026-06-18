package ananda.yoga.projectuasmobile

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityPembayaranBinding
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONObject

class PembayaranActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityPembayaranBinding
    private lateinit var token: String

    private var idTransaksi = ""
    private var nomorPs = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPembayaranBinding.inflate(layoutInflater)
        setContentView(b.root)

        token = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""

        idTransaksi = intent.getStringExtra("id_transaksi") ?: ""
        nomorPs     = intent.getStringExtra("nomor_ps") ?: ""

        b.tvInfoPembayaran.text = "Playstation : $nomorPs\n\nID Transaksi : $idTransaksi"

        b.btnBayar.setOnClickListener(this)
        b.btnKembali.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnBayar    -> bayarCash()
            R.id.btnKembali  -> finish()
        }
    }

    // ── Kirim request bayar cash ──────────────────────────────────────────────

    private fun bayarCash() {
        b.btnBayar.isEnabled = false
        b.btnBayar.text = "Memproses..."

        val body = JSONObject().apply {
            put("metode_pembayaran", "cash")
        }

        val request = object : JsonObjectRequest(
            Method.PATCH,
            ApiConfig.BAYAR_TRANSAKSI(idTransaksi),
            body,
            { response ->
                tampilkanStatusMenunggu()
                generateQr(idTransaksi)
                Toast.makeText(
                    this,
                    response.optString("message", "Silakan tunjukkan QR ke kasir."),
                    Toast.LENGTH_LONG
                ).show()
            },
            { error ->
                b.btnBayar.isEnabled = true
                b.btnBayar.text = "Bayar Sekarang"
                val msg = try {
                    JSONObject(String(error.networkResponse?.data ?: ByteArray(0)))
                        .getString("message")
                } catch (e: Exception) {
                    "Gagal melakukan pembayaran"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders() = hashMapOf(
                "Authorization"  to "Bearer $token",
                "Accept"         to "application/json",
                "Content-Type"   to "application/json"
            )
        }

        Volley.newRequestQueue(this).add(request)
    }

    // ── UI setelah berhasil ───────────────────────────────────────────────────

    private fun tampilkanStatusMenunggu() {
        b.tvStatus.text = "MENUNGGU VALIDASI"
        b.tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
        b.btnBayar.isEnabled = false
        b.btnBayar.text = "Menunggu Validasi"
    }

    // ── Generate QR Code ─────────────────────────────────────────────────────

    private fun generateQr(idTransaksi: String) {
        try {
            val bitMatrix = MultiFormatWriter().encode(
                idTransaksi,          // isi QR = ID transaksi saja
                BarcodeFormat.QR_CODE,
                600,
                600
            )
            val bitmap: Bitmap = BarcodeEncoder().createBitmap(bitMatrix)

            b.ivQrCode.setImageBitmap(bitmap)
            b.ivQrCode.visibility    = View.VISIBLE
            b.tvQrLabel.visibility   = View.VISIBLE
            b.tvQrSubLabel.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuat QR Code", Toast.LENGTH_SHORT).show()
        }
    }
}