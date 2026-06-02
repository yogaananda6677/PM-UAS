package ananda.yoga.projectuasmobile.config

import ananda.yoga.projectuasmobile.BuildConfig

object ApiConfig {

    const val TIMEOUT = 15000

    val BASE_URL: String = BuildConfig.BASE_URL

    val LOGIN = BASE_URL + "login.php"
    val REGISTER = BASE_URL + "register.php"

    val GET_PS = BASE_URL + "ps/get_ps.php"
    val GET_PS_TERSEDIA = BASE_URL + "ps/get_ps_tersedia.php"

    val CREATE_PEMESANAN = BASE_URL + "pemesanan/create.php"
    val RIWAYAT_PEMESANAN = BASE_URL + "pemesanan/riwayat.php"

    val CREATE_PENGADUAN = BASE_URL + "pengaduan/create.php"
    val RIWAYAT_PENGADUAN = BASE_URL + "pengaduan/riwayat.php"
}