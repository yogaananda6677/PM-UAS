package ananda.yoga.projectuasmobile.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.PengaduanActivity
import ananda.yoga.projectuasmobile.PengaduanRiwayatActivity
import ananda.yoga.projectuasmobile.R
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.FragmentDashboardBinding
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class DashboardFragment : Fragment(), View.OnClickListener {

    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!

    private lateinit var token: String
    private lateinit var idUser: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentDashboardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pref = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        token = pref.getString("token", "") ?: ""
        idUser = pref.getString("id_user", "") ?: ""

        // Tampilkan nama user
        val nama = pref.getString("name", "User")
        b.tvGreetingName.text = nama

        // Setup tombol
        b.btnBuatPengaduan.setOnClickListener(this)
        b.btnRiwayatPengaduan.setOnClickListener(this)

        // Ambil data statistik dan pengaduan terbaru
        getStatistikPengaduan()
        getAktivitasTerbaru()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnBuatPengaduan -> {
                // Cek permission kamera
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                } else {
                    startActivity(Intent(requireContext(), PengaduanActivity::class.java))
                }
            }
            R.id.btnRiwayatPengaduan -> {
                startActivity(Intent(requireContext(), PengaduanRiwayatActivity::class.java))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(requireContext(), PengaduanActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "Kamera diperlukan untuk foto bukti", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getStatistikPengaduan() {
        if (token.isEmpty()) return

        val request = object : JsonObjectRequest(
            Request.Method.GET,
            ApiConfig.RIWAYAT_PENGADUAN,
            null,
            { response ->
                val dataArray = response.optJSONArray("data") ?: JSONArray()
                var total = 0
                var pending = 0
                var proses = 0
                var selesai = 0
                var dibatalkan = 0

                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val status = obj.optString("status_pengaduan", "pending")
                    total++
                    when (status.lowercase()) {
                        "pending" -> pending++
                        "proses" -> proses++
                        "selesai" -> selesai++
                        "dibatalkan" -> dibatalkan++
                    }
                }

                // Tampilkan statistik
                b.tvStatPengaduanTotal.text = total.toString()
                b.tvStatPengaduanPending.text = pending.toString()
                b.tvStatPengaduanProses.text = proses.toString()
                b.tvStatPengaduanSelesai.text = selesai.toString()

                // Sembunyikan loading atau tampilkan card
                b.cardStatPengaduan.visibility = View.VISIBLE
            },
            { error ->
                Toast.makeText(requireContext(), "Gagal ambil statistik pengaduan", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Bearer $token",
                    "Accept" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun getAktivitasTerbaru() {
        // Untuk demo, bisa tampilkan beberapa aktivitas dari riwayat pemesanan atau pengaduan
        // Bisa diambil dari API riwayat pemesanan, atau gabungan
        // Untuk sederhana, kita ambil dari riwayat pengaduan (3 data terakhir)
        // Di sini kita bisa panggil RIWAYAT_PENGADUAN lagi dan tampilkan di ListView
        // Tapi karena sudah dipanggil di getStatistikPengaduan, kita bisa reuse atau panggil terpisah
        // Saya panggil terpisah untuk menjaga modularitas
        if (token.isEmpty()) return

        val request = object : JsonObjectRequest(
            Request.Method.GET,
            ApiConfig.RIWAYAT_PENGADUAN,
            null,
            { response ->
                val dataArray = response.optJSONArray("data") ?: JSONArray()
                val items = mutableListOf<String>()
                val max = minOf(3, dataArray.length())
                for (i in 0 until max) {
                    val obj = dataArray.getJSONObject(i)
                    val judul = obj.optString("judul_pengaduan", "Pengaduan")
                    val status = obj.optString("status_pengaduan", "pending")
                    items.add("$judul - Status: $status")
                }

                // Tampilkan di ListView lvAktivitas (sudah ada di layout)
                val adapter = android.widget.ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    items
                )
                b.lvAktivitas.adapter = adapter
            },
            { error ->
                // Silent fail
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Bearer $token",
                    "Accept" to "application/json"
                )
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}