package com.example.operatorapp

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonObject
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Map : AppCompatActivity(), OnMapReadyCallback,
    Callback<DirectionsResponse?>, PermissionsListener {
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationComponent: LocationComponent
    private lateinit var db: FirebaseFirestore
    private lateinit var fabUserLocation: View
    private lateinit var fabLocationSearch: View
    private lateinit var btnDisplayRoute: Button
    private lateinit var tvDistance: TextView
    private lateinit var tvS: TextView
    private lateinit var tvD: TextView
    private lateinit var home: CarmenFeature
    private lateinit var work: CarmenFeature
    private lateinit var stopId: MutableList<String>
    private lateinit var stops: MutableList<Point>
    private lateinit var stop: MutableList<Point>
    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val symbolIconId = "symbolIconId"
    var address: String? = null
    private var origin: Point = Point.fromLngLat(90.399452, 23.777176)
    private var destination: Point = Point.fromLngLat(90.399452, 23.777176)
    private var client: MapboxDirections? = null
    var c = 0
    private var distance = 0.0
    private var st: String? = null
    var startLocation: String? = ""
    var endLocation: String? = ""
    private lateinit var firestore: FirebaseFirestore
    private var previousLocation: LatLng? = null
    private var currentLocation: LatLng? = null
    private lateinit var busLoc: LatLng
    private var bus: Marker? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            resources.getString(R.string.accessToken)
        )
        setContentView(R.layout.activity_map)
        init()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun init(){
        mapView = findViewById<View>(R.id.mapView) as MapView
        fabUserLocation = findViewById(R.id.fabUserLocation_routeMap)
        fabLocationSearch = findViewById(R.id.fabLocationSearch_routeMap)
        tvDistance = findViewById(R.id.distanceView_routeMap)
        tvS = findViewById(R.id.tvS_routeMap)
        tvD = findViewById(R.id.tvD_routeMap)
        btnDisplayRoute = findViewById(R.id.btnDisplayRoute_routeMap)
        db = FirebaseFirestore.getInstance()
        stopId = mutableListOf()
        stops = mutableListOf()
        firestore = FirebaseFirestore.getInstance()
    }

    private fun moveToUserLoc() {
        fabUserLocation.setOnClickListener {
            val lastLocation = mapboxMap.locationComponent.lastKnownLocation
            if (lastLocation != null) {
                val position: CameraPosition = CameraPosition.Builder()
                    .target(LatLng(lastLocation.latitude, lastLocation.longitude))
                    .zoom(14.0)
                    .tilt(13.0)
                    .build()
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000)
            }
        }
    }

    private fun fetchRoute(){
        btnDisplayRoute.setOnClickListener {
            getStopId{
                getStop{
                    drawRoute(stops[0], stops[1])
                }
            }
        }
    }

    private fun getBusLoc(){
        firestore.collection("userLocations").document("89")
            .addSnapshotListener{ snapshot, exception ->
                if (exception != null) {
                    Log.e("Firestore", "Listen failed", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lat = snapshot.getDouble("latitude")
                    val long = snapshot.getDouble("longitude")
                    val status = snapshot.getString("status")

                    if(lat != null && long != null){
                        busLoc = LatLng(lat, long)
                        updateMarkerPosition(busLoc, status)
                        Log.i("my_tag", "lat: $lat long: $long")
                    }else {
                        Log.e("Firestore", "One or both fields are missing")
                    }
                } else {
                    Log.d("Firestore", "Current data: null")
                }
            }
    }

    private fun updateMarkerPosition(location: LatLng, status: String?) {
        if(bus == null){
            val busPosDrawable = ResourcesCompat.getDrawable(resources, R.drawable.bus_position_symbol, null)
            val busPosIcon = IconFactory.getInstance(this).fromBitmap(BitmapUtils.getBitmapFromDrawable(busPosDrawable)!!)
            bus = mapboxMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .icon(busPosIcon)
                    .title("BUS")
            )

        } else {
            if(status == "inactive"){
                hideBusMarker()
                return
            }
            updateMarkerPositionAnimation(location)
        }
    }

    private fun hideBusMarker() {
        mapboxMap.removeMarker(bus!!)

        bus = null
    }

    private fun updateMarkerPositionAnimation(newLocation: LatLng) {
        Log.i("my_tag", "marker update fun called")

        previousLocation = currentLocation
        currentLocation = newLocation
        Log.i("my_tag", "previous location: $previousLocation")
        Log.i("my_tag", "current location: $currentLocation")

        previousLocation?.let { prevLocation ->
            val interpolator = LinearInterpolator()
            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.duration = 1000 // Animation duration in milliseconds
            valueAnimator.addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val lat = (currentLocation!!.latitude - prevLocation.latitude) * fraction + prevLocation.latitude
                val lng = (currentLocation!!.longitude - prevLocation.longitude) * fraction + prevLocation.longitude
                val animatedPosition = LatLng(lat, lng)

                bus!!.position = animatedPosition
                mapboxMap.updateMarker(bus!!)

            }
            valueAnimator.interpolator = interpolator
            valueAnimator.start()
        }
    }

    @SuppressLint("LogNotTimber")
    private fun getStop(callback: () -> Unit){

        val stop = mutableListOf<Point>()

        for (documentId in stopId) {
            db.collection("Stop").document(documentId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val snapshot = task.result

                        // Check if the document exists
                        if (snapshot.exists()) {

                            // Extract data for each stop
                            val lat = snapshot.getString("lat")!!.toDouble()
                            val long = snapshot.getString("long")!!.toDouble()

                            val stopData = Point.fromLngLat(lat, long)

                            stop.add(stopData)

                            // Now stopsData list contains the latitude and longitude for each stop

                        } else {
                            Log.e("stop", "Document $documentId does not exist.")
                        }
                    } else {
                        // Handle errors
                        Log.e("stop", "Error getting document $documentId:", task.exception)

                        Toast.makeText(this, "Error getting stops: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }

                    if (stopId.indexOf(documentId) == stopId.size - 1) {
                        stops = stop
                        callback.invoke()
                    }
                }
                .addOnFailureListener {
                    Log.e("stop", "Firestore query failed:", it)

                    Toast.makeText(this@Map, "Oops....something went wrong", Toast.LENGTH_SHORT).show()
                }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun getStopId(callback: () -> Unit) {
        db.collection("Route").get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (documentSnapshot in querySnapshot.documents) {
                        val routeData = documentSnapshot.data
                        val id = mutableListOf<String>()

                        // Extract stops from the routeData map
                        for (i in 1..routeData!!.size) {
                            val stopKey = "stop$i"
                            val stop = routeData[stopKey] as String
                            id.add(stop)
                        }

                        stopId = id
                    }
                } else {
                    // No documents found
                }

                Log.d("stop", "Route: $stopId")
                callback.invoke() // Callback to indicate that the Firestore query is complete
            }
    }

    private fun drawRoute(origin: Point, destination: Point) {
        client = MapboxDirections.builder()
            .origin(origin)
            .destination(destination)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(resources.getString(R.string.accessToken))
            .build()
        client?.enqueueCall(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.MAPBOX_STREETS
        ) { style ->
            enableLocationComponent(style)
            initSearchFab()
            moveToUserLoc()
            addUserLocations()
            getBusLoc()
//            fetchRoute()

            val drawable = ResourcesCompat.getDrawable(
                resources, R.drawable.ic_baseline_location_on_24, null
            )
            val mBitmap = BitmapUtils.getBitmapFromDrawable(drawable)
            // Add the symbol layer icon to map for future use
            style.addImage(symbolIconId, mBitmap!!)

            // Create an empty GeoJSON source using the empty feature collection
            setUpSource(style)

            // Set up a new symbol layer for displaying the searched location's feature coordinates
            setupLayer(style)



//            mapboxMap.addOnMapClickListener(object : OnMapClickListener {
//                var source: LatLng? = null
//                override fun onMapClick(point: LatLng): Boolean {
//                    if (c == 0) {
//                        origin = Point.fromLngLat(point.longitude, point.latitude)
//                        source = point
//                        val markerOptions = MarkerOptions()
//                        markerOptions.position(point)
//                        markerOptions.title("Source")
//                        mapboxMap.addMarker(markerOptions)
//                        reverseGeocodeFunc(point, c)
//                    }
//                    if (c == 1) {
//                        destination = Point.fromLngLat(point.longitude, point.latitude)
//                        getStopId(mapboxMap, origin, destination)
//                        val markerOptions2 = MarkerOptions()
//                        markerOptions2.position(point)
//                        markerOptions2.title("destination")
//                        mapboxMap.addMarker(markerOptions2)
//                        reverseGeocodeFunc(point, c)
//                        getStopId(mapboxMap, origin, destination)
//                    }
//
//                    if (c > 1) {
//                        c = 0
//                        recreate()
//                    }
//                    c++
//                    return true
//                }
//            })
        }
    }

//    private fun reverseGeocodeFunc(point: LatLng, c: Int) {
//        val reverseGeocode = MapboxGeocoding.builder()
//            .accessToken(resources.getString(R.string.accessToken))
//            .query(Point.fromLngLat(point.longitude, point.latitude))
//            .geocodingTypes(GeocodingCriteria.TYPE_POI)
//            .build()
//        reverseGeocode.enqueueCall(object : Callback<GeocodingResponse> {
//            override fun onResponse(
//                call: Call<GeocodingResponse>,
//                response: Response<GeocodingResponse>
//            ) {
//                val results = response.body()!!.features()
//                if (results.size > 0) {
//
//                    val firstResultPoint = results[0].center()
//
//                    val feature: CarmenFeature = results[0]
//                    if (c == 0) {
//                        startLocation += feature.placeName()
//                        startLocation = startLocation!!.replace(", Dhaka, Bangladesh", ".")
//                        tvS = findViewById(R.id.tvS)
//                        tvS.text = startLocation
//                    }
//                    if (c == 1) {
//                        endLocation += feature.placeName()
//                        endLocation = endLocation!!.replace(", Dhaka, Bangladesh", ".")
//                        tvD = findViewById(R.id.tvD)
//                        tvD.text = endLocation
//                    }
//
//                    Toast.makeText(this@Map, "" + feature.placeName(), Toast.LENGTH_LONG)
//                        .show()
//
//                } else {
//                    Toast.makeText(this@Map, "Not found", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
//                throwable.printStackTrace()
//            }
//        })
//    }





    override fun onResponse(
        call: Call<DirectionsResponse?>,
        response: Response<DirectionsResponse?>
    ) {
        // You can get the generic HTTP info about the response
        if (response.body() == null) {
            Toast.makeText(
                this@Map,
                "No routes found, make sure to set right user and access token",
                Toast.LENGTH_LONG
            ).show()
            return
        } else if (response.body()!!.routes().size < 1) {
            Toast.makeText(this@Map, "NO routes found", Toast.LENGTH_LONG).show()
            return
        }
    }

    override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {}

    private fun initSearchFab() {
        fabLocationSearch.setOnClickListener {
            val intent: Intent = PlaceAutocomplete.IntentBuilder()
                .placeOptions(
                    PlaceOptions.builder()
                        .limit(10)
//                        .addInjectedFeature(work)
                        .build(PlaceOptions.MODE_CARDS)
                )
                .accessToken(resources.getString(R.string.accessToken))
                .build(this@Map)

            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }
    }

    private fun addUserLocations() {

        work = CarmenFeature.builder().text("Mapbox DC Office")
            .placeName("740 15th Street NW, Washington DC")
            .geometry(Point.fromLngLat(-77.0338348, 38.899750))
            .id("mapbox-dc")
            .properties(JsonObject())
            .build()
    }

    private fun setUpSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(geojsonSourceLayerId))
    }

    private fun setupLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                PropertyFactory.iconImage(symbolIconId),
                PropertyFactory.iconOffset(arrayOf(0f, -8f))
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            val selectedCarmenFeature: CarmenFeature = PlaceAutocomplete.getPlace(data)

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon
            val style = mapboxMap.style
            if (style != null) {
                val source = style.getSourceAs<GeoJsonSource>(geojsonSourceLayerId)
                source?.setGeoJson(
                    FeatureCollection.fromFeatures(
                        arrayOf(
                            Feature.fromJson(
                                selectedCarmenFeature.toJson()
                            )
                        )
                    )
                )

                // Move map camera to the selected location
                mapboxMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(
                                LatLng(
                                    (selectedCarmenFeature.geometry() as Point?)!!.latitude(),
                                    (selectedCarmenFeature.geometry() as Point?)!!.longitude()
                                )
                            )
                            .zoom(14.0)
                            .build()
                    ), 4000
                )
            }
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this@Map)) {

            // Get an instance of the component
            val locationComponent = mapboxMap.locationComponent

            // Activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this@Map, loadedMapStyle)
                    .build()
            )

            // Enable to make component visible
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
//            locationComponent.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {}

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.getStyle { style -> enableLocationComponent(style) }
        } else {
            finish()
        }
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val REQUEST_CODE_AUTOCOMPLETE = 1
    }

}
