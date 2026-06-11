package ananda.yoga.projectuasmobile.fragment

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ananda.yoga.projectuasmobile.R
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import mumayank.com.airlocationlibrary.AirLocation
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class DashboardFragment : Fragment() {

    private var player: ExoPlayer? = null
    private lateinit var mapView: MapView
    private lateinit var tvJarak: TextView
    private lateinit var airLocation: AirLocation

    private val lokasiPS = GeoPoint(-7.9797, 112.0577)
    private val namaPS   = "InfinityPS Rental"
    private val alamatPS = "Tawang, Kec. Wates, Kab. Kediri"

    private val arrayItemPos = ArrayList<OverlayItem>()
    private var lat = 0.0
    private var lng = 0.0

    // ======================================================
    // LIFECYCLE
    // ======================================================

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvJarak = view.findViewById(R.id.tvJarak)

        setupGreeting(view)
        setupStatistik(view)
        setupAktivitasTerakhir(view)
        setupVideo(view)
        setupMap(view)
        setupAirLocation()
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) mapView.onPause()
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        if (::mapView.isInitialized) mapView.onDetach()
    }

    // ======================================================
    // SETUP FUNCTIONS
    // ======================================================

    private fun setupGreeting(view: View) {
        val sharedPref = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val nama = sharedPref.getString("nama", "Pengguna") ?: "Pengguna"

        view.findViewById<TextView>(R.id.tvGreetingName).text = nama

        val jumlahPesan  = sharedPref.getInt("jumlah_pemesanan", 0)
        val layoutBanner = view.findViewById<View>(R.id.layoutNotifBanner)
        val tvBanner     = view.findViewById<TextView>(R.id.tvNotifBanner)

        if (jumlahPesan > 0) {
            layoutBanner.visibility = View.VISIBLE
            tvBanner.text = "Hei, $nama! Kamu sudah memesan PS sebanyak $jumlahPesan kali."
        } else {
            layoutBanner.visibility = View.GONE
        }
    }

    private fun setupStatistik(view: View) {
        val sharedPref = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)

        view.findViewById<TextView>(R.id.tvStatPemesanan).text =
            sharedPref.getInt("jumlah_pemesanan", 0).toString()
        view.findViewById<TextView>(R.id.tvStatPengaduan).text =
            sharedPref.getInt("jumlah_pengaduan", 0).toString()
        view.findViewById<TextView>(R.id.tvStatSelesai).text =
            sharedPref.getInt("jumlah_selesai", 0).toString()
    }

    private fun setupAktivitasTerakhir(view: View) {
        val sharedPref  = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val jumlahPesan = sharedPref.getInt("jumlah_pemesanan", 0)

        val aktivitasList = if (jumlahPesan == 0) {
            listOf("Belum ada aktivitas")
        } else {
            listOf(
                "Pemesanan PS-3 | Selesai",
                "Pengaduan Joystick | Diproses",
                "Pemesanan PS-1 | Aktif"
            )
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            aktivitasList
        )
        view.findViewById<ListView>(R.id.lvAktivitas).adapter = adapter
    }

    private fun setupVideo(view: View) {
        val rawId = resources.getIdentifier("testimoni", "raw", requireContext().packageName)
        val playerView = view.findViewById<PlayerView>(R.id.playerView)

        if (rawId == 0) {
            playerView.visibility = View.GONE
            return
        }

        player = ExoPlayer.Builder(requireContext()).build()
        playerView.player = player

        val videoUri  = "android.resource://${requireContext().packageName}/$rawId"
        val mediaItem = MediaItem.fromUri(videoUri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
    }

    private fun setupMap(view: View) {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        mapView = view.findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.animateTo(lokasiPS)

        // Marker lokasi PS
        arrayItemPos.add(OverlayItem(namaPS, alamatPS, lokasiPS))
        mapView.overlays.add(
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
    }

    private fun setupAirLocation() {
        val locationManager = requireContext()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!gpsEnabled) {
            tvJarak.text = "Aktifkan GPS di HP kamu"
            return
        }

        airLocation = AirLocation(
            requireActivity(),
            object : AirLocation.Callback {
                override fun onSuccess(locations: ArrayList<Location>) {
                    lat = locations[0].latitude
                    lng = locations[0].longitude
                    drawMapWithUserLocation()
                }
                override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
                    tvJarak.text = "Gagal: $locationFailedEnum"
                }
            },
            isLocationRequiredOnlyOneTime = true
        )
        airLocation.start()
    }

    // ======================================================
    // MAP FUNCTIONS
    // ======================================================

    private fun drawMapWithUserLocation() {
        // Bersihkan overlay lama
        if (mapView.overlays.size > 1) {
            for (i in mapView.overlays.size - 1 downTo 1) {
                mapView.overlays.removeAt(i)
            }
        }

        // Kompas
        val compass = CompassOverlay(
            requireContext(),
            InternalCompassOrientationProvider(requireContext()),
            mapView
        )
        compass.enableCompass()
        mapView.overlays.add(compass)

        val lokasiUser = GeoPoint(lat, lng)

        // Marker lokasi user
        val markerUser = ArrayList<OverlayItem>()
        markerUser.add(OverlayItem("Lokasi Kamu", "Kamu berada di sini", lokasiUser))
        mapView.overlays.add(
            ItemizedIconOverlay(
                markerUser,
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

        // Titik biru GPS bawaan osmdroid
        val gps = GpsMyLocationProvider(requireContext())
        val me  = MyLocationNewOverlay(gps, mapView)
        me.enableMyLocation()
        mapView.overlays.add(me)

        // Zoom supaya kedua titik kelihatan semua
        val boundingBox = BoundingBox.fromGeoPoints(arrayListOf(lokasiUser, lokasiPS))
        mapView.post {
            mapView.zoomToBoundingBox(boundingBox, true, 150)
        }

        // Hitung jarak lurus sementara
        val hasil = FloatArray(1)
        Location.distanceBetween(lat, lng, lokasiPS.latitude, lokasiPS.longitude, hasil)
        val jarakMeter = hasil[0]
        tvJarak.text = if (jarakMeter >= 1000) {
            "📍 $namaPS · ${"%.1f km".format(jarakMeter / 1000)} dari lokasi kamu"
        } else {
            "📍 $namaPS · ${jarakMeter.toInt()} meter dari lokasi kamu"
        }

        // Ambil rute jalan dari OSRM
        drawRoute(lokasiUser, lokasiPS)

        mapView.invalidate()
    }

    private fun drawRoute(start: GeoPoint, end: GeoPoint) {
        val url = "https://router.project-osrm.org/route/v1/driving/" +
                "${start.longitude},${start.latitude};" +
                "${end.longitude},${end.latitude}" +
                "?overview=full&geometries=geojson"

        tvJarak.text = "🔄 Menghitung rute..."

        val request = object : StringRequest(
            Method.GET, url,
            { response ->
                try {
                    val json   = JSONObject(response)
                    val routes = json.getJSONArray("routes")

                    // ✅ Ganti return@StringRequest dengan if-else
                    if (routes.length() > 0) {
                        val route       = routes.getJSONObject(0)
                        val geometry    = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val routePoints = ArrayList<GeoPoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            routePoints.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                        }

                        val jarakM      = route.getDouble("distance")
                        val durasiS     = route.getDouble("duration")
                        val durasiMenit = (durasiS / 60).toInt()
                        val jarakStr    = if (jarakM >= 1000) {
                            "%.1f km".format(jarakM / 1000)
                        } else {
                            "${jarakM.toInt()} m"
                        }
                        tvJarak.text = "📍 $namaPS · $jarakStr · ±$durasiMenit menit"

                        val polyline = Polyline()
                        polyline.setPoints(routePoints)
                        polyline.color = android.graphics.Color.parseColor("#185FA5")
                        polyline.width = 8f
                        mapView.overlays.add(polyline)
                        mapView.invalidate()
                    } else {
                        tvJarak.text = "Rute tidak ditemukan"
                    }

                } catch (e: Exception) {
                    tvJarak.text = "Gagal memuat rute"
                    e.printStackTrace()
                }
            },
            { tvJarak.text = "Gagal memuat rute" }
        ) {}

        Volley.newRequestQueue(requireContext()).add(request)
    }
    // ======================================================
    // PERMISSION & ACTIVITY RESULT
    // ======================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (::airLocation.isInitialized) {
            airLocation.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (::airLocation.isInitialized) {
            airLocation.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = DashboardFragment()
    }
}