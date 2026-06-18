package ananda.yoga.projectuasmobile.model

data class Pengaduan(
    val id: Int,
    val judul_pengaduan: String,
    val isi_pengaduan: String,
    val kategori_aduan: String,
    val status_pengaduan: String,
    val foto_bukti: String?,
    val created_at: String,
    val updated_at: String,
    val pengadu: Pengadu?,
    val admin: Pengadu?,
    val catatan_admin: String?,
    val ditangani_pada: String?,
    val diselesaikan_pada: String?
)

data class Pengadu(
    val id_user: Int,
    val name: String,
    val username: String,
    val email: String
)