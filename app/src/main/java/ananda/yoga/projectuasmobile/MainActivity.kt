package ananda.yoga.projectuasmobile

import ananda.yoga.projectuasmobile.databinding.ActivityMainBinding
import ananda.yoga.projectuasmobile.fragment.DashboardFragment
import ananda.yoga.projectuasmobile.fragment.MonitoringFragment
import ananda.yoga.projectuasmobile.fragment.ProfilFragment
import ananda.yoga.projectuasmobile.fragment.RiwayatFragment
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fragment pertama yang tampil saat aplikasi dibuka
        replaceFragment(DashboardFragment())

        // Event klik menu bottom navigation
        b.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard_menu -> {
                    replaceFragment(DashboardFragment())
                    true
                }

                R.id.monitoring_menu -> {
                    replaceFragment(MonitoringFragment())
                    true
                }

                R.id.riwayat_menu -> {
                    replaceFragment(RiwayatFragment())
                    true
                }

                R.id.profil_menu -> {
                    replaceFragment(ProfilFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }
}