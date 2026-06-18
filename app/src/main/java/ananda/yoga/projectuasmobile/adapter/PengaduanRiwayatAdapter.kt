package ananda.yoga.projectuasmobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ananda.yoga.projectuasmobile.R
import ananda.yoga.projectuasmobile.model.Pengaduan
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.SimpleDateFormat
import java.util.Locale

class PengaduanRiwayatAdapter(
    private val context: Context,
    private val list: List<Pengaduan>,
    private val onItemClick: (Pengaduan) -> Unit
) : RecyclerView.Adapter<PengaduanRiwayatAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_riwayat_pengaduan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvJudul = itemView.findViewById<TextView>(R.id.tvJudulPengaduan)
        private val tvStatus = itemView.findViewById<TextView>(R.id.tvStatusPengaduan)
        private val tvKategori = itemView.findViewById<TextView>(R.id.tvKategoriPengaduan)
        private val tvTanggal = itemView.findViewById<TextView>(R.id.tvTanggalPengaduan)
        private val ivBukti = itemView.findViewById<ImageView>(R.id.ivBuktiPreview)
        private val tvVideoIndicator = itemView.findViewById<TextView>(R.id.tvVideoIndicator)

        fun bind(item: Pengaduan) {
            tvJudul.text = item.judul_pengaduan
            tvKategori.text = kategoriLabel(item.kategori_aduan)
            tvTanggal.text = formatTanggal(item.created_at)

            tvStatus.text = statusLabel(item.status_pengaduan)
            setStatusColor(tvStatus, item.status_pengaduan)

            val buktiUrl = item.foto_bukti
            if (buktiUrl.isNullOrEmpty()) {
                ivBukti.visibility = View.GONE
                tvVideoIndicator.visibility = View.GONE
            } else {
                ivBukti.visibility = View.VISIBLE
                val fullUrl = "http://192.168.20.226:8000/storage/$buktiUrl"
                val isVideo = buktiUrl.endsWith(".mp4") || buktiUrl.endsWith(".mov") || buktiUrl.endsWith(".3gp")

                if (isVideo) {
                    tvVideoIndicator.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(fullUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.ic_media_play)
                        .error(android.R.drawable.ic_media_play)
                        .into(ivBukti)
                } else {
                    tvVideoIndicator.visibility = View.GONE
                    Glide.with(context)
                        .load(fullUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivBukti)
                }
            }
        }

        private fun setStatusColor(tv: TextView, status: String) {
            val color = when (status.lowercase()) {
                "pending" -> android.graphics.Color.parseColor("#FB8C00")
                "proses" -> android.graphics.Color.parseColor("#1976D2")
                "selesai" -> android.graphics.Color.parseColor("#4CAF50")
                "dibatalkan" -> android.graphics.Color.parseColor("#E53935")
                else -> android.graphics.Color.parseColor("#1976D2")
            }
            tv.setBackgroundColor(color)
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
}