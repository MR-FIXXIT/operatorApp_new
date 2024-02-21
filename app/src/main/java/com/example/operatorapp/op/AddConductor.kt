package com.example.operatorapp.op

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.operatorapp.R
import com.google.firebase.auth.FirebaseAuth

class AddConductor : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPass: EditText
    private lateinit var etConfirmPass: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBus: EditText
    private lateinit var btnReg: Button
    private lateinit var auth: FirebaseAuth

    private fun init(){
        etUsername = findViewById(R.id.etUsername_addCon)
        etPass = findViewById(R.id.etPass_addCon)
        etConfirmPass = findViewById(R.id.etConfirmPass_addCon)
        etPhone = findViewById(R.id.etPhone_addCon)
        etBus = findViewById(R.id.etBusName_addCon)
        btnReg = findViewById(R.id.btnReg_addCon)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_conductor)

        init()

        btnReg.setOnClickListener {
            var username = etUsername.text.toString().trim()
            val password = etPass.text.toString().trim()
            val confPass = etConfirmPass.text.toString().trim()
            val busName = etBus.text.toString().trim()

            if(username.isNotEmpty() && password.isNotEmpty() && confPass.isNotEmpty() && busName.isNotEmpty()) {
                if (password == confPass) {
                    username = "$username@hotmail.com"
                    register(username, password)
                } else {
                    Toast.makeText(
                        this@AddConductor,
                        "Passwords does not match",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }else{
                Toast.makeText(this@AddConductor, "Required fields cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun register(username: String, password: String){
        auth.createUserWithEmailAndPassword(username, password).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this@AddConductor, "Conductor Registered", Toast.LENGTH_SHORT).show()
                clearFields()
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun clearFields(){
        etUsername.setText("")
        etPhone.setText("")
        etPass.setText("")
        etConfirmPass.setText("")
        etBus.setText("")
    }
}