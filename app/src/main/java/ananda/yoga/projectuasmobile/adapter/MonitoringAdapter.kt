package ananda.yoga.projectuasmobile.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import ananda.yoga.projectuasmobile.R
import ananda.yoga.projectuasmobile.model.MonitoringPs

class MonitoringAdapter(

    private val context: Context,
    private val data: ArrayList<MonitoringPs>

) : BaseAdapter() {

    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Any = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {

        val view = convertView ?: LayoutInflater
            .from(context)
            .inflate(R.layout.item_monitoring_ps, parent, false)

        val item = data[position]

        val tvNomor = view.findViewById<TextView>(R.id.tvNomorPs)
        val tvTipe = view.findViewById<TextView>(R.id.tvTipePs)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatusPs)
        val tvPelanggan = view.findViewById<TextView>(R.id.tvPelanggan)
        val tvJam = view.findViewById<TextView>(R.id.tvJam)
        val tvBayar = view.findViewById<TextView>(R.id.tvBayar)

        tvNomor.text = "🎮 ${item.nomorPs}"
        tvTipe.text = "🎮 Tipe : ${item.tipePs}"

        tvPelanggan.text =
            if (item.namaPelanggan.isBlank())
                "👤 Pelanggan : -"
            else
                "👤 Pelanggan : ${item.namaPelanggan}"

        val jam =
            if (item.jamMulai.isBlank() || item.jamSelesai.isBlank())
                "-"
            else
                "${item.jamMulai} - ${item.jamSelesai}"

        tvJam.text = "🕒 Jam : $jam"

        tvBayar.text =
            if (item.statusBayar.isBlank())
                "💰 Pembayaran : -"
            else
                "💰 Pembayaran : ${item.statusBayar.uppercase()}"

        tvStatus.text = item.statusPs.uppercase()

        when (item.statusPs.lowercase()) {

            "tersedia" -> {

                tvStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                tvStatus.setTextColor(Color.WHITE)

            }

            "digunakan",
            "dipakai" -> {

                tvStatus.setBackgroundColor(Color.parseColor("#FB8C00"))
                tvStatus.setTextColor(Color.WHITE)

            }

            "maintenance" -> {

                tvStatus.setBackgroundColor(Color.parseColor("#E53935"))
                tvStatus.setTextColor(Color.WHITE)

            }

            "waiting",
            "menunggu" -> {

                tvStatus.setBackgroundColor(Color.parseColor("#1976D2"))
                tvStatus.setTextColor(Color.WHITE)

            }

            else -> {

                tvStatus.setBackgroundColor(Color.GRAY)
                tvStatus.setTextColor(Color.WHITE)

            }

        }

        if (item.statusBayar.equals("lunas", true)) {

            tvBayar.setTextColor(Color.parseColor("#2E7D32"))

        } else {

            tvBayar.setTextColor(Color.parseColor("#D32F2F"))

        }

        return view

    }

}