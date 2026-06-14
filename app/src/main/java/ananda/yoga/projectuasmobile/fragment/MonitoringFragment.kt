package ananda.yoga.projectuasmobile.fragment

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

class MonitoringFragment : Fragment() {

    private var _b: FragmentMonitoringBinding? = null
    private val b get() = _b!!

    private val semuaData = ArrayList<MonitoringPs>()
    private val tampilData = ArrayList<MonitoringPs>()
    private lateinit var adapterMonitoring: ArrayAdapter<String>

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

        setupListView()
        setupSpinner()
        setupSearch()
        setupButton()
        getDataMonitoring()
    }

    private fun setupListView() {
        adapterMonitoring = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            ArrayList<String>()
        )

        b.lvMonitoring.adapter = adapterMonitoring

        b.lvMonitoring.setOnItemClickListener { _, view, position, _ ->
            val item = tampilData[position]

            if (isWaiting(item)) {
                Toast.makeText(
                    requireContext(),
                    "Pemesanan ${item.nomorPs} masih menunggu persetujuan admin",
                    Toast.LENGTH_LONG
                ).show()

            } else if (isTersedia(item.statusPs)) {
                bukaPemesanan(item)

            } else if (isMilikSaya(item)) {
                tampilkanPopupMenu(view, item)

            } else if (isDigunakan(item.statusPs) || item.idTransaksi.isNotEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "${item.nomorPs} sedang digunakan. Berakhir jam ${formatJam(item.jamSelesai)}",
                    Toast.LENGTH_LONG
                ).show()

            } else {
                Toast.makeText(
                    requireContext(),
                    "${item.nomorPs} belum bisa dipesan",
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
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
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
        b.btnRefresh.setOnClickListener {
            getDataMonitoring()
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

                val idTransaksi = if (transaksiObj != null) {
                    transaksiObj.optString("id_transaksi", transaksiObj.optString("id", ""))
                } else {
                    ""
                }

                val statusTransaksi = if (transaksiObj != null) {
                    transaksiObj.optString("status_transaksi", transaksiObj.optString("status", ""))
                } else {
                    ""
                }

                val userObj = transaksiObj?.optJSONObject("user")
                    ?: transaksiObj?.optJSONObject("pelanggan")

                val idUserTransaksi = if (userObj != null) {
                    userObj.optString("id_user", userObj.optString("id", ""))
                } else {
                    ""
                }

                val namaPelanggan = if (userObj != null) {
                    userObj.optString("name", userObj.optString("nama", "-"))
                } else {
                    "-"
                }

                val pembayaranObj = transaksiObj?.optJSONObject("pembayaran")
                val statusBayar = if (pembayaranObj != null) {
                    pembayaranObj.optString("status_bayar", pembayaranObj.optString("status", "-"))
                } else {
                    "-"
                }

                val detailSewa = cariDetailSewa(transaksiObj, idPs)
                val jamMulai = detailSewa?.optString("jam_mulai", "-") ?: "-"

                var jamSelesai = detailSewa?.optString("jam_selesai", "-") ?: "-"

                if (jamSelesai == "null" || jamSelesai == "-" || jamSelesai.isEmpty()) {
                    val durasi = detailSewa?.optInt("durasi_menit", 0) ?: 0
                    jamSelesai = hitungJamSelesai(jamMulai, durasi)
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
                        jamMulai = jamMulai,
                        jamSelesai = jamSelesai,
                        statusBayar = statusBayar,
                        statusTransaksi = statusTransaksi
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
        if (transaksiObj == null) {
            return null
        }

        val detailArray = transaksiObj.optJSONArray("detail_sewa")
            ?: transaksiObj.optJSONArray("sewa")
            ?: return null

        for (i in 0 until detailArray.length()) {
            val detail = detailArray.getJSONObject(i)
            val idPsDetail = detail.optString("id_ps", "")

            if (idPsDetail == idPs) {
                return detail
            }
        }

        if (detailArray.length() > 0) {
            return detailArray.getJSONObject(0)
        }

        return null
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
        if (!::adapterMonitoring.isInitialized) {
            return
        }

        val keyword = b.actCariPs.text.toString().trim().lowercase()
        val statusPilihan = b.spStatus.selectedItem?.toString() ?: "Semua"

        tampilData.clear()
        adapterMonitoring.clear()

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
                adapterMonitoring.add(buatTextList(item))
            }
        }

        adapterMonitoring.notifyDataSetChanged()
        b.tvKosong.visibility = if (tampilData.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun buatTextList(item: MonitoringPs): String {
        if (isWaiting(item)) {
            return "${item.nomorPs} | ${item.tipePs}\nMENUNGGU PERSETUJUAN ADMIN"
        }

        if (isTersedia(item.statusPs)) {
            return "${item.nomorPs} | ${item.tipePs}\nTERSEDIA - klik untuk pesan"
        }

        if (isMilikSaya(item)) {
            return "${item.nomorPs} | ${item.tipePs}\nPUNYAKU - klik untuk bayar / tambah\nBerakhir: ${formatJam(item.jamSelesai)} | Bayar: ${item.statusBayar}"
        }

        if (isDigunakan(item.statusPs)) {
            return "${item.nomorPs} | ${item.tipePs}\nDIGUNAKAN ORANG LAIN\nBerakhir: ${formatJam(item.jamSelesai)}"
        }

        return "${item.nomorPs} | ${item.tipePs}\n${item.statusPs}"
    }

    private fun tampilkanPopupMenu(anchor: View, item: MonitoringPs) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_monitoring_pelanggan, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.itemBayar -> {
                    bukaPembayaran(item)
                    true
                }

                R.id.itemTambah -> {
                    bukaTambahPesanan(item)
                    true
                }

                else -> false
            }
        }

        popup.show()
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
        intent.putExtra("total", 0L)
        startActivity(intent)
    }

    private fun bukaTambahPesanan(item: MonitoringPs) {
        val intent = Intent(requireContext(), TambahPesananActivity::class.java)
        intent.putExtra("id_transaksi", item.idTransaksi)
        intent.putExtra("id_ps", item.idPs)
        intent.putExtra("nomor_ps", item.nomorPs)
        startActivity(intent)
    }

    private fun isWaiting(item: MonitoringPs): Boolean {
        val statusTransaksi = item.statusTransaksi.lowercase()
        val statusPs = item.statusPs.lowercase()

        return statusTransaksi == "waiting" ||
                statusTransaksi == "pending" ||
                statusTransaksi == "menunggu" ||
                statusTransaksi == "menunggu_persetujuan" ||
                statusPs == "waiting" ||
                statusPs == "pending" ||
                statusPs == "menunggu"
    }

    private fun isMilikSaya(item: MonitoringPs): Boolean {
        val pref = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val idUserLogin = pref.getString("id_user", "") ?: ""

        return idUserLogin.isNotEmpty() && item.idUserTransaksi == idUserLogin
    }

    private fun ubahStatus(status: String): String {
        val s = status.lowercase()

        return when (s) {
            "tersedia", "available" -> "Tersedia"
            "digunakan", "dipakai", "used", "rented" -> "Digunakan"
            "maintenance", "perbaikan" -> "Maintenance"
            "waiting", "pending", "menunggu" -> "Menunggu"
            else -> status
        }
    }

    private fun isDigunakan(status: String): Boolean {
        val s = status.lowercase()
        return s == "digunakan" || s == "dipakai" || s == "used" || s == "rented"
    }

    private fun isTersedia(status: String): Boolean {
        val s = status.lowercase()
        return s == "tersedia" || s == "available"
    }

    private fun formatJam(value: String): String {
        if (value.isEmpty() || value == "-" || value == "null") {
            return "-"
        }

        if (value.length >= 16) {
            return value.substring(11, 16)
        }

        return value
    }

    private fun hitungJamSelesai(jamMulai: String, durasiMenit: Int): String {
        if (jamMulai.isEmpty() || jamMulai == "-" || durasiMenit <= 0) {
            return "-"
        }

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
