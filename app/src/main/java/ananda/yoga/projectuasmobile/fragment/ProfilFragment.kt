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
import android.widget.PopupMenu
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.EditText
import de.hdodenhof.circleimageview.CircleImageView

class ProfilFragment : Fragment(R.layout.fragment_profil) {

    private lateinit var profileImage: CircleImageView
    private lateinit var sharedPref: android.content.SharedPreferences

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

        val rowName = view.findViewById<View>(R.id.rowName)
        val rowUsername = view.findViewById<View>(R.id.rowUsername)
        val rowEmail = view.findViewById<View>(R.id.rowEmail)

        val tvNameLabel = rowName.findViewById<TextView>(R.id.tvLabel)
        val tvName = rowName.findViewById<TextView>(R.id.tvValue)

        val tvUsernameLabel = rowUsername.findViewById<TextView>(R.id.tvLabel)
        val tvUsername = rowUsername.findViewById<TextView>(R.id.tvValue)

        val tvEmailLabel = rowEmail.findViewById<TextView>(R.id.tvLabel)
        val tvEmail = rowEmail.findViewById<TextView>(R.id.tvValue)

        tvNameLabel.text = "Nama"
        tvName.text = sharedPref.getString("nama", "-")

        tvUsernameLabel.text = "Username"
        tvUsername.text = sharedPref.getString("username", "-")

        tvEmailLabel.text = "Email"
        tvEmail.text = sharedPref.getString("email", "-")

        rowName.setOnLongClickListener {
            showInfoContextMenu(it, "nama", tvName)
            true
        }

        rowUsername.setOnLongClickListener {
            showInfoContextMenu(it, "username", tvUsername)
            true
        }

        rowEmail.setOnLongClickListener {
            showInfoContextMenu(it, "email", tvEmail)
            true
        }

        loadSavedImage()

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        view.findViewById<TextView>(R.id.tvEditPhoto).setOnClickListener {
            showImagePicker()
        }

        view.findViewById<TextView>(R.id.menuPassword).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, GantiPasswordFragment())
                .addToBackStack(null)
                .commit()
        }

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
        val savedUri = sharedPref.getString("profile_image_uri", null)
        if (savedUri != null) {
            profileImage.setImageURI(Uri.parse(savedUri))
            return
        }

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

    private fun showInfoContextMenu(
        anchor: View,
        key: String,
        textView: TextView
    ) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_profile_info, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.menu_copy -> {
                    val clipboard = requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                    val clip = ClipData.newPlainText(
                        key,
                        textView.text.toString()
                    )

                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(
                        requireContext(),
                        "Berhasil dicopy",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }

                R.id.menu_edit -> {
                    showEditDialog(key, textView)
                    true
                }

                else -> false
            }
        }

        popup.show()
    }
    private fun showEditDialog(
        key: String,
        textView: TextView
    ) {
        val editText = EditText(requireContext())
        editText.setText(textView.text.toString())
        editText.setSelection(editText.text.length)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Data")
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val newValue = editText.text.toString().trim()

                if (newValue.isNotEmpty()) {
                    sharedPref.edit().putString(key, newValue).apply()
                    textView.text = newValue

                    Toast.makeText(
                        requireContext(),
                        "Data berhasil diubah",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
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