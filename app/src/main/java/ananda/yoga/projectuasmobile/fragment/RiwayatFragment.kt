package ananda.yoga.projectuasmobile.fragment

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.R
import ananda.yoga.projectuasmobile.SettingsHelper
import ananda.yoga.projectuasmobile.config.ApiConfig
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RiwayatFragment : Fragment() {

    private lateinit var tvTanggalMulai: TextView
    private lateinit var tvTanggalSelesai: TextView
    private lateinit var tvJumlahRiwayat: TextView
    private lateinit var lvRiwayat: ListView
    private lateinit var btnFilter: Button

    private var tanggalMulai   = ""
    private var tanggalSelesai = ""

    data class RiwayatItem(
        val namaPS: String,
        val tanggal: String,
        val jamMulai: String,
        val jamSelesai: String,
        val status: String,
        val totalHarga: Long
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_riwayat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTanggalMulai   = view.findViewById(R.id.tvTanggalMulai)
        tvTanggalSelesai = view.findViewById(R.id.tvTanggalSelesai)
        tvJumlahRiwayat  = view.findViewById(R.id.tvJumlahRiwayat)
        lvRiwayat        = view.findViewById(R.id.lvRiwayat)
        btnFilter        = view.findViewById(R.id.btnFilter)

        // DatePicker untuk tanggal mulai
        tvTanggalMulai.setOnClickListener {
            showDatePicker { tanggal ->
                tanggalMulai = tanggal
                tvTanggalMulai.text = tanggal
            }
        }

        // DatePicker untuk tanggal selesai
        tvTanggalSelesai.setOnClickListener {
            showDatePicker { tanggal ->
                tanggalSelesai = tanggal
                tvTanggalSelesai.text = tanggal
            }
        }

        // Tombol filter
        btnFilter.setOnClickListener {
            if (tanggalMulai.isEmpty() || tanggalSelesai.isEmpty()) {
                Toast.makeText(requireContext(), "Pilih tanggal mulai dan selesai dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loadRiwayat(tanggalMulai, tanggalSelesai)
        }

        // Load semua riwayat saat pertama buka
        loadRiwayat()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val tanggal = "%04d-%02d-%02d".format(year, month + 1, day)
                onDateSelected(tanggal)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadRiwayat(
        tanggalMulai: String = "",
        tanggalSelesai: String = ""
    ) {
        tvJumlahRiwayat.text = "Memuat data..."

        val token = SettingsHelper.getToken(requireContext())

        // ✅ Cek token kosong atau tidak
        if (token.isEmpty()) {
            tvJumlahRiwayat.text = "Token kosong, silakan login ulang"
            return
        }

        var url = ApiConfig.RIWAYAT_PEMESANAN
        if (tanggalMulai.isNotEmpty() && tanggalSelesai.isNotEmpty()) {
            url += "?tanggal_mulai=$tanggalMulai&tanggal_selesai=$tanggalSelesai"
        }

        android.util.Log.d("RIWAYAT", "URL: $url")
        android.util.Log.d("RIWAYAT", "Token: $token")

        val request = object : JsonObjectRequest(
            Method.GET, url, null,
            { response ->
                android.util.Log.d("RIWAYAT", "Response: $response")

                // ✅ Langsung ambil "data" tanpa cek "success"
                val data = response.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    tampilkanRiwayat(data)
                } else {
                    tvJumlahRiwayat.text = "Belum ada riwayat pemesanan"
                }
            },
            { error ->
                // ✅ Log detail error
                val statusCode = error.networkResponse?.statusCode
                val errorBody = error.networkResponse?.data?.let { String(it) }
                android.util.Log.e("RIWAYAT", "Error code: $statusCode")
                android.util.Log.e("RIWAYAT", "Error body: $errorBody")

                tvJumlahRiwayat.text = "Gagal memuat (kode: $statusCode)"
                Toast.makeText(
                    requireContext(),
                    "Error $statusCode: $errorBody",
                    Toast.LENGTH_LONG
                ).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer $token"
                )
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }
    private fun tampilkanRiwayat(data: JSONArray) {
        val list = ArrayList<RiwayatItem>()

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            list.add(
                RiwayatItem(
                    namaPS     = item.optString("nama_ps", "PS"),
                    tanggal    = formatTanggal(item.optString("tanggal", "")),
                    jamMulai   = item.optString("jam_mulai", "-"),
                    jamSelesai = item.optString("jam_selesai", "-"),
                    status     = item.optString("status_transaksi", "-"),
                    totalHarga = item.optLong("total_harga", 0)
                )
            )
        }

        tvJumlahRiwayat.text = "${list.size} transaksi ditemukan"

        val adapter = object : BaseAdapter() {
            override fun getCount() = list.size
            override fun getItem(pos: Int) = list[pos]
            override fun getItemId(pos: Int) = pos.toLong()

            override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
                val v = convertView ?: LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_riwayat, parent, false)

                val item = list[pos]

                v.findViewById<TextView>(R.id.tvNamaPS).text = item.namaPS
                v.findViewById<TextView>(R.id.tvTanggal).text = item.tanggal
                v.findViewById<TextView>(R.id.tvJam).text =
                    "${item.jamMulai} - ${item.jamSelesai}"

                val tvStatus = v.findViewById<TextView>(R.id.tvStatus)
                tvStatus.text = item.status
                tvStatus.setTextColor(
                    when (item.status.lowercase()) {
                        "selesai"  -> android.graphics.Color.parseColor("#3B6D11")
                        "aktif"    -> android.graphics.Color.parseColor("#185FA5")
                        "batal"    -> android.graphics.Color.parseColor("#A32D2D")
                        else       -> android.graphics.Color.parseColor("#888780")
                    }
                )

                val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                v.findViewById<TextView>(R.id.tvHarga).text =
                    formatRupiah.format(item.totalHarga)

                return v
            }
        }

        lvRiwayat.adapter = adapter
    }

    private fun formatTanggal(tanggal: String): String {
        return try {
            val sdf    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfOut = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            sdfOut.format(sdf.parse(tanggal)!!)
        } catch (e: Exception) {
            tanggal
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RiwayatFragment()
    }
}