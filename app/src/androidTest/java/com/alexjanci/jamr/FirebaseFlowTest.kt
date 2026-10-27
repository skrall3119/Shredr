package com.alexjanci.jamr

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Explicit live smoke test: creates only uniquely named disposable fixtures. */
@RunWith(AndroidJUnit4::class)
class FirebaseFlowTest {
    @Test fun isolatedAccountsCanSaveProfilesMatchAndExchangeMessage() {
        org.junit.Assume.assumeTrue("Live Firebase testing requires explicit opt-in",
            InstrumentationRegistry.getArguments().getString("liveFirebase") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val options = FirebaseApp.getInstance().options
        val apps = (0..1).map { FirebaseApp.initializeApp(context, options, "flow-$suffix-$it")!! }
        val auths = apps.map { FirebaseAuth.getInstance(it) }
        val stores = apps.map { FirebaseFirestore.getInstance(it) }
        val ids = mutableListOf<String>()
        fun <T> wait(task: com.google.android.gms.tasks.Task<T>): T = Tasks.await(task, 30, TimeUnit.SECONDS)
        fun denied(task: com.google.android.gms.tasks.Task<*>) {
            try {
                wait(task)
                fail("Expected PERMISSION_DENIED")
            } catch (e: java.util.concurrent.ExecutionException) {
                assertEquals(com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED,
                    (e.cause as? com.google.firebase.firestore.FirebaseFirestoreException)?.code)
            }
        }
        try {
            for (i in 0..1) {
                val email = "shredr-test-$suffix-$i@example.com"
                val password = UUID.randomUUID().toString()
                ids.add(wait(auths[i].createUserWithEmailAndPassword(email, password)).user!!.uid)
                auths[i].signOut()
                assertEquals(ids[i], wait(auths[i].signInWithEmailAndPassword(email, password)).user!!.uid)
                wait(stores[i].collection("public-profiles").document(ids[i]).set(mapOf("id" to ids[i], "fname" to "Disposable flow test", "age" to "25", "bio" to "Automated test fixture")))
                assertEquals("Automated test fixture", wait(stores[i].collection("public-profiles").document(ids[i]).get()).getString("bio"))
            }
            val a = ids[0]; val b = ids[1]
            denied(stores[1].collection("public-profiles").document(a).update("bio", "Unauthorized"))
            denied(stores[1].collection("users").document(a).get(com.google.firebase.firestore.Source.SERVER))
            denied(stores[1].collection("user-likes").document(a).collection("likes").get(com.google.firebase.firestore.Source.SERVER))
            denied(stores[0].collection("user-matches").document(a).collection("matches").document(b).set(User(id=b)))
            wait(stores[0].collection("user-likes").document(a).collection("likes").document(b).set(mapOf("id" to b)))
            wait(stores[1].collection("user-likes").document(b).collection("likes").document(a).set(mapOf("id" to a)))
            val batch = stores[0].batch()
            batch.set(stores[0].collection("user-matches").document(a).collection("matches").document(b), User(fname="Disposable flow test", id=b, age="25", bio="Automated test fixture"))
            batch.set(stores[0].collection("user-matches").document(b).collection("matches").document(a), User(fname="Disposable flow test", id=a, age="25", bio="Automated test fixture"))
            wait(batch.commit())
            val message = ChatLogFragment.ChatMessage(suffix,"Automated test message",a,b,System.currentTimeMillis()/1000)
            wait(stores[0].batch()
                .set(stores[0].collection("user-messages").document(a).collection(b).document(suffix),message)
                .set(stores[0].collection("user-messages").document(b).collection(a).document(suffix),message).commit())
            assertEquals(message.text, wait(stores[1].collection("user-messages").document(b).collection(a).document(suffix).get()).getString("text"))
            denied(stores[1].collection("user-messages").document(a).collection(b).document(suffix).get(com.google.firebase.firestore.Source.SERVER))
            denied(stores[0].collection("user-messages").document(a).collection(b).document(suffix).update("text", "Altered"))
        } finally {
            for (i in ids.indices) {
                val own = ids[i]
                val batch = stores[i].batch()
                for (other in ids.filter { it != own }) {
                    batch.delete(stores[i].collection("user-likes").document(own).collection("likes").document(other))
                    batch.delete(stores[i].collection("user-matches").document(own).collection("matches").document(other))
                    batch.delete(stores[i].collection("user-messages").document(own).collection(other).document(suffix))
                }
                batch.delete(stores[i].collection("user-likes").document(own))
                batch.delete(stores[i].collection("public-profiles").document(own))
                wait(batch.commit())
                auths[i].currentUser?.let { wait(it.delete()) }
            }
            apps.forEach { it.delete() }
        }
    }
}
