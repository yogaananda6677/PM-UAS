package ananda.yoga.projectuasmobile.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.PembayaranActivity
import ananda.yoga.projectuasmobile.PemesananActivity
import ananda.yoga.projectuasmobile.R
import ananda.yoga.projectuasmobile.TambahPesananActivity
import ananda.yoga.projectuasmobile.adapter.MonitoringAdapter
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.FragmentMonitoringBinding
import ananda.yoga.projectuasmobile.model.MonitoringPs
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonitoringFragment : Fragment(), View.OnClickListener {

    private var _b: FragmentMonitoringBinding? = null
    private val b get() = _b!!

    private val semuaData = ArrayList<MonitoringPs>()
    private val tampilData = ArrayList<MonitoringPs>()
    private lateinit var adapterMonitoring: MonitoringAdapter
    private lateinit var idUserLogin: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentMonitoringBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pref = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        idUserLogin = pref.getString("id_user", "") ?: ""

        setupListView()
        setupSpinner()
        setupSearch()
        setupButton()
        getDataMonitoring()
    }

    override fun onResume() {
        super.onResume()
        getDataMonitoring()
    }

    private fun setupListView() {
        adapterMonitoring = MonitoringAdapter(
            requireContext(),
            tampilData,
            idUserLogin
        )
        b.gvMonitoring.adapter = adapterMonitoring

        b.gvMonitoring.setOnItemClickListener { _, view, position, _ ->
            val item = tampilData[position]

            if (isWaiting(item)) {
                Toast.makeText(
                    requireContext(),
                    "Pemesanan masih menunggu persetujuan admin",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (isTersedia(item.statusPs)) {
                bukaPemesanan(item)
            } else if (isMilikSaya(item)) {
                tampilkanPopupMenu(view, item)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Playstation sedang digunakan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupSpinner() {
        val dataStatus = arrayOf("Semua", "Tersedia", "Digunakan", "Menunggu", "Maintenance")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            dataStatus
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spStatus.adapter = adapter

        b.spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearch() {
        b.actCariPs.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterData()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupButton() {
        b.btnRefresh.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnRefresh -> getDataMonitoring()
        }
    }

    private fun getDataMonitoring() {
        b.progressBar.visibility = View.VISIBLE
        b.tvKosong.visibility = View.GONE

        val pref = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val token = pref.getString("token", "") ?: ""

        val request = object : StringRequest(
            Request.Method.GET,
            ApiConfig.GET_MONITORING,
            { response ->
                b.progressBar.visibility = View.GONE
                prosesResponse(response)
            },
            { error ->
                b.progressBar.visibility = View.GONE
                val statusCode = error.networkResponse?.statusCode
                val errorBody = error.networkResponse?.data?.toString(Charsets.UTF_8)
                val pesan = errorBody ?: "Gagal mengambil data monitoring"
                Toast.makeText(
                    requireContext(),
                    "Error $statusCode: $pesan",
                    Toast.LENGTH_SHORT
                ).show()
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

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun prosesResponse(response: String) {
        semuaData.clear()

        try {
            val jsonObject = JSONObject(response)
            val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)

                val idPs = item.optString("id_ps", item.optString("id", ""))
                val nomorPs = item.optString("nomor_ps", "-")
                val statusPs = item.optString("status_ps", item.optString("status", "-"))

                val tipeObj = item.optJSONObject("tipe")
                val tipePs = item.optString(
                    "tipe_ps",
                    tipeObj?.optString("nama_tipe", "-") ?: item.optString("nama_tipe", "-")
                )

                val hargaSewa = item.optLong(
                    "harga_sewa",
                    tipeObj?.optLong("harga_sewa", 0L) ?: 0L
                )

                val transaksiObj = item.optJSONObject("active_transaksi")
                    ?: item.optJSONObject("transaksi_aktif")
                    ?: item.optJSONObject("pemesanan_aktif")

                val idTransaksi = transaksiObj?.optString("id_transaksi", transaksiObj.optString("id", "")) ?: ""
                val statusTransaksi = transaksiObj?.optString("status_transaksi", transaksiObj.optString("status", "")) ?: ""

                val userObj = transaksiObj?.optJSONObject("user") ?: transaksiObj?.optJSONObject("pelanggan")
                val idUserTransaksi = userObj?.optString("id_user", userObj.optString("id", "")) ?: ""
                val namaPelanggan = userObj?.optString("name", userObj.optString("nama", "-")) ?: "-"

                val pembayaranObj = transaksiObj?.optJSONObject("pembayaran")
                val statusBayar = pembayaranObj?.optString("status_bayar", pembayaranObj.optString("status", "-")) ?: "-"

                val detailSewa = cariDetailSewa(transaksiObj, idPs)
                val jamMulai = detailSewa?.optString("jam_mulai", "-") ?: "-"
                var jamSelesai = detailSewa?.optString("jam_selesai", "-") ?: "-"

                var durasiMenit = 0
                if (jamSelesai == "null" || jamSelesai == "-" || jamSelesai.isEmpty()) {
                    durasiMenit = detailSewa?.optInt("durasi_menit", 0) ?: 0
                    jamSelesai = hitungJamSelesai(jamMulai, durasiMenit)
                } else {
                    try {
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val start = format.parse(jamMulai)
                        val end = format.parse(jamSelesai)
                        if (start != null && end != null) {
                            durasiMenit = ((end.time - start.time) / 60000).toInt()
                        }
                    } catch (e: Exception) {
                        durasiMenit = 0
                    }
                }

                semuaData.add(
                    MonitoringPs(
                        idPs = idPs,
                        nomorPs = nomorPs,
                        tipePs = tipePs,
                        statusPs = statusPs,
                        hargaSewa = hargaSewa,
                        idTransaksi = idTransaksi,
                        idUserTransaksi = idUserTransaksi,
                        namaPelanggan = namaPelanggan,
                        jamMulai = formatJam(jamMulai),
                        jamSelesai = formatJam(jamSelesai),
                        statusBayar = statusBayar,
                        statusTransaksi = statusTransaksi,
                        durasiMenit = durasiMenit,
                        jamMulaiFull = jamMulai,
                        jamSelesaiFull = jamSelesai
                    )
                )
            }

            setupAutoComplete()
            filterData()

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Format data monitoring tidak sesuai",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun cariDetailSewa(transaksiObj: JSONObject?, idPs: String): JSONObject? {
        if (transaksiObj == null) return null
        val detailArray = transaksiObj.optJSONArray("detail_sewa") ?: transaksiObj.optJSONArray("sewa") ?: return null
        for (i in 0 until detailArray.length()) {
            val detail = detailArray.getJSONObject(i)
            if (detail.optString("id_ps", "") == idPs) {
                return detail
            }
        }
        return if (detailArray.length() > 0) detailArray.getJSONObject(0) else null
    }

    private fun setupAutoComplete() {
        val listNamaPs = ArrayList<String>()
        for (item in semuaData) {
            listNamaPs.add(item.nomorPs)
        }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            listNamaPs
        )
        b.actCariPs.setAdapter(adapter)
    }

    private fun filterData() {
        if (!::adapterMonitoring.isInitialized) return

        val keyword = b.actCariPs.text.toString().trim().lowercase()
        val statusPilihan = b.spStatus.selectedItem?.toString() ?: "Semua"

        tampilData.clear()
        for (item in semuaData) {
            val cocokCari = item.nomorPs.lowercase().contains(keyword) ||
                    item.tipePs.lowercase().contains(keyword)
            val cocokStatus = when (statusPilihan) {
                "Semua" -> true
                "Menunggu" -> isWaiting(item)
                else -> ubahStatus(item.statusPs).equals(statusPilihan, ignoreCase = true)
            }
            if (cocokCari && cocokStatus) {
                tampilData.add(item)
            }
        }

        adapterMonitoring.notifyDataSetChanged()
        b.tvKosong.visibility = if (tampilData.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun tampilkanPopupMenu(anchor: View, item: MonitoringPs) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_monitoring_pelanggan, popup.menu)

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.itemTambahProduk -> {
                    val intent = Intent(requireContext(), TambahPesananActivity::class.java)
                    intent.putExtra("mode", "produk")
                    intent.putExtra("id_transaksi", item.idTransaksi)
                    intent.putExtra("id_ps", item.idPs)
                    intent.putExtra("nomor_ps", item.nomorPs)
                    startActivity(intent)
                    true
                }
                R.id.itemTambahWaktu -> {
                    val intent = Intent(requireContext(), TambahPesananActivity::class.java)
                    intent.putExtra("mode", "waktu")
                    intent.putExtra("id_transaksi", item.idTransaksi)
                    intent.putExtra("id_ps", item.idPs)
                    intent.putExtra("nomor_ps", item.nomorPs)
                    startActivity(intent)
                    true
                }
                R.id.itemBayar -> {
                    bukaPembayaran(item)
                    true
                }
                R.id.itemDetail -> {
                    tampilkanDetailPesanan(item)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun tampilkanDetailPesanan(item: MonitoringPs) {
        val message = buildString {
            appendLine("📋 DETAIL PESANAN")
            appendLine("═══════════════════════")
            appendLine("ID Transaksi : ${item.idTransaksi}")
            appendLine("ID PS        : ${item.idPs}")
            appendLine("Nomor PS     : ${item.nomorPs}")
            appendLine("Tipe PS      : ${item.tipePs}")
            appendLine("Harga Sewa   : Rp ${String.format("%,d", item.hargaSewa).replace(",", ".")} / jam")
            appendLine("Pelanggan    : ${item.namaPelanggan}")
            appendLine("Status PS    : ${item.statusPs}")
            appendLine("Status Transaksi : ${item.statusTransaksi}")
            appendLine("Jam Mulai    : ${item.jamMulai}")
            appendLine("Jam Selesai  : ${item.jamSelesai}")
            appendLine("Durasi       : ${item.durasiMenit} menit")
            appendLine("Status Bayar : ${item.statusBayar}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Pesanan")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun bukaPemesanan(item: MonitoringPs) {
        val intent = Intent(requireContext(), PemesananActivity::class.java)
        intent.putExtra("id_ps", item.idPs)
        intent.putExtra("nomor_ps", item.nomorPs)
        intent.putExtra("tipe_ps", item.tipePs)
        intent.putExtra("harga_sewa", item.hargaSewa)
        startActivity(intent)
    }

    private fun bukaPembayaran(item: MonitoringPs) {
        val intent = Intent(requireContext(), PembayaranActivity::class.java)
        intent.putExtra("id_transaksi", item.idTransaksi)
        intent.putExtra("nomor_ps", item.nomorPs)
        intent.putExtra("status_bayar", item.statusBayar)
        intent.putExtra("total", 0L)
        startActivity(intent)
    }

    private fun isWaiting(item: MonitoringPs): Boolean {
        val statusTransaksi = item.statusTransaksi.lowercase()
        val statusPs = item.statusPs.lowercase()
        return statusTransaksi in listOf("waiting", "pending", "menunggu", "menunggu_persetujuan") ||
                statusPs in listOf("waiting", "pending", "menunggu")
    }

    private fun isMilikSaya(item: MonitoringPs): Boolean {
        return idUserLogin.isNotEmpty() && item.idUserTransaksi == idUserLogin
    }

    private fun ubahStatus(status: String): String {
        return when (status.lowercase()) {
            "tersedia", "available" -> "Tersedia"
            "digunakan", "dipakai", "used", "rented" -> "Digunakan"
            "maintenance", "perbaikan" -> "Maintenance"
            "waiting", "pending", "menunggu" -> "Menunggu"
            else -> status
        }
    }

    private fun isTersedia(status: String): Boolean {
        return status.lowercase() in listOf("tersedia", "available")
    }

    private fun formatJam(value: String): String {
        if (value.isEmpty() || value == "-" || value == "null") return "-"
        return try {
            if (value.length >= 16) value.substring(11, 16) else value
        } catch (e: Exception) {
            "-"
        }
    }

    private fun hitungJamSelesai(jamMulai: String, durasiMenit: Int): String {
        if (jamMulai.isEmpty() || jamMulai == "-" || durasiMenit <= 0) return "-"
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = format.parse(jamMulai)
            val calendar = Calendar.getInstance()
            calendar.time = date!!
            calendar.add(Calendar.MINUTE, durasiMenit)
            format.format(calendar.time)
        } catch (e: Exception) {
            "-"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}