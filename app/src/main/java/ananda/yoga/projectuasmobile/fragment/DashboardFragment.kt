package ananda.yoga.projectuasmobile.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.PengaduanActivity
import ananda.yoga.projectuasmobile.PengaduanRiwayatActivity
import ananda.yoga.projectuasmobile.R
import ananda.yoga.projectuasmobile.config.ApiConfig
import ananda.yoga.projectuasmobile.databinding.FragmentDashboardBinding
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mumayank.com.airlocationlibrary.AirLocation
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class DashboardFragment : Fragment(), View.OnClickListener {

    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!

    private lateinit var token: String
    private lateinit var idUser: String
    private lateinit var map: MapView

    private val rentalLat = -7.9243442
    private val rentalLon = 112.1322567

    private var lat = 0.0
    private var lng = 0.0
    private val arrayItemPos = ArrayList<OverlayItem>()

    private var airLocation: AirLocation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentDashboardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pref = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        token = pref.getString("token", "") ?: ""
        idUser = pref.getString("id_user", "") ?: ""

        val name = pref.getString("nama", "User")
        b.tvGreetingName.text = "Hai, $name"


        airLocation = AirLocation(
            requireActivity(),
            object : AirLocation.Callback {

                override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
                    Log.e("GPS", "Gagal: $locationFailedEnum")

                }

                override fun onSuccess(locations: ArrayList<Location>) {
                    lat = locations[0].latitude
                    lng = locations[0].longitude

                    Log.d("GPS", "Berhasil: lat=$lat, lng=$lng")

                    val lokasiRental = Location("rental").apply {
                        latitude = rentalLat
                        longitude = rentalLon
                    }
                    val lokasiUser = Location("user").apply {
                        latitude = lat
                        longitude = lng
                    }

                    val jarakMeter = lokasiUser.distanceTo(lokasiRental)
                    val jarakText = if (jarakMeter >= 1000) {
                        String.format("%.2f km", jarakMeter / 1000)
                    } else {
                        String.format("%.0f meter", jarakMeter)
                    }

                    if (_b != null) {
                        b.tvJarak.visibility = View.VISIBLE
                        b.tvJarak.text = "📍 Lat: $lat\n📍 Lng: $lng\n📏 Jarak ke Rental: $jarakText"
                    }

                    drawMap()
                }
            },
            isLocationRequiredOnlyOneTime = false
        )

        b.btnBuatPengaduan.setOnClickListener(this)
        b.btnRiwayatPengaduan.setOnClickListener(this)

        b.btnLokasiSaya.setOnClickListener {
            Log.d("GPS", "Tombol lokasi ditekan, start AirLocation")
            Toast.makeText(requireContext(), "Mengambil lokasi...", Toast.LENGTH_SHORT).show()
            airLocation?.start()
        }

        getStatistikPengaduan()
        getAktivitasTerbaru()
        setupMap()
    }

    private fun setupMap() {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        map = b.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val rentalPoint = GeoPoint(rentalLat, rentalLon)
        map.controller.setZoom(16.0)
        map.controller.setCenter(rentalPoint)

        arrayItemPos.add(
            OverlayItem("Infinity PlayStation", "Lokasi Rental PS", GeoPoint(rentalLat, rentalLon))
        )

        map.overlays.add(
            ItemizedIconOverlay(
                arrayItemPos,
                object : OnItemGestureListener<OverlayItem> {
                    override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                        Toast.makeText(requireContext(), item.title, Toast.LENGTH_SHORT).show()
                        return true
                    }
                    override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                        Toast.makeText(requireContext(), item.snippet, Toast.LENGTH_SHORT).show()
                        return true
                    }
                },
                requireContext()
            )
        )

        map.invalidate()
    }

    private fun drawMap() {
        if (!::map.isInitialized) return

        val compass = CompassOverlay(
            requireContext(),
            InternalCompassOrientationProvider(requireContext()),
            map
        )
        compass.enableCompass()
        map.overlays.clear()
        map.overlays.add(0, compass)

        val gps = GpsMyLocationProvider(requireContext())
        val me = MyLocationNewOverlay(gps, map)
        me.enableMyLocation()
        map.overlays.add(1, me)

        val userPoint = GeoPoint(lat, lng)
        map.controller.setZoom(18.0)
        map.controller.animateTo(userPoint)

        // Tambah kembali marker rental
        arrayItemPos.clear()
        arrayItemPos.add(
            OverlayItem("Infinity PlayStation", "Lokasi Rental PS", GeoPoint(rentalLat, rentalLon))
        )

        map.overlays.add(
            ItemizedIconOverlay(
                arrayItemPos,
                object : OnItemGestureListener<OverlayItem> {
                    override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                        Toast.makeText(requireContext(), item.title, Toast.LENGTH_SHORT).show()
                        return true
                    }
                    override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                        Toast.makeText(requireContext(), item.snippet, Toast.LENGTH_SHORT).show()
                        return true
                    }
                },
                requireContext()
            )
        )

        map.invalidate()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnBuatPengaduan -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                } else {
                    startActivity(Intent(requireContext(), PengaduanActivity::class.java))
                }
            }
            R.id.btnRiwayatPengaduan -> {
                startActivity(Intent(requireContext(), PengaduanRiwayatActivity::class.java))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airLocation?.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(requireContext(), PengaduanActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "Kamera diperlukan untuk foto bukti", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airLocation?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::map.isInitialized) map.onPause()
    }

    private fun getStatistikPengaduan() {
        if (token.isEmpty()) return
        val request = object : JsonObjectRequest(
            Request.Method.GET, ApiConfig.RIWAYAT_PENGADUAN, null,
            { response ->
                if (_b != null && isAdded) {
                    val dataArray = response.optJSONArray("data") ?: JSONArray()
                    var total = 0; var pending = 0; var proses = 0; var selesai = 0
                    for (i in 0 until dataArray.length()) {
                        val obj = dataArray.getJSONObject(i)
                        total++
                        when (obj.optString("status_pengaduan", "pending").lowercase()) {
                            "pending" -> pending++
                            "proses" -> proses++
                            "selesai" -> selesai++
                        }
                    }
                    b.tvStatPengaduanTotal.text = total.toString()
                    b.tvStatPengaduanPending.text = pending.toString()
                    b.tvStatPengaduanProses.text = proses.toString()
                    b.tvStatPengaduanSelesai.text = selesai.toString()
                    b.cardStatPengaduan.visibility = View.VISIBLE
                }
            },
            { _ ->
                if (_b != null && isAdded) {
                    Toast.makeText(requireContext(), "Gagal ambil statistik", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getHeaders() = hashMapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            )
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun getAktivitasTerbaru() {
        if (token.isEmpty()) return
        val request = object : JsonObjectRequest(
            Request.Method.GET, ApiConfig.RIWAYAT_PEMESANAN, null,
            { response ->
                if (_b != null && isAdded) {
                    val dataArray = response.optJSONArray("data") ?: JSONArray()
                    val items = mutableListOf<String>()

                    for (i in 0 until minOf(3, dataArray.length())) {
                        val obj = dataArray.getJSONObject(i)
                        val detailSewa = obj.optJSONArray("detail_sewa")
                        val sewa = detailSewa?.optJSONObject(0)
                        val namaPS = sewa?.optString("tipe_ps", "PS") ?: "PS"
                        val nomorPS = sewa?.optJSONObject("playstation")
                            ?.optString("nomor_ps", "") ?: ""
                        val status = obj.optString("status_transaksi", "-")
                        val totalHarga = obj.optDouble("total_harga", 0.0).toLong()
                        val formatRupiah = java.text.NumberFormat
                            .getCurrencyInstance(java.util.Locale("id", "ID"))
                        val statusLabel = when (status.lowercase()) {
                            "selesai" -> "✅ Selesai"
                            "waiting" -> "⏳ Menunggu"
                            "aktif"   -> "🎮 Aktif"
                            "batal"   -> "❌ Batal"
                            else      -> status
                        }
                        items.add("$namaPS $nomorPS | $statusLabel | ${formatRupiah.format(totalHarga)}")
                    }

                    if (items.isEmpty()) items.add("Belum ada transaksi")

                    b.lvAktivitas.adapter = android.widget.ArrayAdapter(
                        requireContext(), android.R.layout.simple_list_item_1, items
                    )
                }
            },
            { _ -> }
        ) {
            override fun getHeaders() = hashMapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            )
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}