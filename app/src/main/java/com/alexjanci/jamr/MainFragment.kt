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
import kotlinx.coroutines.GlobalScope
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

    private val store = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val documentReference= store.collection("users")
    private val storageRef = Firebase.storage.reference
    private val myRef = store.collection("users").document(auth.uid!!)
    private val likesRef = store.collection("user-likes").document(auth.uid!!)

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

        val user = myRef.get()
        user.addOnSuccessListener {
            userMe = it.toObject(User::class.java)!!
        }
    }

    private fun initialize(list: List<User>){
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
        if(direction == Direction.Right){
            val likedID = users[manager.topPosition - 1].id
            val data = hashMapOf(likedID to likedID)
            likesRef.set(data, SetOptions.merge()).addOnSuccessListener {
                val likedRef = store.collection("user-likes").document(likedID).get().addOnSuccessListener {
                    if (it.data != null && it.data!!.contains("${auth.uid}")){
                        val matchedRef = store.collection("user-matches").document("${auth.uid}").collection("matches").document(likedID)
                        val matchedRef2 = store.collection("user-matches").document(likedID).collection("matches").document("${auth.uid}")

                        val matchedUser = users[manager.topPosition - 1]

                        matchedRef.set(matchedUser)
                        matchedRef2.set(userMe)
                    }
                }
            }
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

    private fun createList(){
        GlobalScope.launch(Dispatchers.IO) {

            val defaultRef = storageRef.child("users/default/default.png")
            val documents = documentReference.get().await()
            val likes = likesRef.get().await()
            for(document in documents){
                val userID = document.id
                if (userID != auth.currentUser!!.uid) {
                    if(document.id != likes[userID]){
                        var uri: Uri
                        val profileRef = storageRef.child("users/$userID/profile.jpg")

                        try {
                            uri = profileRef.downloadUrl.await()
                        } catch (e: Exception) {
                            uri = defaultRef.downloadUrl.await()
                        }
                        val name = document.data.getValue("fname").toString()
                        val city = document.data.getValue("city").toString()
                        val age = document.data.getValue("age").toString()
                        users.add(User(fname = name, city = city, age = age, pic = uri.toString(), id = userID))
                    }
                }
            }
            withContext(Main){
                try {
                    if (childFragmentManager.isDestroyed) {
                    } else {
                        try {
                            initialize(users)
                        } catch (e: Exception) {
                        }
                    }
                } catch (e: Exception){
                }
            }
        }

    }

}
