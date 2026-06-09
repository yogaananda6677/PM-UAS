package ananda.yoga.projectuasmobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ananda.yoga.projectuasmobile.databinding.ActivityOnBoardingBinding

class OnBoarding : AppCompatActivity() {

    private lateinit var b: ActivityOnBoardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnMulai.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish() // ← tambah ini
        }

        b.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // ← tambah ini
        }
    }
}