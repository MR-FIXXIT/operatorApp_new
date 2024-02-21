package com.example.operatorapp.op

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.operatorapp.R
import com.google.firebase.auth.FirebaseAuth

class OpHome : AppCompatActivity() {
    private lateinit var btnAddRoute: Button
    private lateinit var btnAddConductor: Button
    private lateinit var btnSignOut: Button
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_op_home)

        init()

        btnAddRoute.setOnClickListener {
            startActivity(Intent(this@OpHome, AddRoute::class.java))
        }

        btnAddConductor.setOnClickListener {
            startActivity(Intent(this@OpHome, AddConductor::class.java))
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this@OpHome, OpSignin::class.java))
        }


    }

    private fun init(){
        btnAddRoute = findViewById(R.id.btnAddRoute)
        btnAddConductor = findViewById(R.id.btnAddConductor)
        btnSignOut = findViewById(R.id.btnSignOut_ophome)
        auth = FirebaseAuth.getInstance()
    }
}