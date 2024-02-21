package com.example.operatorapp.op

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.operatorapp.R
import com.google.firebase.auth.FirebaseAuth

class OpSignin : AppCompatActivity() {

    private lateinit var btnSignin: Button
    private lateinit var tvSignup: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText

    private fun init(){
        auth = FirebaseAuth.getInstance()
        btnSignin = findViewById(R.id.btnSignin)
        tvSignup = findViewById(R.id.tvSignup)
        etEmail = findViewById(R.id.etEmail_addCon)
        etPass = findViewById(R.id.etPass_addCon)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_op_signin)

        init()

        tvSignup.setOnClickListener {
            val intent = Intent(this, OpSignup::class.java)
            startActivity(intent)
        }

        btnSignin.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()

        if(auth.currentUser != null){
            startActivity(Intent(this, OpHome::class.java))
        }
    }

    private fun signIn(){
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()

        if (email.isNotEmpty() && pass.isNotEmpty()) {

            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                if (it.isSuccessful) {
                    startActivity(Intent(this, OpHome::class.java))
                } else {
                    Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Fill the fields to continue", Toast.LENGTH_SHORT).show()
        }
    }
}