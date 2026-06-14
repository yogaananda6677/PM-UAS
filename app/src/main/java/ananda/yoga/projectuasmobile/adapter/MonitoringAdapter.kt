package ananda.yoga.projectuasmobile.adapter

import android.content.Context
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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_monitoring_ps, parent, false)

        val item = data[position]

        val tvNomorPs = view.findViewById<TextView>(R.id.tvNomorPs)
        val tvTipePs = view.findViewById<TextView>(R.id.tvTipePs)
        val tvStatusPs = view.findViewById<TextView>(R.id.tvStatusPs)

        tvNomorPs.text = item.nomorPs
        tvTipePs.text = item.tipePs
        tvStatusPs.text = item.statusPs

        when (item.statusPs.lowercase()) {
            "tersedia" -> tvStatusPs.setTextColor(context.getColor(android.R.color.holo_green_dark))
            "digunakan", "dipakai" -> tvStatusPs.setTextColor(context.getColor(android.R.color.holo_orange_dark))
            "maintenance" -> tvStatusPs.setTextColor(context.getColor(android.R.color.holo_red_dark))
            else -> tvStatusPs.setTextColor(context.getColor(android.R.color.black))
        }

        return view
    }
}