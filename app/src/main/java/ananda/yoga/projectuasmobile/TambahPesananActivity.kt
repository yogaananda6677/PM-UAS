package ananda.yoga.projectuasmobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.databinding.ActivityTambahPesananBinding

class TambahPesananActivity : AppCompatActivity() {

    private lateinit var b: ActivityTambahPesananBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTambahPesananBinding.inflate(layoutInflater)
        setContentView(b.root)

        val idTransaksi = intent.getStringExtra("id_transaksi") ?: ""
        val nomorPs = intent.getStringExtra("nomor_ps") ?: "-"

        b.tvInfoTambah.text = "Tambah produk/waktu\nTransaksi: $idTransaksi\nPS: $nomorPs"

        b.btnSimpanTambah.setOnClickListener {
            Toast.makeText(this, "Nanti lanjutkan tambah produk/waktu ke API", Toast.LENGTH_SHORT).show()
        }

        b.btnKembali.setOnClickListener {
            finish()
        }
    }
}
