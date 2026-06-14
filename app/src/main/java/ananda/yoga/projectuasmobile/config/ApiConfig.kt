package ananda.yoga.projectuasmobile.config

import ananda.yoga.projectuasmobile.BuildConfig

object ApiConfig {

    const val TIMEOUT = 15000

    val BASE_URL: String = BuildConfig.BASE_URL

    val LOGIN = BASE_URL + "login"
    val REGISTER = BASE_URL + "register"

    val GET_PS = BASE_URL + "ps/get_ps"
    val GET_PS_TERSEDIA = BASE_URL + "ps/get_ps_tersedia"
    val GET_MONITORING = BASE_URL + "monitoring/pelanggan"

    val GET_PRODUK = BASE_URL + "produk"
    val CREATE_TRANSAKSI = BASE_URL + "transaksi"

    val CREATE_PEMESANAN = BASE_URL + "pemesanan/create"
    val RIWAYAT_PEMESANAN = BASE_URL + "pemesanan/riwayat"

    val CREATE_PENGADUAN = BASE_URL + "pengaduan/create"
    val RIWAYAT_PENGADUAN = BASE_URL + "pengaduan/riwayat"
    val UPDATE_PASSWORD = BASE_URL + "user/password"

    fun BAYAR_TRANSAKSI(idTransaksi: String): String {
        return BASE_URL + "transaksi/" + idTransaksi + "/bayar"
    }

    fun TAMBAH_WAKTU(idTransaksi: String): String {
        return BASE_URL + "transaksi/" + idTransaksi + "/tambah-waktu"
    }

    fun TAMBAH_PRODUK(idTransaksi: String): String {
        return BASE_URL + "transaksi/" + idTransaksi + "/tambah-produk"
    }
}
