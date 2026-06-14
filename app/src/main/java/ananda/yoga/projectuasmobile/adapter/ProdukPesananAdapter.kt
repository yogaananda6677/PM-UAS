package ananda.yoga.projectuasmobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import ananda.yoga.projectuasmobile.R
import ananda.yoga.projectuasmobile.model.ProdukPesanan

class ProdukPesananAdapter(
    private val context: Context,
    private val data: ArrayList<ProdukPesanan>,
    private val onJumlahBerubah: () -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Any = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_produk_pesanan, parent, false)

        val produk = data[position]

        val tvNamaProduk = view.findViewById<TextView>(R.id.tvNamaProduk)
        val tvHargaProduk = view.findViewById<TextView>(R.id.tvHargaProduk)
        val tvQtyProduk = view.findViewById<TextView>(R.id.tvQtyProduk)
        val btnKurang = view.findViewById<Button>(R.id.btnKurang)
        val btnTambah = view.findViewById<Button>(R.id.btnTambah)

        tvNamaProduk.text = produk.namaProduk
        tvHargaProduk.text = formatRupiah(produk.harga) + " x " + produk.qty
        tvQtyProduk.text = produk.qty.toString()

        btnTambah.setOnClickListener {
            produk.qty++
            notifyDataSetChanged()
            onJumlahBerubah()
        }

        btnKurang.setOnClickListener {
            produk.qty--

            if (produk.qty <= 0) {
                data.removeAt(position)
            }

            notifyDataSetChanged()
            onJumlahBerubah()
        }

        return view
    }

    private fun formatRupiah(value: Long): String {
        return "Rp " + String.format("%,d", value).replace(",", ".")
    }
}
