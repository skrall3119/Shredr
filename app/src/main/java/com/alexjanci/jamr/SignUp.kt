package com.alexjanci.jamr

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.common.primitives.UnsignedBytes.toInt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_sign_up.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class SignUp : AppCompatActivity() {

    private lateinit var fAuth: FirebaseAuth
    private lateinit var fStore: FirebaseFirestore
    private lateinit var userID: String
    private var age = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_sign_up)

        fAuth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance()

        if(fAuth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        editTextDate.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                editTextDate.showSoftInputOnFocus = false
                editTextDate.hideKeyboard()

                val c = Calendar.getInstance()
                val curYear = c.get(Calendar.YEAR)
                val curMonth = c.get(Calendar.MONTH)
                val day = c.get(Calendar.DAY_OF_MONTH)

                val dpd = DatePickerDialog(this, { view, year, month, dayOfMonth ->
                    val text = "" + dayOfMonth + "" + (month + 1) + "" + year
                    editTextDate.setText(text)
                    age = curYear - year
                }, curYear, curMonth, day)
                dpd.show()
            }
        }


        registerButton.setOnClickListener {
            val email:String = editTextRegEmail.text.toString().trim()
            val password:String = editTextPassword.text.toString().trim()
            val confirmPass: String = editTextConfirmPassword.text.toString().trim()
            val name: String = editTextName.text.toString().trim()
            val birthDate: String = editTextDate.text.toString().trim()

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
            else if (TextUtils.isEmpty(birthDate)){
                editTextDate.error = "Birth Date is required"
            }
            else if (age < 18){
               editTextDate.error = "Must be over 18 years of age"
            }

            else {
                fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) {task ->
                    if(task.isSuccessful){
                        Toast.makeText(this, "User Created.", Toast.LENGTH_SHORT).show()
                        userID = fAuth.currentUser!!.uid
                        val documentReference = fStore.collection("users").document(userID)
                        val user = hashMapOf<String, String>()
                        user.put("fname", name)
                        user.put("email",email)
                        user.put("age", age.toString())
                        user.put("id", userID)
                        uploadDefaultImageToFirebase()
                        documentReference.set(user).addOnSuccessListener {
                        }
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Error! " + task.exception!!.message.toString(), Toast.LENGTH_LONG).show()
                    }
                }
            }

        }
    }

    private fun uploadDefaultImageToFirebase() {
        val storageRef = Firebase.storage.reference
        val fileRef = storageRef.child("users/$userID/profile.jpg")
        val uri = Uri.parse("android.resource://com.alexjanci.jamr/drawable/defaultpic")
        fileRef.putFile(uri).addOnSuccessListener {
        }.addOnFailureListener {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun EditText.hideKeyboard(){
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}