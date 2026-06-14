package ananda.yoga.projectuasmobile.model

data class Produk(
    val idProduk: String,
    val namaProduk: String,
    val harga: Long,
    val stok: Int
) {
    override fun toString(): String {
        if (idProduk.isEmpty()) {
            return namaProduk
        }

        return namaProduk + " - Rp " + String.format("%,d", harga).replace(",", ".")
    }
}
