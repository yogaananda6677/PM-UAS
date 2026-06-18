package ananda.yoga.projectuasmobile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityTambahPesananBinding
import ananda.yoga.projectuasmobile.model.Produk
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class TambahPesananActivity : AppCompatActivity() {

    private lateinit var b: ActivityTambahPesananBinding
    private lateinit var token: String

    private var idTransaksi = ""
    private var idPs = ""
    private var nomorPs = ""
    private var mode = "produk" // produk / waktu

    private val listProduk = ArrayList<Produk>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityTambahPesananBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Ambil token
        val pref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        token = pref.getString("token", "") ?: ""

        // Ambil intent
        idTransaksi = intent.getStringExtra("id_transaksi") ?: ""
        idPs = intent.getStringExtra("id_ps") ?: ""
        nomorPs = intent.getStringExtra("nomor_ps") ?: ""
        mode = intent.getStringExtra("mode") ?: "produk"

        b.tvInfoTambah.text = "Transaksi : $idTransaksi\nPlaystation : $nomorPs"

        // Mode
        if (mode == "produk") {
            b.tvMode.text = "Tambah Produk"
            b.layoutProduk.visibility = View.VISIBLE
            b.layoutWaktu.visibility = View.GONE
            b.tvQty.visibility = View.VISIBLE
            b.etQty.visibility = View.VISIBLE
            loadProduk()
        } else {
            b.tvMode.text = "Tambah Waktu"
            b.layoutProduk.visibility = View.GONE
            b.layoutWaktu.visibility = View.VISIBLE
            b.tvQty.visibility = View.GONE
            b.etQty.visibility = View.GONE
            setupWaktuSpinner()
        }

        // Tombol kembali
        b.btnKembali.setOnClickListener { finish() }

        // Tombol simpan
        b.btnSimpanTambah.setOnClickListener {
            if (mode == "produk") {
                tambahProduk()
            } else {
                tambahWaktu()
            }
        }

        // Warna tombol (tema biru)
        b.btnSimpanTambah.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, R.color.blue_primary)
        b.btnKembali.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, android.R.color.darker_gray)
    }

    // ===== AMBIL PRODUK (sama persis dengan PemesananActivity) =====
    private fun loadProduk() {
        listProduk.clear()

        val request = object : JsonObjectRequest(
            Method.GET,
            ApiConfig.GET_PRODUK,
            null,
            { response ->
                prosesProduk(response)
            },
            { error ->
                Toast.makeText(this, "Gagal memuat produk", Toast.LENGTH_SHORT).show()
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

    private fun prosesProduk(response: JSONObject) {
        try {
            // Cek apakah response langsung array atau punya key "data"
            val dataArray = response.optJSONArray("data") ?: run {
                // jika response langsung array, parse ulang
                try {
                    JSONArray(response.toString())
                } catch (e: Exception) {
                    JSONArray()
                }
            }

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                val id = obj.optInt("id_produk", obj.optInt("id", 0))
                val nama = obj.optString("nama_produk", obj.optString("nama", "-"))
                val harga = obj.optLong("harga", obj.optLong("harga_produk", 0L))
                val stok = obj.optInt("stok", 0)

                listProduk.add(Produk(id.toString(), nama, harga, stok))
            }

            // Adapter produk (seperti di PemesananActivity)
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listProduk
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            b.spProduk.adapter = adapter

            if (listProduk.isEmpty()) {
                Toast.makeText(this, "Tidak ada produk tersedia", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Format produk tidak sesuai", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== SETUP SPINNER WAKTU =====
    private fun setupWaktuSpinner() {
        val opsiWaktu = arrayOf("30 Menit", "60 Menit", "90 Menit", "120 Menit")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opsiWaktu
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spWaktu.adapter = adapter
    }

    // ===== TAMBAH PRODUK =====
    private fun tambahProduk() {
        if (listProduk.isEmpty()) {
            Toast.makeText(this, "Produk belum tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val posisi = b.spProduk.selectedItemPosition
        if (posisi < 0 || posisi >= listProduk.size) {
            Toast.makeText(this, "Pilih produk", Toast.LENGTH_SHORT).show()
            return
        }

        val qty = b.etQty.text.toString().trim()
        if (qty.isEmpty() || qty.toIntOrNull() == null || qty.toInt() <= 0) {
            Toast.makeText(this, "Qty harus angka positif", Toast.LENGTH_SHORT).show()
            return
        }

        val produkTerpilih = listProduk[posisi]

        // Buat JSON body
        val produkArray = JSONArray()
        val produkObj = JSONObject()
        produkObj.put("id_produk", produkTerpilih.idProduk.toInt())
        produkObj.put("qty", qty.toInt())
        produkArray.put(produkObj)

        val body = JSONObject()
        body.put("produk", produkArray)

        val request = object : JsonObjectRequest(
            Method.PATCH,
            ApiConfig.TAMBAH_PRODUK(idTransaksi),
            body,
            { _ ->
                Toast.makeText(this, "Produk berhasil ditambahkan", Toast.LENGTH_LONG).show()
                finish()
            },
            { error ->
                val pesan = try {
                    JSONObject(String(error.networkResponse?.data ?: ByteArray(0)))
                        .optString("message", "Gagal tambah produk")
                } catch (e: Exception) {
                    "Gagal tambah produk"
                }
                Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
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

    // ===== TAMBAH WAKTU =====
    private fun tambahWaktu() {
        val menitTambahan = when (b.spWaktu.selectedItemPosition) {
            0 -> 30
            1 -> 60
            2 -> 90
            3 -> 120
            else -> 30
        }

        val body = JSONObject()
        body.put("id_ps", idPs.toInt())
        body.put("menit_tambahan", menitTambahan)

        val request = object : JsonObjectRequest(
            Method.PATCH,
            ApiConfig.TAMBAH_WAKTU(idTransaksi),
            body,
            { _ ->
                Toast.makeText(this, "Waktu berhasil ditambahkan", Toast.LENGTH_LONG).show()
                finish()
            },
            { error ->
                Toast.makeText(this, error.message ?: "Gagal tambah waktu", Toast.LENGTH_LONG).show()
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