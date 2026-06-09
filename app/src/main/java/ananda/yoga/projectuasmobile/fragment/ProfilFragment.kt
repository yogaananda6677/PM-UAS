package ananda.yoga.projectuasmobile.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.LoginActivity
import ananda.yoga.projectuasmobile.R
import de.hdodenhof.circleimageview.CircleImageView

class ProfilFragment : Fragment(R.layout.fragment_profil) {

    private lateinit var profileImage: CircleImageView
    private lateinit var sharedPref: android.content.SharedPreferences

    // Launcher kamera — hasilnya Bitmap langsung
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap)
                // Simpan bitmap ke file internal lalu simpan path-nya
                saveBitmapToFile(bitmap)
                Toast.makeText(requireContext(), "Foto berhasil diambil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        profileImage = view.findViewById(R.id.profileImage)

        // Set label & value
        view.findViewById<View>(R.id.rowName)
            .findViewById<TextView>(R.id.tvLabel).text = "Nama"
        view.findViewById<View>(R.id.rowName)
            .findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("nama", "-")

        view.findViewById<View>(R.id.rowUsername)
            .findViewById<TextView>(R.id.tvLabel).text = "Username"
        view.findViewById<View>(R.id.rowUsername)
            .findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("username", "-")

        view.findViewById<View>(R.id.rowEmail)
            .findViewById<TextView>(R.id.tvLabel).text = "Email"
        view.findViewById<View>(R.id.rowEmail)
            .findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("email", "-")

        // Load foto profil tersimpan
        loadSavedImage()

        // Tombol back
        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Edit foto → langsung buka kamera
        view.findViewById<TextView>(R.id.tvEditPhoto).setOnClickListener {
            showImagePicker()
        }

        // Ganti password
        view.findViewById<TextView>(R.id.menuPassword).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, GantiPasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        // Logout
        view.findViewById<TextView>(R.id.menuLogout).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showImagePicker() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Foto")
            .setItems(arrayOf("Ambil dari Kamera", "Pilih dari Galeri", "Hapus Foto")) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkGalleryPermission()
                    2 -> {
                        sharedPref.edit()
                            .remove("profile_image_path")
                            .remove("profile_image_uri")
                            .apply()
                        profileImage.setImageResource(R.drawable.ic_person_24)
                        Toast.makeText(requireContext(), "Foto profil dihapus", Toast.LENGTH_SHORT).show()
                    }
                }
            }.show()
    }
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        } else {
            openCamera()
        }
    }
    private fun checkGalleryPermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), 102)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    // Tambah launcher galeri di atas cameraLauncher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                profileImage.setImageURI(imageUri)
                sharedPref.edit().putString("profile_image_uri", imageUri.toString()).apply()
                Toast.makeText(requireContext(), "Foto berhasil dipilih", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap) {
        try {
            val filename = "profile_photo.jpg"
            requireContext().openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            sharedPref.edit().putString("profile_image_path", filename).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSavedImage() {
        // Cek dulu dari galeri (URI)
        val savedUri = sharedPref.getString("profile_image_uri", null)
        if (savedUri != null) {
            profileImage.setImageURI(Uri.parse(savedUri))
            return
        }

        // Kalau tidak ada, cek dari kamera (file)
        val filename = sharedPref.getString("profile_image_path", null)
        if (filename != null) {
            try {
                requireContext().openFileInput(filename).use { fis ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(fis)
                    profileImage.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah kamu yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                sharedPref.edit().clear().apply()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
}