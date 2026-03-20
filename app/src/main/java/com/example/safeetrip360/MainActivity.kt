package com.example.safeetrip360
import android.os.Bundle

import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import android.content.Intent


import android.widget.Button


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.google.firebase.FirebaseApp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import androidx.compose.material.icons.filled.*

import com.google.android.gms.location.LocationServices
import android.telephony.SmsManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import android.os.Looper

import android.widget.Toast
import com.google.android.gms.location.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.net.toUri

class MainActivity :  ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
         val db = FirebaseFirestore.getInstance()
        // Define the update frequency
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000
        ).setMinUpdateIntervalMillis(5000).build()
        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val user = FirebaseAuth.getInstance().currentUser
                for (location in result.locations) {
                    val data = hashMapOf(
                        "lat" to location.latitude,
                        "lng" to location.longitude,
                        "time" to FieldValue.serverTimestamp()
                    )
                    // Save to Firestore
                    user?.uid?.let { uid ->
                        db.collection("users").document(uid).collection("logs").add(data)
                    }
                    android.util.Log.d("LiveTracking", "Saved: ${location.latitude}")
                }
            }
        }
        val auth = FirebaseAuth.getInstance()
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        if (permissions.any {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }

        ActivityCompat.requestPermissions(this, permissions, 1)



//
        setContent {
            MaterialTheme {
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null && auth.currentUser?.isEmailVerified == true) }

                if (isLoggedIn) {
                    MainContentScreen(
                        onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                        },
                        onStopTracking = {
                            stopSOS()
                        }
                    )
                } else {
                    AuthScreen(auth) {
                        isLoggedIn = true
                    }
                }
            }
        }
    }
    fun openNavigationToSafety(context: android.content.Context, destLat: Double, destLng: Double) {
        // Opens Google Maps in "Turn-by-Turn Navigation" mode
        val gmmIntentUri = "google.navigation:q=$destLat,$destLng".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            // Fallback if Maps isn't installed
            Toast.makeText(context, "Google Maps not found", Toast.LENGTH_SHORT).show()
        }
    }


    fun sendLiveAlerts(location: android.location.Location) {
        val mapsUrl = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
        val message = "🚨 EMERGENCY ALERT\nI need help immediately.\nMy live location: $mapsUrl"

        // A. Send SMS
        try {
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage("7904004217", null, message, null, null)
        } catch (e: Exception) {
            android.util.Log.e("SOS", "SMS Failed: ${e.message}")
        }

        // B. Update Firestore (which can trigger a Firebase Cloud Function Email)
        val log = hashMapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "time" to FieldValue.serverTimestamp()
        )
        FirebaseFirestore.getInstance().collection("emergency_logs").add(log)
    }

    fun startSOS() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Toast.makeText(this, "Live SOS Started", Toast.LENGTH_SHORT).show()
        }
    }
    // Find the button
    // 1. Find the button


    fun stopSOS() {
        fusedLocationClient.removeLocationUpdates(locationCallback) //
        Toast.makeText(this, "SOS Stopped. You are safe.", Toast.LENGTH_SHORT).show()
    }
    fun triggerEmergency(context: android.content.Context) {
        val client = LocationServices.getFusedLocationProviderClient(context)

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            client.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // Corrected String Interpolation
                    val mapsUrl = "https://www.google.com/maps?q=${it.latitude},${it.longitude}"
                    val message = "🚨 EMERGENCY! I'm in danger. My location: $mapsUrl"

                    // Start continuous tracking
                    startSOS()

                    // Send SMS
                    try {
                        val smsManager = context.getSystemService(SmsManager::class.java)
                        smsManager.sendTextMessage("7904004217", null, message, null, null)
                        Toast.makeText(context, "SOS Sent!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("SOS_ERROR", "SMS Failed", e)
                    }

                    // Open Dialer
                    val intent = Intent(
                        android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:112")
                    )
                    context.startActivity(intent)
                }
            }
        }
    }
    // 1. Create a SAFE ZONE (Alert on EXIT)
    fun addSafeZone() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geofence = geofenceHelper.getGeofence(
                    "SAFE_HOME",
                    location.latitude, location.longitude,
                    200f, // 200m Radius
                    Geofence.GEOFENCE_TRANSITION_EXIT // <--- Alert on EXIT
                )
                val request = geofenceHelper.getGeofencingRequest(geofence)
                geofencingClient.addGeofences(request, geofenceHelper.getPendingIntent())
                    .addOnSuccessListener { Toast.makeText(this, "Safe Zone Active (Exit Alert)", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // 2. Create a DANGER ZONE (Alert on ENTER)
// For demo, we put this 500m away from current location
    fun addDemoDangerZone() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Create a fake danger zone 0.005 degrees away (approx 500m)
                val dangerLat = location.latitude + 0.005
                val dangerLng = location.longitude + 0.005

                val geofence = geofenceHelper.getGeofence(
                    "DANGER_TEST",
                    dangerLat, dangerLng,
                    200f,
                    Geofence.GEOFENCE_TRANSITION_ENTER // <--- Alert on ENTER
                )
                val request = geofenceHelper.getGeofencingRequest(geofence)
                geofencingClient.addGeofences(request, geofenceHelper.getPendingIntent())
                    .addOnSuccessListener { Toast.makeText(this, "Danger Zone Created nearby (Walk there to test)", Toast.LENGTH_SHORT).show() }
            }
        }
    }


}


@Composable
fun AuthScreen(
    auth: FirebaseAuth,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SafeeTrip360 Login",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val user = result.user
                                user?.reload()?.addOnCompleteListener { task ->
                                    isLoading = false
                                    if (user?.isEmailVerified == true) {
                                        isVerified = true
                                        onLoginSuccess()
                                    } else {
                                        auth.signOut()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Please verify your email first!")
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(it.message ?: "Login failed")
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotEmpty() && password.isNotEmpty()
                ) {
                    Text("Login")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) return@Button
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                result.user?.sendEmailVerification()
                                auth.signOut()
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Verification email sent! Check your inbox.")
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        it.message ?: "Signup failed"
                                    )
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Up")
                }

                TextButton(
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    auth.currentUser?.sendEmailVerification()
                                    auth.signOut()
                                    scope.launch { snackbarHostState.showSnackbar("Link Resent!") }
                                }
                        }
                    }
                ) {
                    Text("Resend Verification Email")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isVerified) "✅ Email Verified" else " Email  Verified",
                color = if (isVerified) Color(0xFF4CAF50) else Color.Red
            )
        }
    }
}


@Composable
fun MainContentScreen(onLogout: () -> Unit, onStopTracking: () -> Unit) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0B1120),
                contentColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Navigation Items
                val items = listOf(
                    Triple(Screen.Home, Icons.Default.Home, "Home"),
                    Triple(Screen.Map, Icons.Default.Place, "Live Map"),
                    Triple(Screen.Profile, Icons.Default.Person, "Profile"),
                    Triple(Screen.Settings, Icons.Default.Settings, "Settings"),
                    Triple(Screen.Help, Icons.Default.Info, "Help")
                )

                items.forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFFD32F2F),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .padding(innerPadding)
                .background(Color(0xFF0B1120))
        ) {
            // 1. HOME
            composable(Screen.Home.route) {
                WelcomeHomeScreen(onNavigateToPanic = {
                    navController.navigate(Screen.Panic.route)
                })
            }

            // 2. PANIC (Dashboard)
            composable(Screen.Panic.route) {
                DashboardScreen(onEmergencyTrigger = { context ->
                    (context as? MainActivity)?.triggerEmergency(context)
                })
            }

            // 3. MAP
            composable(Screen.Map.route) {
                LiveRouteScreen(currentLat = 13.0827, currentLng = 80.2707)
            }

            // 4. PROFILE (Fixed!)
            composable(Screen.Profile.route) {
                ProfileScreen(onLogout = onLogout) // Now this will work
            }

            // 5. HELP (Fixed!)
            composable(Screen.Help.route) {
                HelpScreen() // Now this exists
            }

            // 6. SETTINGS
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToHistory = { navController.navigate(Screen.Notifications.route) },
                    onLogout = onLogout
                )
            }

            // 7. NOTIFICATIONS
            composable(Screen.Notifications.route) {
                NotificationHistoryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun DashboardScreen(onEmergencyTrigger: (android.content.Context) -> Unit) {
    val context = LocalContext.current

    // Helper to get Activity
    fun getMainActivity(): MainActivity? = context as? MainActivity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8)) // Soft Blue-Grey
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enables scrolling for smaller screens
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- TITLE ---
        Text(
            text = "Your Safety, Our Priority",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF1F2937)
        )
        Text(
            text = "Stay protected while exploring.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4B5563)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- SOS BUTTON (The Master Trigger) ---
        Surface(
            modifier = Modifier
                .size(180.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {

                        // 1. START AUDIO RECORDING (Silent Witness)
                        try {
                            AudioRecorder.startRecording(context)
                            android.widget.Toast.makeText(context, "🎙️ Evidence Recording Started...", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "❌ Mic Error: Check Permissions", android.widget.Toast.LENGTH_SHORT).show()
                        }

                        // 2. TRIGGER ALARM / UI EFFECTS
                        onEmergencyTrigger(context)

                        // 3. FETCH LOCATION & SEND SMS
                        try {
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

                            // Check Location Permission
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    val smsManager = context.getSystemService(SmsManager::class.java)
                                    // ----------------------------------------------------
                                    // 👇 REPLACE THIS WITH YOUR REAL PHONE NUMBER
                                    // ----------------------------------------------------
                                    val targetNumber = "+916385682576"

                                    val messageBody = if (location != null) {
                                        // Create Clickable Google Maps Link
                                        " SOS! I need help! My Location: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                                    } else {
                                        " SOS! I need help! (GPS Location fetching...)"
                                    }

                                    // Send SMS
                                    smsManager.sendTextMessage(targetNumber, null, messageBody, null, null)
                                    android.widget.Toast.makeText(context, "✅ Location Sent via SMS!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "❌ Location Permission Missing", android.widget.Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "❌ SMS Failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    })
                },
            shape = CircleShape,
            color = Color(0xFF121212), // Black
            shadowElevation = 8.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("SOS", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Click to activate SOS", color = Color.Gray)

        Spacer(modifier = Modifier.height(30.dp))

        // --- GEOFENCE CONTROLS ---

        // Safe Zone
        Button(
            onClick = { getMainActivity()?.addSafeZone() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Safe Zone Here")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Danger Zone
        Button(
            onClick = { getMainActivity()?.addDemoDangerZone() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Danger Zone")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- STOP RECORDING BUTTON (Manual Stop) ---
        Button(
            onClick = {
                AudioRecorder.stopRecording()
                android.widget.Toast.makeText(context, "⏹️ Recording Stopped & Saved", android.widget.Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Text("Stop Recording / False Alarm")
        }
        // --- STOP RECORDING BUTTON (Manual Stop) ---


        Spacer(modifier = Modifier.height(16.dp))

        // --- NEW: DEAD MAN'S SWITCH BUTTON ---
        Button(
            onClick = {
                // Open the Safety Timer Screen
                val intent = Intent(context, SafetyTimerActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Red color
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Dead Man's Switch")
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Panic : Screen("panic") // New route for the SOS button
    object Profile : Screen("profile")
    object Help : Screen("help")
    object Settings : Screen("settings")
    object Map : Screen("map")
    object Notifications : Screen("notifications")
}

@Composable
fun WelcomeHomeScreen(onNavigateToPanic: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color(0xFF4CAF50))
        Spacer(modifier = Modifier.height(24.dp))
        Text("SafeeTrip360", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("Your Personal Travel Guardian", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNavigateToPanic,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("ENTER SAFETY MODE", style = MaterialTheme.typography.titleMedium)
        }
    }
}


@Composable
fun SimpleScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}
@Composable
fun LiveRouteMap() {
    // 1. Set initial camera position (e.g., Chennai)
    val chennai = LatLng(13.0827, 80.2707)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(chennai, 15f)
    }

    // 2. Define Route Points (Start -> End)
    val routePoints = listOf(
        LatLng(13.0827, 80.2707), // Start
        LatLng(13.0850, 80.2720), // Waypoint
        LatLng(13.0900, 80.2750)  // End
    )

    // 3. Render the Map
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {

        Marker(
            state = MarkerState(position = chennai),
            title = "Start Location",
            snippet = "SafeeTrip360 Tracking"
        )
        Polyline(
            points = routePoints,
            color = Color.Blue,
            width = 12f
        )
    }
}
@Composable
fun LiveRouteScreen(currentLat: Double, currentLng: Double) {
    // 1. Setup the camera centering on the user
    val userLocation = LatLng(currentLat, currentLng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    // 2. Render the Google Map
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true)
    ) {
        // 3. Add a marker for the user
        Marker(
            state = MarkerState(position = userLocation),
            title = "My Location",
            snippet = "SafeeTrip360 Tracking Active"
        )
    }
}
@Composable
fun NotificationHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var alerts by remember { mutableStateOf(AlertHistoryManager.getAlerts(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Alert History", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                AlertHistoryManager.clearHistory(context)
                alerts = emptyList()
            }) { Text("Clear") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // The List
        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alerts recorded yet.", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(alerts) { alert ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Red tint
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(alert.second, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(alert.third)
                            Text(alert.first, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
//@Composable
//fun SettingsScreen(onNavigateToHistory: () -> Unit) {
//    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
//        Text("Settings", style = MaterialTheme.typography.headlineLarge)
//        Spacer(modifier = Modifier.height(24.dp))
//
//        // --- NEW BUTTON ---
//        Button(
//            onClick = onNavigateToHistory,
//            modifier = Modifier.fillMaxWidth().height(56.dp),
//            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
//        ) {
//            Icon(Icons.Default.Notifications, contentDescription = null)
//            Spacer(modifier = Modifier.width(12.dp))
//            Text("View Alert History")
//        }
//        // ------------------
//    }
//}
@Composable
fun SettingsScreen(onNavigateToHistory: () -> Unit, onLogout: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateToHistory, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
            Icon(Icons.Default.Notifications, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("View Alert History")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("Logout")
        }
    }
}
@Composable
fun HelpScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF2196F3))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Help & Support", style = MaterialTheme.typography.headlineMedium,color = Color.White)
        Text("Emergency Contact: +91 8754537814", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("1. Press SOS for immediate help.", color = Color.Gray)
        Text("2. Use Map to see your location.", color = Color.Gray)
    }
}
@Composable
fun ProfileScreen(onLogout: () -> Unit) { // <--- Added onLogout parameter
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(100.dp))
        Text("User Profile", style = MaterialTheme.typography.headlineMedium,color = Color.White)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogout, // <--- This calls the logout function
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Log Out")
        }
    }
}
// Find the button

