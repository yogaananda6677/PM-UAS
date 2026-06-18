package ananda.yoga.projectuasmobile

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
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
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PengaduanActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityPengaduanBinding
    private lateinit var token: String

    private var selectedFile: File? = null
    private var isVideo = false

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 100
        private const val REQUEST_GALLERY = 101
        private const val REQUEST_PERMISSION_CAMERA = 102
        private const val REQUEST_PERMISSION_STORAGE = 103
        private const val TAG = "PengaduanActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPengaduanBinding.inflate(layoutInflater)
        setContentView(b.root)

        val pref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        token = pref.getString("token", "") ?: ""

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
                    != PackageManager.PERMISSION_GRANTED) {
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
                isVideo = false
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
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        if (photoFile != null) {
            val photoURI = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "Gagal membuat file gambar", Toast.LENGTH_SHORT).show()
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
            )
        } catch (e: Exception) {
            null
        }
    }

    // ==================== GALERI ====================
    private fun ambilDariGaleri() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "image/*",
                "video/mp4", "video/mov", "video/3gp"
            ))
        }
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    // Ambil dari file yang dibuat sebelumnya (createImageFile)
                    val file = File(cacheDir, "temp_kamera_${System.currentTimeMillis()}.jpg")
                    // Kita perlu ambil file dari kamera: biasanya tersimpan di external files dir
                    // Karena kita pakai FileProvider, kita cari file terakhir di external files
                    val externalFilesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val files = externalFilesDir?.listFiles()?.sortedByDescending { it.lastModified() }
                    val latestFile = files?.firstOrNull { it.extension == "jpg" || it.extension == "jpeg" }
                    if (latestFile != null && latestFile.exists()) {
                        // Kompres dan salin ke cache
                        val compressed = compressImageToCache(latestFile)
                        if (compressed != null) {
                            selectedFile = compressed
                            isVideo = false
                            tampilkanPreview(compressed)
                        } else {
                            Toast.makeText(this, "Gagal mengompres gambar", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Gagal mengambil gambar dari kamera", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_GALLERY -> {
                    data?.data?.let { uri ->
                        val mimeType = contentResolver.getType(uri)
                        isVideo = mimeType?.startsWith("video/") == true

                        if (isVideo) {
                            // Video: salin ke cache
                            val file = copyUriToCache(uri)
                            if (file != null) {
                                selectedFile = file
                                tampilkanPreview(file)
                            } else {
                                Toast.makeText(this, "Gagal mengambil video", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Gambar: kompres
                            val bitmap = getBitmapFromUri(uri)
                            if (bitmap != null) {
                                val compressed = compressBitmapToCache(bitmap)
                                if (compressed != null) {
                                    selectedFile = compressed
                                    isVideo = false
                                    tampilkanPreview(compressed)
                                } else {
                                    Toast.makeText(this, "Gagal mengompres gambar", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "Gagal membaca gambar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun compressImageToCache(sourceFile: File): File? {
        val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
        if (bitmap == null) {
            Log.e(TAG, "Gagal decode bitmap dari file: ${sourceFile.absolutePath}")
            return null
        }
        return compressBitmapToCache(bitmap)
    }

    private fun compressBitmapToCache(bitmap: Bitmap): File? {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)
        try {
            FileOutputStream(file).use { output ->
                // Kompres dengan kualitas 30% (lebih kecil)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, output)
            }
            Log.d(TAG, "File ukuran: ${file.length() / 1024} KB")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Gagal kompres gambar", e)
            return null
        }
    }

    private fun copyUriToCache(uri: Uri): File? {
        val fileName = getFileName(uri)
        val file = File(cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Video ukuran: ${file.length() / 1024} KB")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Gagal copy video", e)
            return null
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
            name = uri.path?.substringAfterLast("/") ?: "file_${System.currentTimeMillis()}"
        }
        return name
    }

    private fun tampilkanPreview(file: File) {
        if (isVideo) {
            b.ivFotoBukti.setImageResource(android.R.drawable.ic_media_play)
            b.tvNamaFile.text = "🎬 Video: ${file.name}"
        } else {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                b.ivFotoBukti.setImageBitmap(bitmap)
                b.tvNamaFile.text = "📷 Gambar: ${file.name}"
            } else {
                // Fallback: coba load dari file
                b.ivFotoBukti.setImageURI(Uri.fromFile(file))
                b.tvNamaFile.text = "📷 Gambar: ${file.name}"
            }
        }
        b.ivFotoBukti.visibility = View.VISIBLE
        b.tvNamaFile.visibility = View.VISIBLE
        b.btnHapusFoto.visibility = View.VISIBLE
    }

    // ==================== KIRIM PENGADUAN ====================
    private fun kirimPengaduan() {
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

        setLoading(true)

        val client = OkHttpClient()
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("judul_pengaduan", judul)
            .addFormDataPart("isi_pengaduan", isi)
            .addFormDataPart("kategori_aduan", kategori)

        selectedFile?.let { file ->
            if (file.exists()) {
                val mediaType = if (isVideo) {
                    "video/mp4".toMediaTypeOrNull()
                } else {
                    "image/jpeg".toMediaTypeOrNull()
                }
                builder.addFormDataPart("foto_bukti", file.name, file.asRequestBody(mediaType))
                Log.d(TAG, "Upload file: ${file.name}, ukuran: ${file.length() / 1024} KB")
            } else {
                Log.e(TAG, "File tidak ditemukan: ${file.absolutePath}")
                Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show()
                setLoading(false)
                return
            }
        }

        val request = Request.Builder()
            .url(ApiConfig.CREATE_PENGADUAN)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .post(builder.build())
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    setLoading(false)
                    Toast.makeText(
                        this@PengaduanActivity,
                        "Gagal: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string() ?: "{}"
                runOnUiThread {
                    setLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@PengaduanActivity,
                            "Pengaduan berhasil dikirim",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        val msg = try {
                            JSONObject(body).optString("message", "Gagal kirim pengaduan")
                        } catch (e: Exception) {
                            "Gagal kirim pengaduan"
                        }
                        Toast.makeText(this@PengaduanActivity, "Error ${response.code}: $msg", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Upload gagal: code=${response.code}, body=$body")
                    }
                }
            }
        })
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