package ananda.yoga.projectuasmobile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityTambahPesananBinding
import com.android.volley.AuthFailureError
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

    // mode = produk / waktu
    private var mode = "produk"

    private val namaProduk = ArrayList<String>()
    private val idProduk = ArrayList<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityTambahPesananBinding.inflate(layoutInflater)
        setContentView(b.root)

        val pref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        token = pref.getString("token", "") ?: ""

        idTransaksi = intent.getStringExtra("id_transaksi") ?: ""
        idPs = intent.getStringExtra("id_ps") ?: ""
        nomorPs = intent.getStringExtra("nomor_ps") ?: ""
        mode = intent.getStringExtra("mode") ?: "produk"

        Toast.makeText(
            this,
            "ID Transaksi = $idTransaksi",
            Toast.LENGTH_LONG
        ).show()

        b.tvInfoTambah.text =
            "Transaksi : $idTransaksi\nPlaystation : $nomorPs"

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

            val waktu = arrayListOf(
                "30 Menit",
                "60 Menit",
                "90 Menit",
                "120 Menit"
            )
        }

        b.btnKembali.setOnClickListener {
            finish()
        }

        b.btnSimpanTambah.setOnClickListener {

            if (mode == "produk") {

                tambahProduk()

            } else {

                tambahWaktu()

            }

        }

    }

    private fun loadProduk() {

        val request = object : JsonObjectRequest(
            Method.GET,
            ApiConfig.GET_PRODUK,
            null,
            { response ->

                namaProduk.clear()
                idProduk.clear()

                val data: JSONArray = response.getJSONArray("data")

                for (i in 0 until data.length()) {

                    val obj = data.getJSONObject(i)

                    idProduk.add(obj.getInt("id_produk"))

                    namaProduk.add(
                        obj.getString("nama") +
                                " | Rp " +
                                obj.getString("harga")
                    )

                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    namaProduk
                )

                b.spProduk.adapter = adapter

            },
            {

                Toast.makeText(
                    this,
                    "Produk gagal dimuat",
                    Toast.LENGTH_SHORT
                ).show()

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

    private fun tambahProduk() {

        if (idProduk.isEmpty()) {
            Toast.makeText(this, "Produk belum tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val posisi = b.spProduk.selectedItemPosition

        if (posisi < 0) {
            Toast.makeText(this, "Pilih produk", Toast.LENGTH_SHORT).show()
            return
        }

        val qty = b.etQty.text.toString().trim()

        if (qty.isEmpty()) {
            Toast.makeText(this, "Qty wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val body = JSONObject()

        val produkArray = JSONArray()

        val produkObject = JSONObject()

        produkObject.put("id_produk", idProduk[posisi])
        produkObject.put("qty", qty.toInt())

        produkArray.put(produkObject)

        body.put("produk", produkArray)

        val request = object : JsonObjectRequest(
            Method.PATCH,
            ApiConfig.TAMBAH_PRODUK(idTransaksi),
            body,

            {

                Toast.makeText(
                    this,
                    "Produk berhasil ditambahkan",
                    Toast.LENGTH_LONG
                ).show()

                finish()

            },

            { error ->

                val pesan = try{
                    JSONObject(
                        String(error.networkResponse?.data ?: ByteArray(0))
                    ).getString("message")
                }catch(e:Exception){
                    "Gagal tambah produk"
                }

                Toast.makeText(
                    this,
                    pesan,
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

            {

                Toast.makeText(
                    this,
                    "Waktu berhasil ditambahkan",
                    Toast.LENGTH_LONG
                ).show()

                finish()

            },

            { error ->

                Toast.makeText(
                    this,
                    error.message ?: "Gagal tambah waktu",
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