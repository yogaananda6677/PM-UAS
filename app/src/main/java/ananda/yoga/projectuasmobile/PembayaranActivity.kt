package ananda.yoga.projectuasmobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.databinding.ActivityPembayaranBinding

class PembayaranActivity : AppCompatActivity() {

    private lateinit var b: ActivityPembayaranBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPembayaranBinding.inflate(layoutInflater)
        setContentView(b.root)

        val idTransaksi = intent.getStringExtra("id_transaksi") ?: ""
        val nomorPs = intent.getStringExtra("nomor_ps") ?: "-"

        b.tvInfoPembayaran.text = "Transaksi: $idTransaksi\nPS: $nomorPs"

        b.btnBayar.setOnClickListener {
            Toast.makeText(this, "Nanti lanjutkan proses bayar ke API", Toast.LENGTH_SHORT).show()
        }

        b.btnKembali.setOnClickListener {
            finish()
        }
    }
}
