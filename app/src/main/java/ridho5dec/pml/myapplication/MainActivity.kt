package ridho5dec.pml.myapplication

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import ridho5dec.pml.myapplication.ui.theme.Pml15RidhoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Penting: Konfigurasi OSMDroid (Sesuai modul)
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContent {
            Pml15RidhoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Warna kustom sesuai kode XML di modul
val ColorBackgroundPosisiku = Color(0xFFFFF9C4) // Kuning muda
val ColorBackgroundTujuan = Color(0xFFB2EBF2)   // Cyan muda
val ColorBackgroundInfo = Color(0xFFDCEDC8)     // Hijau muda
val ColorFabBlue = Color(0xFFBBDEFB)            // Biru muda untuk FAB
val ColorFabPink = Color(0xFFF8BBD0)            // Pink muda untuk FAB Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    // State dummy untuk input text
    var textTujuan by remember { mutableStateOf("") }
    var isRefreshChecked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp) // Margin global sedikit agar rapi
    ) {

        // --- BAGIAN ATAS: Chip & Info Posisi ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = isRefreshChecked,
                onClick = { isRefreshChecked = !isRefreshChecked },
                label = { Text("Refresh") },
                leadingIcon = {
                    if (isRefreshChecked) {
                        Icon(
                            imageVector = Icons.Filled.Share, // Placeholder icon check
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier.padding(end = 8.dp)
            )

            // TextView: txPosisiku
            Text(
                text = "Lat: -7.80xxx\nLng: 112.00xxx", // Dummy text
                modifier = Modifier
                    .weight(1f)
                    .background(ColorBackgroundPosisiku)
                    .padding(4.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BAGIAN INPUT: EditText & Button ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // EditText: edTujuan
            OutlinedTextField(
                value = textTujuan,
                onValueChange = { textTujuan = it },
                label = { Text("Lokasi Tujuan") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                singleLine = true
            )

            // Button: btnCari
            Button(
                onClick = { /* Logika Cari */ },
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Cari")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BAGIAN INFO: TextViews ---

        // TextView: txTujuan
        Text(
            text = "Info Lokasi Tujuan...",
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBackgroundTujuan)
                .padding(4.dp),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // TextView: txInfo
        Text(
            text = "Info Jarak & Durasi...",
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBackgroundInfo)
                .padding(4.dp),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- BAGIAN PETA & FABs ---
        Box(
            modifier = Modifier
                .weight(1f) // Mengisi sisa ruang ke bawah
                .fillMaxWidth()
                .background(Color.LightGray) // Placeholder bg
        ) {
            // 1. Peta (MapView)
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        // Atur posisi default (misal Kediri sesuai modul)
                        // controller.setCenter(GeoPoint(-7.801166, 112.008228))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Floating Action Buttons (Overlay)
            // Disusun vertikal di pojok kanan atas (TopEnd)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // FAB 1: Polyline
                SmallFloatingActionButton(
                    onClick = { /* Aksi Polyline */ },
                    containerColor = ColorFabBlue
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Polyline")
                }

                // FAB 2: Polygon
                SmallFloatingActionButton(
                    onClick = { /* Aksi Polygon */ },
                    containerColor = ColorFabBlue
                ) {
                    // Menggunakan icon hexagon sebagai pengganti 'outline_activity_zone'
                    Icon(Icons.Outlined.Hexagon, contentDescription = "Polygon")
                }

                Spacer(modifier = Modifier.height(16.dp)) // Jarak agak jauh ke delete

                // FAB 3: Delete
                SmallFloatingActionButton(
                    onClick = { /* Aksi Delete */ },
                    containerColor = ColorFabPink
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    Pml15RidhoTheme {
        MapScreen()
    }
}