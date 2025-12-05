package ridho5dec.pml.myapplication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home // <--- IMPORT BARU
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mumayank.com.airlocationlibrary.AirLocation
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import ridho5dec.pml.myapplication.ui.theme.Pml15RidhoTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    // Variable untuk AirLocation
    private var airLocation: AirLocation? = null
    // State untuk menyimpan lokasi terkini agar UI Compose bisa update
    private var locationState = mutableStateOf<Location?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfigurasi OSMDroid (Menggunakan getSharedPreferences biasa agar tidak deprecated)
        Configuration.getInstance().load(
            applicationContext,
            applicationContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )

        // Inisialisasi AirLocation (GPS)
        airLocation = AirLocation(this, object : AirLocation.Callback {
            override fun onSuccess(locations: ArrayList<Location>) {
                // Update state lokasi saat berhasil didapatkan
                locationState.value = locations.firstOrNull()
            }
            override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
                Toast.makeText(this@MainActivity, "Gagal mendapatkan lokasi", Toast.LENGTH_SHORT).show()
            }
        }, isLocationRequiredOnlyOneTime = true)

        // Mulai pencarian lokasi (pastikan dipanggil pada objek airLocation)
        airLocation?.start()

        setContent {
            Pml15RidhoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Kirim state lokasi dan fungsi refresh ke UI
                    MapScreen(
                        modifier = Modifier.padding(innerPadding),
                        currentLocation = locationState.value,
                        onRefreshLocation = { airLocation?.start() }
                    )
                }
            }
        }
    }

    // Override onActivityResult (Wajib untuk AirLocation)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airLocation?.onActivityResult(requestCode, resultCode, data)
    }

    // Override onRequestPermissionsResult (Wajib untuk AirLocation)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airLocation?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

// Warna kustom
val ColorBackgroundPosisiku = ComposeColor(0xFFFFF9C4)
val ColorBackgroundTujuan = ComposeColor(0xFFB2EBF2)
val ColorBackgroundInfo = ComposeColor(0xFFDCEDC8)
val ColorFabBlue = ComposeColor(0xFFBBDEFB)
val ColorFabPink = ComposeColor(0xFFF8BBD0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    currentLocation: Location?,
    onRefreshLocation: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // Untuk coroutine background (Routing)

    // State UI
    var textTujuan by remember { mutableStateOf("") }
    var infoTujuan by remember { mutableStateOf("Info Lokasi Tujuan...") }
    var infoJarak by remember { mutableStateOf("Info Jarak & Durasi...") }

    // MapView instance
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    // Efek ketika lokasi berubah
    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            val geoPoint = GeoPoint(loc.latitude, loc.longitude)
            mapView.controller.animateTo(geoPoint)

            // Tambahkan overlay MyLocation
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
            locationOverlay.enableMyLocation()
            mapView.overlays.add(locationOverlay)

            // Tambahkan Kompas
            val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), mapView)
            compassOverlay.enableCompass()
            mapView.overlays.add(compassOverlay)

            mapView.invalidate()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        // --- BAGIAN ATAS: Info Posisi ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onRefreshLocation, modifier = Modifier.padding(end = 8.dp)) {
                Text("Refresh")
            }
            Text(
                text = if (currentLocation != null)
                    "Lat: ${currentLocation.latitude}\nLng: ${currentLocation.longitude}"
                else "Mencari GPS...",
                modifier = Modifier.weight(1f).background(ColorBackgroundPosisiku).padding(4.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // --- BAGIAN INPUT: Pencarian ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = textTujuan, onValueChange = { textTujuan = it },
                label = { Text("Lokasi Tujuan") },
                modifier = Modifier.weight(1f).padding(end = 8.dp), singleLine = true
            )
            Button(onClick = {
                // LOGIKA PENCARIAN & ROUTING
                if (currentLocation != null && textTujuan.isNotEmpty()) {
                    val startPoint = GeoPoint(currentLocation.latitude, currentLocation.longitude)

                    scope.launch(Dispatchers.IO) { // Jalankan di background
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            // Menggunakan cara lama (deprecated tapi stabil untuk API level rendah-menengah)
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocationName(textTujuan, 1)

                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val endPoint = GeoPoint(address.latitude, address.longitude)

                                withContext(Dispatchers.Main) {
                                    infoTujuan = "Tujuan: ${address.featureName ?: textTujuan}\nLat: ${address.latitude}, Lng: ${address.longitude}"
                                }

                                val roadManager = OSRMRoadManager(context, "userAgent/1.0")
                                val waypoints = arrayListOf(startPoint, endPoint)
                                val road = roadManager.getRoad(waypoints)

                                withContext(Dispatchers.Main) {
                                    if (road.mStatus == Road.STATUS_OK) {
                                        val roadOverlay = RoadManager.buildRoadOverlay(road)
                                        roadOverlay.outlinePaint.color = Color.rgb(228, 0, 92) // FIX: color deprecated
                                        roadOverlay.outlinePaint.strokeWidth = 10f // FIX: width deprecated
                                        mapView.overlays.add(roadOverlay)

                                        val marker = Marker(mapView)
                                        marker.position = endPoint
                                        marker.title = textTujuan
                                        mapView.overlays.add(marker)

                                        val km = String.format(Locale.US, "%.2f", road.mLength)
                                        val durationSec = road.mDuration
                                        val min = (durationSec / 60).toInt()
                                        infoJarak = "Jarak: $km km, Durasi: $min menit"

                                        mapView.invalidate()
                                        mapView.zoomToBoundingBox(roadOverlay.bounds, true, 50)
                                    } else {
                                        Toast.makeText(context, "Gagal memuat rute", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Lokasi tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    Toast.makeText(context, "Lokasi GPS belum siap atau tujuan kosong", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Cari") }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = infoTujuan, modifier = Modifier.fillMaxWidth().background(ColorBackgroundTujuan).padding(4.dp), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = infoJarak, modifier = Modifier.fillMaxWidth().background(ColorBackgroundInfo).padding(4.dp), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // --- PETA & FAB ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize().clipToBounds()
            )

            Column(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // FAB 1: Polyline Manual (Sesuai Modul)
                SmallFloatingActionButton(onClick = {
                    currentLocation?.let { loc ->
                        val lat = loc.latitude
                        val lng = loc.longitude

                        // Menyesuaikan koordinat sesuai dengan implementasi drawPolyline() pada modul
                        val points = listOf(
                            GeoPoint(lat, lng), // y,x
                            GeoPoint(lat, lng + 0.002),
                            GeoPoint(lat + 0.002, lng + 0.002),
                            GeoPoint(lat + 0.002, lng - 0.002),
                            GeoPoint(lat - 0.002, lng - 0.002),
                            GeoPoint(lat - 0.002, lng + 0.002),
                            GeoPoint(lat - 0.00050, lng + 0.00200)
                        )

                        val polyline = Polyline()
                        polyline.setPoints(points)
                        polyline.outlinePaint.color = Color.BLUE
                        // Lebar garis 10f sesuai modul
                        polyline.outlinePaint.strokeWidth = 10f
                        mapView.overlays.add(polyline)
                        mapView.invalidate()

                        // Zoom dengan padding 200 sesuai modul
                        polyline.bounds?.let { bbox ->
                            mapView.zoomToBoundingBox(bbox, true, 200)
                        }
                    }
                }, containerColor = ColorFabBlue) { Icon(Icons.Filled.Share, "Polyline") }

                // FAB 2: Polygon
                SmallFloatingActionButton(onClick = {
                    // MENGUBAH: Menggunakan koordinat tetap sesuai permintaan
                    val lat = -7.80088236980126
                    val lng = 112.00859247396205

                    // Membuat bentuk kotak di sekitar koordinat tersebut
                    val points = listOf(
                        GeoPoint(lat + 0.002, lng + 0.002),
                        GeoPoint(lat + 0.002, lng - 0.002),
                        GeoPoint(lat - 0.002, lng - 0.002),
                        GeoPoint(lat - 0.002, lng + 0.002)
                    )

                    val polygon = Polygon()
                    polygon.points = points
                    polygon.fillColor = Color.argb(128, 0, 255, 0)
                    polygon.outlinePaint.color = Color.GREEN
                    polygon.outlinePaint.strokeWidth = 2f
                    mapView.overlays.add(polygon)
                    mapView.invalidate()

                    // Zoom otomatis ke area polygon
                    mapView.zoomToBoundingBox(polygon.bounds, true, 100)
                }, containerColor = ColorFabBlue) { Icon(Icons.Outlined.Hexagon, "Polygon") }

                // FAB 3: Rute ke Rumah (BARU DITAMBAHKAN)
                SmallFloatingActionButton(onClick = {
                    if (currentLocation != null) {
                        val startPoint = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                        val endPoint = GeoPoint(-7.907112724888536, 112.05332056189468) // Koordinat Rumah

                        scope.launch(Dispatchers.IO) {
                            try {
                                val roadManager = OSRMRoadManager(context, "userAgent/1.0")
                                val waypoints = arrayListOf(startPoint, endPoint)
                                val road = roadManager.getRoad(waypoints)

                                withContext(Dispatchers.Main) {
                                    if (road.mStatus == Road.STATUS_OK) {
                                        val roadOverlay = RoadManager.buildRoadOverlay(road)
                                        roadOverlay.outlinePaint.color = Color.MAGENTA
                                        roadOverlay.outlinePaint.strokeWidth = 10f
                                        mapView.overlays.add(roadOverlay)

                                        val marker = Marker(mapView)
                                        marker.position = endPoint
                                        marker.title = "Rumah"
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        mapView.overlays.add(marker)

                                        // Update info text
                                        infoTujuan = "Tujuan: Rumah\nLat: ${endPoint.latitude}, Lng: ${endPoint.longitude}"
                                        val km = String.format(Locale.US, "%.2f", road.mLength)
                                        val durationSec = road.mDuration
                                        val min = (durationSec / 60).toInt()
                                        infoJarak = "Jarak: $km km, Durasi: $min menit"

                                        mapView.invalidate()
                                        mapView.zoomToBoundingBox(roadOverlay.bounds, true, 100)
                                    } else {
                                        Toast.makeText(context, "Gagal memuat rute ke Rumah", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Lokasi GPS belum siap", Toast.LENGTH_SHORT).show()
                    }
                }, containerColor = ColorFabBlue) { Icon(Icons.Filled.Home, "Rumah") }

                Spacer(modifier = Modifier.height(16.dp))

                // FAB 4: Delete
                SmallFloatingActionButton(onClick = {
                    if (mapView.overlays.size > 2) {
                        mapView.overlays.clear()
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
                        locationOverlay.enableMyLocation()
                        mapView.overlays.add(locationOverlay)
                        val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), mapView)
                        compassOverlay.enableCompass()
                        mapView.overlays.add(compassOverlay)
                        mapView.invalidate()
                        infoTujuan = "Info Lokasi Tujuan..."
                        infoJarak = "Info Jarak & Durasi..."
                    }
                }, containerColor = ColorFabPink) { Icon(Icons.Filled.Delete, "Delete") }
            }
        }
    }
}