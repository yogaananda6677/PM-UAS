package ananda.yoga.projectuasmobile

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.ActivityPengaduanBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PengaduanActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityPengaduanBinding
    private lateinit var token: String

    private var selectedFile: File? = null
    private var currentPhotoPath: String? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 100
        private const val REQUEST_GALLERY = 101
        private const val REQUEST_PERMISSION_CAMERA = 102
        private const val REQUEST_PERMISSION_STORAGE = 103
        private const val TAG = "Pengaduan"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPengaduanBinding.inflate(layoutInflater)
        setContentView(b.root)

        val pref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        token = pref.getString("token", "") ?: ""

        if (token.isEmpty()) {
            Log.e(TAG, "Token kosong! User belum login / sesi habis.")
        }

        setupKategoriSpinner()
        setupTombol()
    }

    private fun setupKategoriSpinner() {
        val kategori = arrayOf(
            "Pilih Kategori",
            "ps_rusak",
            "pelayanan",
            "kebersihan",
            "pembayaran",
            "fasilitas",
            "lainnya"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategori)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spKategori.adapter = adapter
    }

    private fun setupTombol() {
        b.btnAmbilFoto.setOnClickListener(this)
        b.btnSimpanPengaduan.setOnClickListener(this)
        b.btnKembali.setOnClickListener(this)
        b.btnHapusFoto.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnAmbilFoto -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_PERMISSION_CAMERA
                    )
                } else {
                    tampilkanPilihanSumberGambar()
                }
            }
            R.id.btnHapusFoto -> {
                selectedFile = null
                b.ivFotoBukti.setImageDrawable(null)
                b.ivFotoBukti.visibility = View.GONE
                b.btnHapusFoto.visibility = View.GONE
                b.tvNamaFile.text = ""
                b.tvNamaFile.visibility = View.GONE
            }
            R.id.btnSimpanPengaduan -> kirimPengaduan()
            R.id.btnKembali -> finish()
        }
    }

    private fun tampilkanPilihanSumberGambar() {
        val options = arrayOf("Kamera", "Galeri")
        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> ambilFotoDariKamera()
                    1 -> ambilDariGaleri()
                }
            }
            .show()
    }

    // ==================== KAMERA ====================
    private fun ambilFotoDariKamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            // Pastikan ada aplikasi kamera yang bisa menangani intent ini
            if (intent.resolveActivity(packageManager) == null) {
                Toast.makeText(this, "Tidak ada aplikasi kamera ditemukan", Toast.LENGTH_SHORT).show()
                return
            }

            val photoFile = createImageFile()
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    photoFile
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                // Beri izin tulis sementara ke aplikasi kamera pihak ketiga
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            } else {
                Toast.makeText(this, "Gagal membuat file gambar", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "ambilFotoDariKamera error", e)
            Toast.makeText(this, "Error kamera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "createImageFile error", e)
            null
        }
    }

    // ==================== GALERI ====================
    private fun ambilDariGaleri() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (resultCode != RESULT_OK) {
            Log.d(TAG, "Result bukan RESULT_OK, dibatalkan oleh user atau gagal.")
            return
        }

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> handleKameraResult()
            REQUEST_GALLERY -> handleGaleriResult(data)
        }
    }

    private fun handleKameraResult() {
        try {
            val path = currentPhotoPath
            if (path.isNullOrEmpty()) {
                Log.e(TAG, "currentPhotoPath null/kosong")
                Toast.makeText(this, "Gagal: path foto tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }

            val file = File(path)
            Log.d(TAG, "Kamera file: ${file.absolutePath}, exists=${file.exists()}, size=${file.length()}")

            if (!file.exists() || file.length() <= 0) {
                Toast.makeText(this, "File foto dari kamera kosong/tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }

            val compressed = compressImage(file)
            if (compressed != null) {
                selectedFile = compressed
                tampilkanPreview(compressed)
            } else {
                Toast.makeText(this, "Gagal kompres gambar dari kamera", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleKameraResult error", e)
            Toast.makeText(this, "Error memproses foto kamera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleGaleriResult(data: Intent?) {
        try {
            val uri = data?.data
            if (uri == null) {
                Toast.makeText(this, "Tidak ada gambar dipilih", Toast.LENGTH_SHORT).show()
                return
            }
            Log.d(TAG, "Gallery URI: $uri")

            val file = getFileFromUri(uri)
            if (file == null || !file.exists() || file.length() <= 0) {
                Toast.makeText(this, "Gagal mengambil gambar dari galeri", Toast.LENGTH_SHORT).show()
                return
            }

            val compressed = compressImage(file)
            if (compressed != null) {
                selectedFile = compressed
                tampilkanPreview(compressed)
            } else {
                Toast.makeText(this, "Gagal kompres gambar dari galeri", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleGaleriResult error", e)
            Toast.makeText(this, "Error memproses gambar galeri: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== KOMPRESI GAMBAR ====================
    private fun compressImage(file: File): File? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                Log.e(TAG, "Bitmap null saat decode: ${file.absolutePath}")
                return null
            }

            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val compressedFile = File(cacheDir, fileName)

            FileOutputStream(compressedFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, output)
            }
            bitmap.recycle()

            if (!compressedFile.exists() || compressedFile.length() <= 0) {
                Log.e(TAG, "File hasil kompres kosong/tidak ada")
                return null
            }

            Log.d(TAG, "Compressed size: ${compressedFile.length() / 1024} KB at ${compressedFile.absolutePath}")
            compressedFile
        } catch (e: Exception) {
            Log.e(TAG, "compressImage error", e)
            null
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val fileName = getFileName(uri)
            val file = File(cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.e(TAG, "openInputStream null untuk $uri")
                return null
            }
            if (file.exists() && file.length() > 0) file else null
        } catch (e: Exception) {
            Log.e(TAG, "getFileFromUri error", e)
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: ""
            }
        }
        if (name.isEmpty()) {
            name = "file_${System.currentTimeMillis()}.jpg"
        }
        return name
    }

    private fun tampilkanPreview(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap != null) {
            b.ivFotoBukti.setImageBitmap(bitmap)
        } else {
            b.ivFotoBukti.setImageURI(Uri.fromFile(file))
        }
        b.ivFotoBukti.visibility = View.VISIBLE
        b.tvNamaFile.text = "📷 Gambar: ${file.name} (${file.length() / 1024} KB)"
        b.tvNamaFile.visibility = View.VISIBLE
        b.btnHapusFoto.visibility = View.VISIBLE
    }

    // ==================== KIRIM PENGADUAN ====================
    private fun kirimPengaduan() {
        try {
            val judul = b.etJudul.text.toString().trim()
            val isi = b.etIsi.text.toString().trim()
            val kategori = b.spKategori.selectedItem.toString()

            if (judul.isEmpty()) {
                b.etJudul.error = "Judul wajib diisi"
                return
            }
            if (isi.isEmpty()) {
                b.etIsi.error = "Isi pengaduan wajib diisi"
                return
            }
            if (kategori == "Pilih Kategori") {
                Toast.makeText(this, "Pilih kategori", Toast.LENGTH_SHORT).show()
                return
            }
            if (token.isEmpty()) {
                Toast.makeText(this, "Sesi login habis, silakan login ulang", Toast.LENGTH_LONG).show()
                return
            }

            setLoading(true)

            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("judul_pengaduan", judul)
                .addFormDataPart("isi_pengaduan", isi)
                .addFormDataPart("kategori_aduan", kategori)

            val file = selectedFile
            if (file != null) {
                if (!file.exists() || file.length() <= 0) {
                    Toast.makeText(this, "File foto tidak valid, hapus dan ambil ulang", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    return
                }
                Log.d(TAG, "Upload file: ${file.name}, size: ${file.length() / 1024} KB")
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                builder.addFormDataPart("foto_bukti", file.name, file.asRequestBody(mediaType))
            } else {
                Log.d(TAG, "Tidak ada foto dilampirkan, mengirim tanpa foto.")
            }

            val request = Request.Builder()
                .url(ApiConfig.CREATE_PENGADUAN)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .post(builder.build())
                .build()

            Log.d(TAG, "Mengirim request ke: ${request.url}")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        setLoading(false)
                        Log.e(TAG, "Upload failed (${e.javaClass.simpleName})", e)
                        Toast.makeText(
                            this@PengaduanActivity,
                            "Gagal koneksi: ${e.javaClass.simpleName} - ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = try {
                        response.body?.string() ?: "{}"
                    } catch (e: Exception) {
                        Log.e(TAG, "Gagal baca response body", e)
                        "{}"
                    }
                    Log.d(TAG, "Response ${response.code}: $body")

                    runOnUiThread {
                        setLoading(false)
                        if (response.isSuccessful) {
                            try {
                                val json = JSONObject(body)
                                val data = json.optJSONObject("data")
                                val fotoPath = data?.optString("foto_bukti")
                                if (file != null && fotoPath.isNullOrEmpty()) {
                                    Toast.makeText(
                                        this@PengaduanActivity,
                                        "Pengaduan dibuat, tapi bukti foto tidak tersimpan di server!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@PengaduanActivity,
                                        "Pengaduan berhasil dikirim",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Parse response error", e)
                                Toast.makeText(
                                    this@PengaduanActivity,
                                    "Pengaduan berhasil dikirim",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            finish()
                        } else {
                            // Tampilkan pesan error server APA ADANYA, jangan ditelan
                            val msg = try {
                                JSONObject(body).optString("message", body.take(200))
                            } catch (e: Exception) {
                                body.take(200).ifEmpty { "Gagal (${response.code})" }
                            }
                            Log.e(TAG, "Error response ${response.code}: $msg")
                            Toast.makeText(
                                this@PengaduanActivity,
                                "Error ${response.code}: $msg",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            // Tangkap SEMUA exception tak terduga sebelum request terkirim
            // (misal: file kebaca habis dihapus sistem, builder gagal, dsb)
            setLoading(false)
            Log.e(TAG, "kirimPengaduan unexpected error", e)
            Toast.makeText(
                this,
                "Error tak terduga: ${e.javaClass.simpleName} - ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        b.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnSimpanPengaduan.isEnabled = !loading
        b.btnSimpanPengaduan.text = if (loading) "Mengirim..." else "Kirim Pengaduan"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    tampilkanPilihanSumberGambar()
                } else {
                    Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_PERMISSION_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ambilFotoDariKamera()
                } else {
                    Toast.makeText(this, "Izin storage diperlukan", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}