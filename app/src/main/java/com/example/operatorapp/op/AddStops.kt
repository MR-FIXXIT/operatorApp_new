package com.example.operatorapp.op

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.operatorapp.R
import com.google.firebase.firestore.FirebaseFirestore

class AddStops : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var etLat: EditText
    private lateinit var etLong: EditText
    private lateinit var etName: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_stops)

        init()

        btnSave.setOnClickListener {
            addStop()
        }
    }

    private fun init(){
        db = FirebaseFirestore.getInstance()
        etLong = findViewById(R.id.etLongitude)
        etLat = findViewById(R.id.etLatitude)
        etName = findViewById(R.id.etStopName)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun addStop(){
        val stopName: String = etName.text.toString()
        val lat: String = etLat.text.toString()
        val long: String = etLong.text.toString()

        if(lat.isNotEmpty() || long.isNotEmpty() || stopName.isNotEmpty()){
            val map = HashMap<String, Any>()
            map["lat"] = lat
            map["long"] = long
            map["stop_name"] = stopName

            db.collection("Stop").document().set(map)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Data Saved", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this,"Failed", Toast.LENGTH_SHORT).show()
                }
        }else{
            Toast.makeText(this,"Fields cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }
}