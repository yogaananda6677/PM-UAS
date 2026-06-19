package ananda.yoga.projectuasmobile.config

import ananda.yoga.projectuasmobile.BuildConfig

object ApiConfig {

    const val TIMEOUT = 15000
    val BASE_URL: String = BuildConfig.BASE_URL
    val STORAGE_URL = BASE_URL + "storage"

    val LOGIN = BASE_URL + "login"
    val REGISTER = BASE_URL + "register"

    val GET_PS = BASE_URL + "playstation"
    val GET_PS_TERSEDIA = BASE_URL + "playstation"
    val GET_MONITORING = BASE_URL + "monitoring/pelanggan"

    val GET_PRODUK = BASE_URL + "produk"

    // TRANSAKSI
    val CREATE_TRANSAKSI = BASE_URL + "transaksi"
    val RIWAYAT_PEMESANAN = BASE_URL + "transaksi-saya"

    fun BAYAR_TRANSAKSI(id: String) = BASE_URL + "transaksi/$id/bayar"
    fun TAMBAH_WAKTU(id: String) = BASE_URL + "transaksi/$id/tambah-waktu"
    fun TAMBAH_PRODUK(id: String) = BASE_URL + "transaksi/$id/tambah-produk"

    val CREATE_PENGADUAN = BASE_URL + "pengaduan"
    val RIWAYAT_PENGADUAN = BASE_URL + "pengaduan"
    fun DETAIL_PENGADUAN(id: String) = BASE_URL + "pengaduan/$id"
    fun CANCEL_PENGADUAN(id: String) = BASE_URL + "pengaduan/$id/cancel"

    val UPDATE_PASSWORD = BASE_URL + "user/password"

    // ==================== TRANSAKSI ====================
    fun DETAIL_TRANSAKSI(id: String): String {
        return BASE_URL + "transaksi/" + id
    }
}