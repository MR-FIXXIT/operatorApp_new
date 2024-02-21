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

class OpSignup : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var etConfirmPass: EditText
    private lateinit var btnSignup: Button
    private lateinit var tvSignin: TextView


    private fun init(){
        firebaseAuth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.etEmailSignup)
        etPass = findViewById(R.id.etPassSignup)
        etConfirmPass = findViewById(R.id.etConfirmPass)
        btnSignup = findViewById(R.id.btnSignup)
        tvSignin = findViewById(R.id.tvSignin)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_op_signup)

        init()

        tvSignin.setOnClickListener {
            startActivity(Intent(this, OpSignin::class.java))
        }

        btnSignup.setOnClickListener {
            createUser()
        }
    }

    private fun createUser(){
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()
        val confirmPass = etConfirmPass.text.toString()

        if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
            if (pass == confirmPass) {

                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        startActivity(Intent(this, OpSignin::class.java))
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Fill the fields to continue", Toast.LENGTH_SHORT).show()
        }
    }

}