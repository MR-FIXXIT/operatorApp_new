package com.example.operatorapp.op

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.operatorapp.R
import com.example.operatorapp.ViewStops
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class AddRoute : AppCompatActivity() {                                              /* the way everything handled in here is a mess
                                                                                     pls don't touch anything*/
    private lateinit var llMain: LinearLayout
    private lateinit var tvSource: TextView
    private lateinit var tvEnd: TextView
    private lateinit var tvInter: TextView
    private lateinit var btnAdd: Button
    private lateinit var btnDelete: Button
    private lateinit var btnSave: Button
    private lateinit var newTV: TextView
    private var indexInLayout: Int = 0
    private lateinit var dynamicTextViews: MutableList<TextView>
    private lateinit var stopIdInRoute: MutableList<String?>
    private lateinit var dynamicTVStopId: MutableList<String?>
    private var selectedStop: ArrayList<String?>? = null
    private var count: Int = 0
    private val VIEW_STOPS_REQUEST_CODE = 1

    private fun init(){
        llMain = findViewById(R.id.llMain)
        tvSource = findViewById(R.id.tvSource)
        tvEnd = findViewById(R.id.tvEnd)
        tvInter = findViewById(R.id.tvInter)
        btnAdd = findViewById(R.id.btnAdd)
        btnDelete = findViewById(R.id.btnDelete)
        btnSave = findViewById(R.id.btnSave)
        dynamicTextViews = mutableListOf()
        stopIdInRoute = MutableList(3){""}
        dynamicTVStopId = MutableList(5){""}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_route)

        init()

        tvSource.setOnClickListener {
            startViewStopsActivity("source")
        }

        tvEnd.setOnClickListener {
            if(tvSource.text.toString().isEmpty()){
                Toast.makeText(this,
                    "Add the source stop first",
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                startViewStopsActivity("end")
            }
        }

        tvInter.setOnClickListener {
            if(tvSource.text.toString().isEmpty()){
                Toast.makeText(this,
                    "Add the source stop first",
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                startViewStopsActivity("inter")
            }
        }

        btnAdd.setOnClickListener {
            if(isDynamicTVEmpty()){
                Toast.makeText(this,
                    "Fill the empty field to add more stops",
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                addTV()
                count++
            }
        }

        btnDelete.setOnClickListener {
            dltTV()
        }

        btnSave.setOnClickListener {
            if(isAnyTVEmpty()){
                Toast.makeText(this,
                    "Make sure all the stops are added and no fields are left empty",
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                removeEmpty()
                stopIdInRoute.addAll(2, dynamicTVStopId)
                uploadDB()
                resetAll()
            }

            Log.i("india", "route : $stopIdInRoute")
        }
    }

    private fun startViewStopsActivity(textViewTag: String) {
        val intent = Intent(this, ViewStops::class.java)
        intent.putExtra("true", true)
        intent.putExtra("textViewTag", textViewTag)
        startActivityForResult(intent, VIEW_STOPS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VIEW_STOPS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedStop = data?.getStringArrayListExtra("selectedStop")
            val stopId = selectedStop?.get(3)

            when (val tag = data?.getStringExtra("textViewTag")) {
                "source" -> {
                    stopIdInRoute[0] = stopId
                    selectTV(tvSource)
                }

                "end" -> {
                    val lastIndex = stopIdInRoute.size - 1

                    stopIdInRoute[lastIndex] = stopId

                    selectTV(tvEnd)
                }

                "inter" -> {
                    stopIdInRoute[1] = stopId
                    selectTV(tvInter)
                }

                else -> {
                    val index = tag!!.toInt() - 2

                    if(dynamicTVStopId.last()!!.isNotEmpty()){
                        dynamicTVStopId.addAll(List(5){""})
                    }
                    dynamicTVStopId[index] = stopId
                    selectTV(dynamicTextViews[index])
                }
            }
        }
    }

    private fun selectTV(tv: TextView){
        tv.text = "${selectedStop?.get(0)}" +
                "\nLatitude:${selectedStop?.get(1)}\tLongitude:${selectedStop?.get(2)}"
    }

    private fun addTV(){
        newTV = TextView(this)
        val layoutParams = LinearLayout.LayoutParams(
            (340 * resources.displayMetrics.density).toInt(),
            (62 * resources.displayMetrics.density).toInt()
        )

        layoutParams.gravity = Gravity.CENTER
        newTV.layoutParams = layoutParams
        newTV.gravity = Gravity.CENTER
        newTV.hint = "Intermediate Stop"
        newTV.setHintTextColor(resources.getColor(R.color.text_view_hint_ideal))
        newTV.textSize = 21f
        newTV.setBackgroundResource(R.color.text_view_ideal)
        newTV.setTextColor(resources.getColor(R.color.black))
        llMain.addView(newTV, llMain.indexOfChild(tvEnd))

        indexInLayout = llMain.indexOfChild(newTV)

        val tag = indexInLayout.toString()
        newTV.setOnClickListener {
            startViewStopsActivity(tag)
        }

        dynamicTextViews.add(newTV)
    }

    private fun dltTV(){
        val tvToRemove = dynamicTextViews.lastOrNull()
        llMain.removeView(tvToRemove)
        dynamicTextViews.removeLastOrNull()
        dynamicTVStopId.removeLastOrNull()
    }

    private fun isDynamicTVEmpty(): Boolean{
        dynamicTextViews.forEach { tv ->
            if(tv.text.toString().isEmpty()){
                return true
            }
        }

        return false
    }

    private fun resetAll() {
        tvSource.text = ""
        tvEnd.text = ""
        tvInter.text = ""

        dynamicTextViews.forEach { tv ->
            llMain.removeView(tv)
        }

        stopIdInRoute.clear()
        dynamicTVStopId.clear()
        dynamicTextViews.clear()
    }

    private fun isAnyTVEmpty(): Boolean{
        dynamicTextViews.forEach { tv ->
            if(tv.text.toString().isEmpty()){
                return true
            }
        }

        return tvSource.text.toString().isEmpty() || tvEnd.text.toString().isEmpty() || tvInter.text.toString().isEmpty()
    }

    private fun uploadDB(){
        val db = FirebaseFirestore.getInstance()

        val routeId = generateRouteId(stopIdInRoute.first(), stopIdInRoute.last() )
        val routeData = hashMapOf<String, Any>()

        for(i in stopIdInRoute.indices){
            routeData["stop${i+1}"] = stopIdInRoute[i] ?: ""
        }

        db.collection("Route").document(routeId).set(routeData)
            .addOnSuccessListener {
                // Document was successfully written
                Toast.makeText(this, "Route data uploaded to Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Handle any errors that occurred while writing the document
                Toast.makeText(this, "Error uploading route data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun generateRouteId(sourceStop: String?, destinationStop: String?): String {
        // Concatenate source and destination stops with a delimiter
        return "$sourceStop-$destinationStop"
    }


    private fun removeEmpty(){
        val size = dynamicTVStopId.size-1

        for(i in size downTo 0){
            if(dynamicTVStopId[i].toString().isEmpty()){
                dynamicTVStopId.removeAt(i)
            }
        }
    }


}







//  I HATE IT HERE
