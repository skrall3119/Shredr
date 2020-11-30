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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.android.synthetic.main.fragment_sign_up.*

class SignIn : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_sign_in)

        val fAuth = FirebaseAuth.getInstance()

        if(fAuth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        buttonSignIn.setOnClickListener {
            val email:String = loginEmail.text.toString().trim()
            val password:String = loginPass.text.toString().trim()

            if (TextUtils.isEmpty(email)){
                loginEmail.error = "Email is required"
            }

            else if (TextUtils.isEmpty(password)){
                loginPass.error = "Password is required"
            }

            else {
                fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        Toast.makeText(this, "User Signed In.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                    }

                    else {
                        Toast.makeText(this, "Error! " + task.exception!!.message.toString(), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        buttonSignUp.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
    }
}