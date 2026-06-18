package ananda.yoga.projectuasmobile.model

data class MonitoringPs(
    val idPs: String,
    val nomorPs: String,
    val tipePs: String,
    val statusPs: String,
    val hargaSewa: Long,
    val idTransaksi: String,
    val idUserTransaksi: String,
    val namaPelanggan: String,
    val jamMulai: String,
    val jamSelesai: String,
    val statusBayar: String,
    val statusTransaksi: String,
    // tambahan untuk detail
    val durasiMenit: Int = 0,
    val jamMulaiFull: String = "",
    val jamSelesaiFull: String = ""
)
