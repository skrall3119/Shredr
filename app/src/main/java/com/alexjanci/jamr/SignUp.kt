package com.alexjanci.jamr

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_sign_up.*

class SignUp : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.fragment_sign_up)

        val fAuth = FirebaseAuth.getInstance()

        if(fAuth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        registerButton.setOnClickListener {
            val email:String = editTextRegEmail.text.toString()
            val password:String = editTextPassword.text.toString()
            val confirmPass: String = editTextConfirmPassword.text.toString()

            if (TextUtils.isEmpty(email)){
                editTextRegEmail.error = "Email is required"
            }

            else if (TextUtils.isEmpty(password)){
                editTextPassword.error = "Password is required"
            }

            else if (password.length < 6){
                editTextPassword.error = "Password length must be more than 6"
            }

            else if (password != confirmPass){
                editTextConfirmPassword.error = "Passwords do not match"
            }

            else {
                fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if(it.isSuccessful){
                        Toast.makeText(this, "User Created.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        Toast.makeText(this, "Error has occured", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }
}