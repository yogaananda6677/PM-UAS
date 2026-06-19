package ananda.yoga.projectuasmobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.databinding.ActivityMainBinding
import ananda.yoga.projectuasmobile.fragment.DashboardFragment
import ananda.yoga.projectuasmobile.fragment.MonitoringFragment
import ananda.yoga.projectuasmobile.fragment.ProfilFragment
import ananda.yoga.projectuasmobile.fragment.RiwayatFragment

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

        // ✅ Pakai topToolbar sesuai id di XML
        setSupportActionBar(b.topToolbar)
        supportActionBar?.title = "InfinityPS"

        // Fragment pertama
        replaceFragment(DashboardFragment())

        // Bottom Navigation
        b.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard_menu -> {
                    supportActionBar?.title = "InfinityPS"
                    replaceFragment(DashboardFragment())
                    true
                }
                R.id.monitoring_menu -> {
                    supportActionBar?.title = "Monitoring"
                    replaceFragment(MonitoringFragment())
                    true
                }
                R.id.riwayat_menu -> {
                    supportActionBar?.title = "Riwayat"
                    replaceFragment(RiwayatFragment())
                    true
                }
                R.id.profil_menu -> {
                    supportActionBar?.title = "Profil"
                    replaceFragment(ProfilFragment())
                    true
                }
                else -> false
            }
        }
    }

    // ✅ Inflate menu ke Toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // ✅ Handle klik item menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuProfil -> {
                b.bottomNavigation.selectedItemId = R.id.profil_menu
                true
            }
            R.id.menuLogout -> {
                AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Apakah kamu yakin ingin keluar?")
                    .setPositiveButton("Ya") { _, _ ->
                        getSharedPreferences("user_session", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }
}