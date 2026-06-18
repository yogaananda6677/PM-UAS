package ananda.yoga.projectuasmobile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ananda.yoga.projectuasmobile.adapter.PengaduanRiwayatAdapter
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityPengaduanRiwayatBinding
import ananda.yoga.projectuasmobile.databinding.DialogDetailPengaduanBinding
import ananda.yoga.projectuasmobile.model.Pengaduan
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

class PengaduanRiwayatActivity : AppCompatActivity() {

    private lateinit var b: ActivityPengaduanRiwayatBinding
    private lateinit var token: String
    private val dataList = mutableListOf<Pengaduan>()
    private lateinit var adapter: PengaduanRiwayatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPengaduanRiwayatBinding.inflate(layoutInflater)
        setContentView(b.root)

        val pref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        token = pref.getString("token", "") ?: ""

        setupRecyclerView()
        b.btnKembali.setOnClickListener { finish() }
        b.btnRefresh.setOnClickListener { getRiwayat() }

        getRiwayat()
    }

    private fun setupRecyclerView() {
        adapter = PengaduanRiwayatAdapter(this, dataList) { item ->
            tampilkanDetailPengaduan(item)
        }
        b.rvRiwayatPengaduan.layoutManager = LinearLayoutManager(this)
        b.rvRiwayatPengaduan.adapter = adapter
    }

    private fun getRiwayat() {
        b.progressBar.visibility = View.VISIBLE
        b.tvKosong.visibility = View.GONE

        val request = object : JsonObjectRequest(
            Method.GET,
            ApiConfig.RIWAYAT_PENGADUAN,
            null,
            { response ->
                b.progressBar.visibility = View.GONE
                dataList.clear()

                val dataArray = response.optJSONArray("data") ?: JSONArray()
                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val pengaduan = Pengaduan(
                        id = obj.optInt("id"),
                        judul_pengaduan = obj.optString("judul_pengaduan", ""),
                        isi_pengaduan = obj.optString("isi_pengaduan", ""),
                        kategori_aduan = obj.optString("kategori_aduan", ""),
                        status_pengaduan = obj.optString("status_pengaduan", "pending"),
                        foto_bukti = obj.optString("foto_bukti", "").ifEmpty { null },
                        created_at = obj.optString("created_at", ""),
                        updated_at = obj.optString("updated_at", ""),
                        pengadu = null,
                        admin = null,
                        catatan_admin = obj.optString("catatan_admin", "").ifEmpty { null },
                        ditangani_pada = obj.optString("ditangani_pada", "").ifEmpty { null },
                        diselesaikan_pada = obj.optString("diselesaikan_pada", "").ifEmpty { null }
                    )
                    dataList.add(pengaduan)
                }

                adapter.notifyDataSetChanged()
                b.tvKosong.visibility = if (dataList.isEmpty()) View.VISIBLE else View.GONE
            },
            { error ->
                b.progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal ambil riwayat", Toast.LENGTH_SHORT).show()
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

    private fun tampilkanDetailPengaduan(item: Pengaduan) {
        val dialogBinding = DialogDetailPengaduanBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.tvDetailJudul.text = item.judul_pengaduan
        dialogBinding.tvDetailKategori.text = "Kategori: ${kategoriLabel(item.kategori_aduan)}"
        dialogBinding.tvDetailStatus.text = statusLabel(item.status_pengaduan)
        dialogBinding.tvDetailIsi.text = item.isi_pengaduan
        dialogBinding.tvDetailTanggal.text = "Dibuat: ${formatTanggal(item.created_at)}"

        if (!item.catatan_admin.isNullOrEmpty()) {
            dialogBinding.tvDetailCatatanAdmin.visibility = View.VISIBLE
            dialogBinding.tvDetailCatatanAdmin.text = "Catatan Admin: ${item.catatan_admin}"
        } else {
            dialogBinding.tvDetailCatatanAdmin.visibility = View.GONE
        }

        val penanganan = when {
            item.ditangani_pada != null && item.diselesaikan_pada != null -> {
                "Ditangani: ${formatTanggal(item.ditangani_pada!!)}\nSelesai: ${formatTanggal(item.diselesaikan_pada!!)}"
            }
            item.ditangani_pada != null -> {
                "Ditangani: ${formatTanggal(item.ditangani_pada!!)}"
            }
            else -> "Belum ditangani"
        }
        dialogBinding.tvDetailPenanganan.text = penanganan

        setStatusColor(dialogBinding.tvDetailStatus, item.status_pengaduan)

        val buktiUrl = item.foto_bukti
        if (buktiUrl.isNullOrEmpty()) {
            dialogBinding.ivDetailBukti.visibility = View.GONE
            dialogBinding.tvDetailVideoIndicator.visibility = View.GONE
        } else {
            dialogBinding.ivDetailBukti.visibility = View.VISIBLE
            val fullUrl = "http://192.168.20.226:8000/storage/$buktiUrl"
            val isVideo = buktiUrl.endsWith(".mp4") || buktiUrl.endsWith(".mov") || buktiUrl.endsWith(".3gp")

            if (isVideo) {
                dialogBinding.tvDetailVideoIndicator.visibility = View.VISIBLE
                Glide.with(this)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_media_play)
                    .error(android.R.drawable.ic_media_play)
                    .into(dialogBinding.ivDetailBukti)
            } else {
                dialogBinding.tvDetailVideoIndicator.visibility = View.GONE
                Glide.with(this)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(dialogBinding.ivDetailBukti)
            }
        }

        dialogBinding.btnTutup.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun kategoriLabel(kategori: String): String {
        return when (kategori) {
            "ps_rusak" -> "PS Rusak"
            "pelayanan" -> "Pelayanan"
            "kebersihan" -> "Kebersihan"
            "pembayaran" -> "Pembayaran"
            "fasilitas" -> "Fasilitas"
            "lainnya" -> "Lainnya"
            else -> kategori
        }
    }

    private fun statusLabel(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "Pending"
            "proses" -> "Diproses"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            else -> status
        }
    }

    private fun setStatusColor(tv: android.widget.TextView, status: String) {
        val color = when (status.lowercase()) {
            "pending" -> android.graphics.Color.parseColor("#FB8C00")
            "proses" -> android.graphics.Color.parseColor("#1976D2")
            "selesai" -> android.graphics.Color.parseColor("#4CAF50")
            "dibatalkan" -> android.graphics.Color.parseColor("#E53935")
            else -> android.graphics.Color.parseColor("#1976D2")
        }
        tv.setBackgroundColor(color)
    }

    private fun formatTanggal(tanggal: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id"))
            val date = inputFormat.parse(tanggal)
            outputFormat.format(date)
        } catch (e: Exception) {
            tanggal.take(10)
        }
    }
}