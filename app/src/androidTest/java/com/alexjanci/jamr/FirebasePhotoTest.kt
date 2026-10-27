package com.alexjanci.jamr

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebasePhotoTest {
    private fun <T> wait(task: Task<T>): T = Tasks.await(task, 60, TimeUnit.SECONDS)

    @Test fun photoUploadDownloadAndProfileUrlRoundTrip() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveFirebase") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "photo-$suffix")!!
        val auth = FirebaseAuth.getInstance(app)
        val store = FirebaseFirestore.getInstance(app)
        val storage = FirebaseStorage.getInstance(app)
        storage.maxUploadRetryTimeMillis = 15000
        storage.maxDownloadRetryTimeMillis = 15000
        val file = File(context.cacheDir, "photo-$suffix.jpg")
        var uploaded = false
        var uid: String? = null
        try {
            uid = wait(auth.createUserWithEmailAndPassword("shredr-photo-$suffix@example.com", UUID.randomUUID().toString())).user!!.uid
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.GREEN)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
            val ref = storage.getReference("users/$uid/profile.jpg")
            wait(ref.putFile(Uri.fromFile(file), StorageMetadata.Builder().setContentType("image/jpeg").build()))
            uploaded = true
            val url = wait(ref.downloadUrl).toString()
            assertTrue(url.startsWith("https://"))
            wait(store.collection("users").document(uid).set(mapOf("id" to uid, "fname" to "Disposable photo test", "pic" to url)))
            assertEquals(url, wait(store.collection("users").document(uid).get(Source.SERVER)).getString("pic"))
            assertArrayEquals(file.readBytes(), wait(ref.getBytes(1024 * 1024)))
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            try {
                assertEquals(200, connection.responseCode)
                assertArrayEquals(file.readBytes(), connection.inputStream.use { it.readBytes() })
            } finally { connection.disconnect() }
        } finally {
            try {
                uid?.let {
                    if (uploaded) wait(storage.getReference("users/$it/profile.jpg").delete())
                    wait(store.collection("users").document(it).delete())
                    auth.currentUser?.let { user -> wait(user.delete()) }
                }
            } finally {
                file.delete()
                app.delete()
            }
        }
    }
}
