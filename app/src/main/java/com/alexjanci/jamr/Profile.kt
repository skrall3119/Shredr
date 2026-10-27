package com.alexjanci.jamr

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*

class Profile : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private val store = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    private var loadedBio = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadedBio = false
        profilePic.setImageResource(R.drawable.defaultpic)
        profilePic.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 1000)
        }
        buttonSave.setOnClickListener {
            val uid = auth.uid ?: return@setOnClickListener
            buttonSave.isEnabled = false
            store.collection("users").document(uid).set(mapOf("bio" to bioText.text.toString()), SetOptions.merge())
                .addOnCompleteListener { task ->
                    if (this.view == null) return@addOnCompleteListener
                    buttonSave.isEnabled = true
                    showMessage(if (task.isSuccessful) "Profile saved" else "Profile could not save. Your edits are still here.")
                }
        }
        logout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(context, SignIn::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.uid ?: return
        listener = store.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (view == null) return@addSnapshotListener
            if (error != null || snapshot == null || !snapshot.exists()) {
                showMessage("Profile could not load. Please check your connection or account access.")
                return@addSnapshotListener
            }
            profileCity.text = snapshot.getString("city").orEmpty()
            profileName.text = snapshot.getString("fname").orEmpty()
            profileEmail.text = snapshot.getString("email") ?: auth.currentUser?.email.orEmpty()
            if (!loadedBio) {
                bioText.setText(snapshot.getString("bio").orEmpty())
                loadedBio = true
            }
            snapshot.getString("pic")?.takeIf { it.isNotBlank() }?.let { loadPhoto(Uri.parse(it)) }
        }
        FirebaseStorage.getInstance().getReference("users/$uid/profile.jpg").downloadUrl
            .addOnSuccessListener { loadPhoto(it) }
    }

    override fun onStop() {
        listener?.remove()
        listener = null
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1000 || resultCode != Activity.RESULT_OK) return
        val imageUri = data?.data ?: return
        val uid = auth.uid ?: return
        val file = FirebaseStorage.getInstance().getReference("users/$uid/profile.jpg")
        profilePic.isEnabled = false
        file.putFile(imageUri).continueWithTask { upload ->
            if (!upload.isSuccessful) throw upload.exception ?: IllegalStateException("Upload failed")
            file.downloadUrl
        }.continueWithTask { download ->
            val url = download.result ?: throw IllegalStateException("Photo URL unavailable")
            store.collection("users").document(uid).set(mapOf("pic" to url.toString()), SetOptions.merge())
        }.addOnCompleteListener { task ->
            if (view == null) return@addOnCompleteListener
            profilePic.isEnabled = true
            if (task.isSuccessful) loadPhoto(imageUri)
            showMessage(if (task.isSuccessful) "Photo saved" else "Photo could not save. Please try again.")
        }
    }

    private fun loadPhoto(uri: Uri) {
        if (view != null) Picasso.get().load(uri).placeholder(R.drawable.defaultpic).error(R.drawable.defaultpic).into(profilePic)
    }

    private fun showMessage(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    companion object { fun newInstance(): Profile = Profile() }
}
