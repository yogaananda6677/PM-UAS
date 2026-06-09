package ananda.yoga.projectuasmobile.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.R

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGreeting(view)
        setupStatistik(view)
        setupAktivitasTerakhir(view)
    }

    private fun setupGreeting(view: View) {
        val sharedPref = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)

        val nama = sharedPref.getString("nama", "Pengguna") ?: "Pengguna"

        // ✅ Hapus tvAvatar, cukup tampilkan nama
        view.findViewById<TextView>(R.id.tvGreetingName).text = nama

        val jumlahPesan = sharedPref.getInt("jumlah_pemesanan", 0)
        val layoutBanner = view.findViewById<View>(R.id.layoutNotifBanner)
        val tvBanner = view.findViewById<TextView>(R.id.tvNotifBanner)

        if (jumlahPesan > 0) {
            layoutBanner.visibility = View.VISIBLE
            tvBanner.text = "Hei, $nama! Kamu sudah memesan PS sebanyak $jumlahPesan kali."
        } else {
            layoutBanner.visibility = View.GONE
        }
    }

    private fun setupStatistik(view: View) {
        val sharedPref = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)

        view.findViewById<TextView>(R.id.tvStatPemesanan).text =
            sharedPref.getInt("jumlah_pemesanan", 0).toString()

        view.findViewById<TextView>(R.id.tvStatPengaduan).text =
            sharedPref.getInt("jumlah_pengaduan", 0).toString()

        view.findViewById<TextView>(R.id.tvStatSelesai).text =
            sharedPref.getInt("jumlah_selesai", 0).toString()
    }

    private fun setupAktivitasTerakhir(view: View) {
        val sharedPref = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val jumlahPesan = sharedPref.getInt("jumlah_pemesanan", 0)

        val aktivitasList = if (jumlahPesan == 0) {
            listOf("Belum ada aktivitas")
        } else {
            listOf(
                "Pemesanan PS-3 | Selesai",
                "Pengaduan Joystick | Diproses",
                "Pemesanan PS-1 | Aktif"
            )
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            aktivitasList
        )
        view.findViewById<ListView>(R.id.lvAktivitas).adapter = adapter
    }

    companion object {
        @JvmStatic
        fun newInstance() = DashboardFragment()
    }
}