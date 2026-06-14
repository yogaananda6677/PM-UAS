package ananda.yoga.projectuasmobile.model

data class ProdukPesanan(
    val idProduk: String,
    val namaProduk: String,
    val harga: Long,
    var qty: Int
)
