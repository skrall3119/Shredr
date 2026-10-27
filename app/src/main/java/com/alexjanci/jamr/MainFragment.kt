package com.alexjanci.jamr

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.yuyakaido.android.cardstackview.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.item.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.lang.IllegalStateException

class MainFragment : Fragment(), CardStackListener {

    private val manager by lazy { CardStackLayoutManager(context, this) }
    private lateinit var adapter: CardStackAdapter
    private val users = ArrayList<User>()
    private lateinit var userMe: User
    private var loadJob: Job? = null

    private val store = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val documentReference= store.collection("public-profiles")
    private val storageRef = Firebase.storage.reference
    private val myRef = store.collection("public-profiles").document(auth.uid!!)
    private val likesRef = store.collection("user-likes").document(auth.uid!!).collection("likes")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onStart() {
        super.onStart()
        createList()
        setupButton()

    }

    private fun initialize(list: List<User>){
        discoveryStatus.text = "You’re all caught up.\nNew connections will appear here."
        discoveryStatus.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        button_container.visibility = if (list.isEmpty()) View.INVISIBLE else View.VISIBLE
        manager.setStackFrom(StackFrom.None)
        manager.setVisibleCount(3)
        manager.setTranslationInterval(8.0f)
        manager.setScaleInterval(0.95f)
        manager.setSwipeThreshold(0.3f)
        manager.setMaxDegree(20.0f)
        manager.setDirections(Direction.HORIZONTAL)
        manager.setCanScrollHorizontal(true)
        manager.setCanScrollVertical(true)
        manager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
        manager.setOverlayInterpolator(LinearInterpolator())

        adapter = CardStackAdapter(list)
        card_stack_view.adapter = adapter
        card_stack_view.layoutManager = manager
        card_stack_view.itemAnimator.apply {
            if (this is DefaultItemAnimator) {
                supportsChangeAnimations = false
            }
        }
    }

    companion object{
        fun newInstance():MainFragment = MainFragment()
    }

    override fun onCardDragging(direction: Direction, ratio: Float) {
    }

    override fun onCardSwiped(direction: Direction) {
        val matchedUser = users.getOrNull(manager.topPosition - 1) ?: return
        if (manager.topPosition >= users.size) {
            discoveryStatus.visibility = View.VISIBLE
        }
        if (direction != Direction.Right || !::userMe.isInitialized) return
        val uid = auth.uid ?: return
        val likedID = matchedUser.id
        likesRef.document(likedID).set(mapOf("id" to likedID)).continueWithTask { saved ->
            if (!saved.isSuccessful) throw saved.exception ?: IllegalStateException("Like failed")
            store.collection("user-likes").document(likedID).collection("likes").document(uid).get()
        }.continueWithTask { liked ->
            val snapshot = liked.result ?: throw IllegalStateException("Like unavailable")
            if (snapshot.exists()) {
                // Read current canonical profiles; display fallbacks and stale cards
                // must not be written into another person's match record.
                store.runTransaction { transaction ->
                    val own = transaction.get(documentReference.document(uid)).toObject(User::class.java)
                        ?: throw IllegalStateException("Profile missing")
                    val other = transaction.get(documentReference.document(likedID)).toObject(User::class.java)
                        ?: throw IllegalStateException("Profile missing")
                    transaction.set(store.collection("user-matches").document(uid).collection("matches").document(likedID), other)
                    transaction.set(store.collection("user-matches").document(likedID).collection("matches").document(uid), own)
                    true
                }
            } else {
                com.google.android.gms.tasks.Tasks.forResult(false)
            }
        }.addOnFailureListener {
            if (view != null) android.widget.Toast.makeText(context, "Could not save connection. Rewind and try again.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onCardRewound() {
    }

    override fun onCardCanceled() {
    }

    override fun onCardAppeared(view: View?, position: Int) {
    }

    override fun onCardDisappeared(view: View?, position: Int) {
        val textView = item_name
        try {
        }
        catch (e: IllegalStateException){
        }
    }


    private fun setupButton() {
        val skip = skip_button
        skip.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Left)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            manager.setSwipeAnimationSetting(setting)
            card_stack_view.swipe()
        }

        val rewind = rewind_button
        rewind.setOnClickListener {
            val setting = RewindAnimationSetting.Builder()
                .setDirection(Direction.Bottom)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(DecelerateInterpolator())
                .build()
            manager.setRewindAnimationSetting(setting)
            card_stack_view.rewind()
        }

        val like = like_button
        like.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Right)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            manager.setSwipeAnimationSetting(setting)
            card_stack_view.swipe()
        }
    }

    override fun onStop() {
        loadJob?.cancel()
        super.onStop()
    }

    private fun createList() {
        button_container.visibility = View.INVISIBLE
        discoveryStatus.visibility = View.VISIBLE
        discoveryStatus.text = "Loading profiles…"
        loadJob?.cancel()
        loadJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val own = myRef.get().await()
                userMe = own.toObject(User::class.java) ?: throw IllegalStateException("Profile missing")
                // Demo accounts only discover other accounts in their demo session.
                val demoGroup = own.getString("demoGroup")
                val documents = if (demoGroup.isNullOrBlank()) documentReference.get().await()
                    else documentReference.whereEqualTo("demoGroup", demoGroup).get().await()
                val likes = likesRef.get().await()
                val loaded = ArrayList<User>()
                for (document in documents) {
                    if (document.id == auth.uid || likes.documents.any { it.id == document.id }) continue
                    var photo = document.getString("pic").orEmpty()
                    if (photo.isBlank()) {
                        photo = try { storageRef.child("users/" + document.id + "/profile.jpg").downloadUrl.await().toString() }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { "android.resource://com.alexjanci.jamr/drawable/defaultpic" }
                    }
                    loaded.add((document.toObject(User::class.java)).copy(id = document.id, pic = photo))
                }
                users.clear()
                users.addAll(loaded)
                initialize(users)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (view != null) {
                    discoveryStatus.visibility = View.VISIBLE
                    discoveryStatus.text = "Couldn’t load profiles.\nTap here to retry."
                    discoveryStatus.setOnClickListener { createList() }
                }
            }
        }
    }
}
