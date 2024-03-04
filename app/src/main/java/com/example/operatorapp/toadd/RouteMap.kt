package com.example.operatorapp.toadd

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.operatorapp.R
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RouteMap : AppCompatActivity(),
    OnMapReadyCallback, Callback<DirectionsResponse> {
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var firestore: FirebaseFirestore
    private var client: MapboxDirections? = null
    private lateinit var receivedList: MutableList<String>
    private val geojsonSourceLayerId = "geojsonSourceLayerId"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(
            this,
            resources.getString(R.string.accessToken)

        )
        setContentView(R.layout.activity_route_map)

        init()

        receivedList = intent.getStringArrayListExtra("listData")!!.toMutableList()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun init(){
        mapView = findViewById(R.id.mapView_routeMap)
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.setStyle(Style.MAPBOX_STREETS){ style ->
            initLayer(style)
            routeForm()
        }
    }

    private fun fetchStopCoordinates(stopIds: MutableList<String>) {
        Log.i("my_tag", "started fun fetchStopCoordinates")
        Log.i("my_tag", "$stopIds")
        val stopCoordinatesFetched = LinkedHashMap<String, Point>()

        for (stopId in stopIds) {
            firestore.collection("Stop")
                .document(stopId)
                .get()
                .addOnSuccessListener { stopDocument ->
                    if (stopDocument != null && stopDocument.exists()) {
                        val lat = stopDocument.getString("lat")
                        val lng = stopDocument.getString("long")
                        val point = Point.fromLngLat(lng!!.toDouble(), lat!!.toDouble())
                        stopCoordinatesFetched[stopId] = point
                        // Check if all stop coordinates have been fetched
                        if (stopCoordinatesFetched.size == stopIds.size) {
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(3000) // Delay for 3 seconds
                                Log.i("my_tag", "$stopCoordinatesFetched")
                                val orderedPoints = mutableListOf<Point>()

                                for (stopId in stopIds) {
                                    val point = stopCoordinatesFetched[stopId]
                                    if (point != null) {
                                        orderedPoints.add(point)
                                    }
                                }
                                Log.d("my_tag", "$orderedPoints")

                                getRoute(orderedPoints)
                            }
                        }
                    } else {
                        Log.d("my_tag", "Stop document not found for ID: $stopId")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("my_tag", "Error getting stop document for ID: $stopId", exception)
                }
        }
    }

    private fun getRoute(stops: List<Point>) {
        if (stops.size >= 3) {
            val origin = stops.first()
            val destination = stops.last()
            val waypoints = mutableListOf<Point>()
            for (i in 1 until stops.size - 1) {
                waypoints.add(stops[i])
            }

            client = MapboxDirections.builder()
                .origin(origin)
                .destination(destination)
                .waypoints(waypoints)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(resources.getString(R.string.accessToken))
                .build()

            client!!.enqueueCall(this)
        } else {
            // Handle case when there are not enough stops to construct a route
            Toast.makeText(this, "At least 3 stops are required to construct a route", Toast.LENGTH_SHORT).show()
        }
    }

    private fun routeForm(){
        fetchStopCoordinates(receivedList)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon
            val style = mapboxMap!!.style
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

    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {}

    override fun onResponse(
        call: Call<DirectionsResponse>,
        response: Response<DirectionsResponse>
    ) {
        if (response.body() == null) {
            Toast.makeText(
                this,
                "NO routes found make sure to set right user and access token",
                Toast.LENGTH_LONG
            ).show()
            return
        } else if (response.body()!!.routes().size < 1) {
            Toast.makeText(this, "NO routes found", Toast.LENGTH_LONG).show()
        }

        val currentRoute = response.body()!!.routes()[0]
        val distance = currentRoute.distance() / 1000
        val st = String.format("%.2f K.M", distance)
        val dv = findViewById<TextView>(R.id.distanceView_routeMap)
        dv.text = st

        mapboxMap!!.getStyle { style -> // Retrieve and update the source designated for showing the directions route
            val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)

            source?.setGeoJson(
                LineString.fromPolyline(
                    currentRoute.geometry()!!,
                    Constants.PRECISION_6
                )
            )
        }
    }

    private fun initLayer(loadedMapStyle: Style){
        val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)

        routeLayer.setProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineColor(Color.parseColor("#1878d9"))
        )
        loadedMapStyle.addLayer(routeLayer)
        loadedMapStyle.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
    }

    companion object {
        private const val REQUEST_CODE_AUTOCOMPLETE = 1
        private const val ROUTE_LAYER_ID = "route-layer-id"
        private const val ROUTE_SOURCE_ID = "route-source-id"
    }

}