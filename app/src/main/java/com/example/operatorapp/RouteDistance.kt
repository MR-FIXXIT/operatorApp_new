package com.example.operatorapp

import android.app.IntentService
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RouteDistance : IntentService("RouteDistance"), Callback<DirectionsResponse> {
    private lateinit var firestore: FirebaseFirestore
    private var client: MapboxDirections? = null
    private lateinit var receivedList: MutableList<String>

    override fun onHandleIntent(intent: Intent?) {

        Log.i("my_tag", "service Started")
        init()

        receivedList = intent?.getStringArrayListExtra("listData")!!.toMutableList()

        routeForm()
    }

    private fun init(){
        firestore = FirebaseFirestore.getInstance()
    }

   private fun fetchStopCoordinates(stopIds: MutableList<String>) {
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
            Toast.makeText(this, "No routes found", Toast.LENGTH_LONG).show()
        }

        val currentRoute = response.body()!!.routes()[0]
        val distance = currentRoute.distance() / 1000
        Log.i("my_tag", "Distance : $distance")
        val broadcastIntent = Intent().apply {
            action = "ROUTE_DISTANCE_ACTION"
            putExtra("distance", distance)
        }
        sendBroadcast(broadcastIntent)
    }

}