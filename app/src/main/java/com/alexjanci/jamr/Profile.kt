package com.alexjanci.jamr

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Profile : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var store: FirebaseFirestore
    private lateinit var userID: String
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var storageRef: StorageReference
    private lateinit var documentReference: DocumentReference
    private lateinit var updateProfileListener: ListenerRegistration


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        store = FirebaseFirestore.getInstance()
        storageRef = Firebase.storage.reference

        userID = auth.currentUser!!.uid

        documentReference = store.collection("users").document(userID)
        updateProfileListener = documentReference.addSnapshotListener { snapshot, _ ->
            try {
                profileCity.text = snapshot!!.getString("city")
                profileName.text = snapshot.getString("fname")
                profileEmail.text = snapshot.getString("email")
                bioText.setText(snapshot.getString("bio"))
            } catch (e: NullPointerException) {
            }
        }

        val profileRef = storageRef.child("users/$userID/profile.jpg")
        profileRef.downloadUrl.addOnSuccessListener {
            try {
                Picasso.get().load(it).into(profilePic)
            } catch (e: Exception) {
                val uri = Uri.parse("android.resource://com.alexjanci.jamr/drawable/defaultpic")
                GlobalScope.launch {
                    profilePic.setImageResource(R.drawable.defaultpic)
                    uploadImageToFirebase(uri)
                }
            }

        }

        profilePic.setOnClickListener {
            val openGalleryIntnet = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(openGalleryIntnet, 1000)

        }

        buttonSave.setOnClickListener {
            firebaseUser = auth.currentUser!!
            val bio = bioText.text.toString()
            val data = hashMapOf("bio" to bio)
            val docRef = store.collection("users").document(firebaseUser.uid)
            docRef.set(data, SetOptions.merge())

        }

        logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(context, SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            if (resultCode == Activity.RESULT_OK) {
                val imageUri = data!!.data

                val ref = store.collection("users").document(userID)
                val uriData = hashMapOf(imageUri.toString() to "uri")
                ref.set(uriData, SetOptions.merge())

                uploadImageToFirebase(imageUri!!)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        updateProfileListener.remove()
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val fileRef = storageRef.child("users/$userID/profile.jpg")
        fileRef.putFile(imageUri).addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener {
                Picasso.get().load(it).into(profilePic)
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        fun newInstance(): Profile = Profile()
    }

}