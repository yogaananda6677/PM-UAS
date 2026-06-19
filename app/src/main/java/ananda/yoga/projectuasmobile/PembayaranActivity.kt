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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PembayaranActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityPembayaranBinding
    private lateinit var token: String

    private var idTransaksi = ""
    private var nomorPs = ""
    private var total = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPembayaranBinding.inflate(layoutInflater)
        setContentView(b.root)

        token = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""

        idTransaksi = intent.getStringExtra("id_transaksi") ?: ""
        nomorPs = intent.getStringExtra("nomor_ps") ?: ""

        b.btnBayar.setOnClickListener(this)
        b.btnKembali.setOnClickListener(this)

        if (idTransaksi.isNotEmpty()) {
            fetchDetailTransaksi()
        } else {
            Toast.makeText(this, "ID Transaksi tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== AMBIL DETAIL TRANSAKSI ====================
    private fun fetchDetailTransaksi() {
        val url = ApiConfig.DETAIL_TRANSAKSI(idTransaksi)

        val request = object : JsonObjectRequest(
            Method.GET,
            url,
            null,
            { response ->
                val data = response.optJSONObject("data")
                if (data != null) {
                    tampilkanDataTransaksi(data)
                } else {
                    Toast.makeText(this, "Data transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                val msg = try {
                    JSONObject(String(error.networkResponse?.data ?: ByteArray(0)))
                        .optString("message", "Gagal ambil detail transaksi")
                } catch (e: Exception) {
                    "Gagal ambil detail transaksi"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Bearer $token",
                    "Accept" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    // ==================== TAMPILKAN DATA ====================
    private fun tampilkanDataTransaksi(data: JSONObject) {
        // Ambil data dasar
        val idTrans = data.optString("id", "")
        val status = data.optString("status_transaksi", "unknown")
        total = data.optLong("total", 0L)

        // Status badge
        when (status.lowercase()) {
            "pending", "waiting" -> {
                b.tvStatus.text = "MENUNGGU KONFIRMASI"
                b.tvStatus.setBackgroundColor(getColor(android.R.color.holo_orange_dark))
            }
            "lunas", "paid" -> {
                b.tvStatus.text = "LUNAS"
                b.tvStatus.setBackgroundColor(getColor(android.R.color.holo_green_dark))
            }
            "batal", "cancelled" -> {
                b.tvStatus.text = "BATAL"
                b.tvStatus.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            }
            else -> {
                b.tvStatus.text = status.uppercase()
                b.tvStatus.setBackgroundColor(getColor(android.R.color.darker_gray))
            }
        }

        // Info dasar
        b.tvInfoPembayaran.text = "Playstation : $nomorPs\nID Transaksi : $idTrans"

        // Rincian pesanan (tanpa emoji)
        val detailBuilder = StringBuilder()

        // SEWA
        val sewaArray = data.optJSONArray("detail_sewa") ?: data.optJSONArray("sewa")
        if (sewaArray != null && sewaArray.length() > 0) {
            detailBuilder.append("SEWA:\n")
            for (i in 0 until sewaArray.length()) {
                val sewa = sewaArray.getJSONObject(i)
                val idPs = sewa.optString("id_ps", "-")
                val jamMulai = sewa.optString("jam_mulai", "-")
                val durasi = sewa.optInt("durasi_menit", 0)
                detailBuilder.append("PS $idPs\n")
                detailBuilder.append("  Mulai : ${formatJam(jamMulai)}\n")
                detailBuilder.append("  Durasi : $durasi menit\n")
            }
            detailBuilder.append("\n")
        }

        // PRODUK
        val produkArray = data.optJSONArray("detail_produk") ?: data.optJSONArray("produk")
        if (produkArray != null && produkArray.length() > 0) {
            detailBuilder.append("PRODUK:\n")
            for (i in 0 until produkArray.length()) {
                val produk = produkArray.getJSONObject(i)
                val nama = produk.optString("nama_produk", produk.optString("nama", "-"))
                val harga = produk.optLong("harga", 0L)
                val qty = produk.optInt("qty", 1)
                val subtotal = produk.optLong("subtotal", harga * qty)
                detailBuilder.append("$nama\n")
                detailBuilder.append("  $qty x ${formatRupiah(harga)} = ${formatRupiah(subtotal)}\n")
            }
        }

        b.tvDetailPesanan.text = detailBuilder.toString()

        // Total
        b.tvTotal.text = "Total : ${formatRupiah(total)}"

        // Jika sudah lunas, disable tombol bayar
        if (status.lowercase() in listOf("lunas", "paid")) {
            b.btnBayar.isEnabled = false
            b.btnBayar.text = "Sudah Lunas"
            b.cardQr.visibility = View.GONE
        }
    }

    // ==================== BAYAR ====================
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnBayar -> bayarCash()
            R.id.btnKembali -> finish()
        }
    }

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
                "Authorization" to "Bearer $token",
                "Accept" to "application/json",
                "Content-Type" to "application/json"
            )
        }

        Volley.newRequestQueue(this).add(request)
    }

    // ==================== UI SETELAH BAYAR ====================
    private fun tampilkanStatusMenunggu() {
        b.tvStatus.text = "MENUNGGU VALIDASI"
        b.tvStatus.setBackgroundColor(getColor(android.R.color.holo_orange_dark))
        b.btnBayar.isEnabled = false
        b.btnBayar.text = "Menunggu Validasi"
    }

    // ==================== GENERATE QR ====================
    private fun generateQr(idTransaksi: String) {
        try {
            val bitMatrix = MultiFormatWriter().encode(
                idTransaksi,
                BarcodeFormat.QR_CODE,
                600,
                600
            )
            val bitmap: Bitmap = BarcodeEncoder().createBitmap(bitMatrix)

            b.ivQrCode.setImageBitmap(bitmap)
            b.cardQr.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuat QR Code", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== FORMATTER ====================
    private fun formatRupiah(value: Long): String {
        val formatter = DecimalFormat("#,###")
        return "Rp " + formatter.format(value).replace(",", ".")
    }

    private fun formatJam(dateTime: String): String {
        if (dateTime == "-" || dateTime == "null" || dateTime.isEmpty()) return "-"
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = format.parse(dateTime)
            val output = SimpleDateFormat("HH:mm", Locale.getDefault())
            output.format(date)
        } catch (e: Exception) {
            dateTime
        }
    }
}