package ananda.yoga.projectuasmobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.adapter.ProdukPesananAdapter
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityPemesananBinding
import ananda.yoga.projectuasmobile.model.Produk
import ananda.yoga.projectuasmobile.model.ProdukPesanan
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PemesananActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityPemesananBinding

    private var idPs = ""
    private var nomorPs = ""
    private var tipePs = ""
    private var hargaSewa = 0L

    private val calMulai = Calendar.getInstance()
    private val calBooking = Calendar.getInstance()
    private val listJamSelesai = ArrayList<Calendar>()

    private val listProduk = ArrayList<Produk>()
    private val listProdukPesanan = ArrayList<ProdukPesanan>()
    private lateinit var produkPesananAdapter: ProdukPesananAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPemesananBinding.inflate(layoutInflater)
        setContentView(b.root)

        ambilIntent()
        tampilkanInfoPs()

        calMulai.timeInMillis = System.currentTimeMillis()
        calBooking.timeInMillis = System.currentTimeMillis()
        calBooking.add(Calendar.MINUTE, 30)

        setupRadioBooking()
        setupJamSelesai()
        setupProdukPesanan()
        setupTombol()
        getDataProduk()
        hitungTotal()
    }

    private fun ambilIntent() {
        idPs = intent.getStringExtra("id_ps") ?: ""
        nomorPs = intent.getStringExtra("nomor_ps") ?: "-"
        tipePs = intent.getStringExtra("tipe_ps") ?: "-"
        hargaSewa = intent.getLongExtra("harga_sewa", 0L)
    }

    private fun tampilkanInfoPs() {
        b.tvInfoPs.text = "Nomor PS : $nomorPs\n" +
                "Tipe PS : $tipePs\n" +
                "Harga Sewa : ${formatRupiah(hargaSewa)} / jam"
    }

    private fun setupRadioBooking() {
        b.tvJamMulai.text = "Jam mulai: sekarang (${formatTanggalJam(calMulai)})"
        b.btnPilihTanggalJam.visibility = View.GONE

        b.rgJenisPesan.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbMainSekarang) {
                b.btnPilihTanggalJam.visibility = View.GONE
                calMulai.timeInMillis = System.currentTimeMillis()
                b.tvJamMulai.text = "Jam mulai: sekarang (${formatTanggalJam(calMulai)})"
                setupJamSelesai()
                hitungTotal()
            } else {
                b.btnPilihTanggalJam.visibility = View.VISIBLE
                calMulai.timeInMillis = calBooking.timeInMillis
                b.tvJamMulai.text = "Jam mulai booking: ${formatTanggalJam(calBooking)}"
                setupJamSelesai()
                hitungTotal()
            }
        }

        b.btnPilihTanggalJam.setOnClickListener(this)
    }

    private fun tampilkanDateTimePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calBooking.set(Calendar.YEAR, year)
                calBooking.set(Calendar.MONTH, month)
                calBooking.set(Calendar.DAY_OF_MONTH, day)

                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        calBooking.set(Calendar.HOUR_OF_DAY, hour)
                        calBooking.set(Calendar.MINUTE, minute)
                        calBooking.set(Calendar.SECOND, 0)

                        val batasMinimal = Calendar.getInstance()
                        batasMinimal.add(Calendar.MINUTE, 30)

                        if (calBooking.before(batasMinimal)) {
                            Toast.makeText(
                                this,
                                "Booking minimal 30 menit dari sekarang",
                                Toast.LENGTH_SHORT
                            ).show()

                            calBooking.timeInMillis = batasMinimal.timeInMillis
                        }

                        calMulai.timeInMillis = calBooking.timeInMillis
                        b.tvJamMulai.text = "Jam mulai booking: ${formatTanggalJam(calBooking)}"
                        setupJamSelesai()
                        hitungTotal()
                    },
                    calBooking.get(Calendar.HOUR_OF_DAY),
                    calBooking.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calBooking.get(Calendar.YEAR),
            calBooking.get(Calendar.MONTH),
            calBooking.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupJamSelesai() {
        val listText = ArrayList<String>()
        listJamSelesai.clear()

        for (i in 1..12) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = calMulai.timeInMillis
            cal.add(Calendar.MINUTE, i * 30)

            listJamSelesai.add(cal)

            val durasi = i * 30
            listText.add(formatJam(cal) + " (" + durasi + " menit)")
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listText
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spJamSelesai.adapter = adapter
        b.spJamSelesai.setSelection(1)
    }

    private fun setupProdukPesanan() {
        produkPesananAdapter = ProdukPesananAdapter(this, listProdukPesanan) {
            hitungTotal()
        }

        b.lvProdukPesanan.adapter = produkPesananAdapter
    }

    private fun setupTombol() {
        b.btnTambahProduk.setOnClickListener(this)
        b.btnSimpanPesanan.setOnClickListener(this)
        b.btnKembali.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnTambahProduk -> tambahProdukDariSpinner()
            R.id.btnSimpanPesanan -> kirimPemesanan()
            R.id.btnKembali -> finish()
            R.id.btnPilihTanggalJam -> tampilkanDateTimePicker()
        }
    }

    private fun getDataProduk() {
        val pref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val token = pref.getString("token", "") ?: ""

        val request = object : StringRequest(
            Request.Method.GET,
            ApiConfig.GET_PRODUK,
            { response ->
                prosesProduk(response)
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                Toast.makeText(this, "Gagal ambil produk: $statusCode", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"

                if (token.isNotEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }

                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun prosesProduk(response: String) {
        listProduk.clear()
        listProduk.add(Produk("", "Pilih produk tambahan", 0L, 0))

        try {
            val dataArray = getArrayFromResponse(response)

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)

                val idProduk = obj.optString("id_produk", obj.optString("id", ""))
                val namaProduk = obj.optString("nama_produk", obj.optString("nama", "-"))
                val harga = obj.optLong("harga", obj.optLong("harga_produk", 0L))
                val stok = obj.optInt("stok", 0)

                listProduk.add(
                    Produk(
                        idProduk = idProduk,
                        namaProduk = namaProduk,
                        harga = harga,
                        stok = stok
                    )
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Format produk tidak sesuai", Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listProduk
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spProduk.adapter = adapter
    }

    private fun getArrayFromResponse(response: String): JSONArray {
        val text = response.trim()

        if (text.startsWith("[")) {
            return JSONArray(text)
        }

        val obj = JSONObject(text)
        val data = obj.opt("data")

        return when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("produk") ?: JSONArray()
            else -> obj.optJSONArray("produk") ?: JSONArray()
        }
    }

    private fun tambahProdukDariSpinner() {
        if (b.spProduk.selectedItemPosition <= 0) {
            Toast.makeText(this, "Pilih produk dulu", Toast.LENGTH_SHORT).show()
            return
        }

        val produk = listProduk[b.spProduk.selectedItemPosition]

        val produkLama = listProdukPesanan.find { it.idProduk == produk.idProduk }

        if (produkLama != null) {
            produkLama.qty++
        } else {
            listProdukPesanan.add(
                ProdukPesanan(
                    idProduk = produk.idProduk,
                    namaProduk = produk.namaProduk,
                    harga = produk.harga,
                    qty = 1
                )
            )
        }

        produkPesananAdapter.notifyDataSetChanged()
        hitungTotal()
    }

    private fun kirimPemesanan() {
        if (idPs.isEmpty()) {
            Toast.makeText(this, "Data PS tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val pref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val token = pref.getString("token", "") ?: ""
        val idUser = pref.getString("id_user", "") ?: ""

        if (idUser.isEmpty()) {
            Toast.makeText(this, "Silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        val sewaArray = JSONArray()
        val sewaObj = JSONObject()
        sewaObj.put("id_ps", idPs)
        sewaObj.put("jam_mulai", formatTanggalJamApi(calMulai))
        sewaObj.put("durasi_menit", getDurasiMenit())
        sewaArray.put(sewaObj)

        val produkArray = JSONArray()
        for (produk in listProdukPesanan) {
            val produkObj = JSONObject()
            produkObj.put("id_produk", produk.idProduk)
            produkObj.put("qty", produk.qty)
            produkArray.put(produkObj)
        }

        val jenisPesan = if (b.rbBookingNanti.isChecked) "booking" else "langsung"

        val params = JSONObject()
        params.put("id_user", idUser)
        params.put("sumber_transaksi", "aplikasi")
        params.put("jenis_pemesanan", jenisPesan)
        params.put("status_transaksi", "waiting")
        params.put("sewa", sewaArray)
        params.put("produk", produkArray)

        setLoading(true)

        val request = object : JsonObjectRequest(
            Method.POST,
            ApiConfig.CREATE_TRANSAKSI,
            params,
            { response ->
                setLoading(false)

                val pesan = response.optString(
                    "message",
                    "Booking berhasil dibuat, menunggu persetujuan admin"
                )

                Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
                finish()
            },
            { error ->
                setLoading(false)

                val errorBody = error.networkResponse?.data?.toString(Charsets.UTF_8)
                val message = try {
                    JSONObject(errorBody ?: "").optString("message", "Gagal membuat booking")
                } catch (e: Exception) {
                    "Gagal terhubung ke server"
                }

                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Content-Type"] = "application/json"

                if (token.isNotEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }

                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun hitungTotal() {
        val durasiMenit = getDurasiMenit()
        val totalSewa = hargaSewa * durasiMenit / 60

        var totalProduk = 0L
        for (produk in listProdukPesanan) {
            totalProduk += produk.harga * produk.qty
        }

        val total = totalSewa + totalProduk

        b.tvTotal.text = "Total sementara: ${formatRupiah(total)}"
    }

    private fun getDurasiMenit(): Int {
        val posisi = b.spJamSelesai.selectedItemPosition

        if (posisi < 0 || posisi >= listJamSelesai.size) {
            return 30
        }

        val selesai = listJamSelesai[posisi]
        val selisih = selesai.timeInMillis - calMulai.timeInMillis

        return (selisih / 60000).toInt()
    }

    private fun setLoading(loading: Boolean) {
        b.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnSimpanPesanan.isEnabled = !loading
        b.btnSimpanPesanan.text = if (loading) "Memproses..." else "Buat Booking"
    }

    private fun formatTanggalJam(cal: Calendar): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(cal.time)
    }

    private fun formatTanggalJamApi(cal: Calendar): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(cal.time)
    }

    private fun formatJam(cal: Calendar): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(cal.time)
    }

    private fun formatRupiah(value: Long): String {
        return "Rp " + String.format("%,d", value).replace(",", ".")
    }
}