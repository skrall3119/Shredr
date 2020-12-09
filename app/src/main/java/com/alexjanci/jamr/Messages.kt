package com.alexjanci.jamr

import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.fragment_messages.*
import kotlinx.android.synthetic.main.item.view.*
import kotlinx.android.synthetic.main.user_row.view.*
import kotlin.math.log

class Messages : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    private lateinit var documentListener: ListenerRegistration

    override fun onStart() {
        super.onStart()

        val adapter = GroupAdapter<GroupieViewHolder>()

        recyclerViewMessage.adapter = adapter

        fetchUsers()
    }

    private fun fetchUsers(){
        val documentReference = FirebaseFirestore.getInstance().collection("users")
        documentListener = documentReference.addSnapshotListener { value, _ ->

            val adapter = GroupAdapter<GroupieViewHolder>()

            value!!.forEach {
                val name = it.getString("fName")
                val id = it.id
                FirebaseStorage.getInstance().getReference().child("users/$id/profile.jpg").downloadUrl.addOnSuccessListener {
                    Log.e("user", "$it")
                    val message = UserMessage(name!!, it.toString()!!)
                    adapter.add(message)
                }
            }
            recyclerViewMessage.adapter = adapter
        }
    }

    override fun onStop() {
        super.onStop()
        documentListener.remove()
    }

    companion object {
        fun newInstance(): Messages = Messages()
    }
}
class UserMessage(val name: String, val uri: String): Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.userName.text = name
        Picasso.get().load(uri).into(viewHolder.itemView.userImage)
    }

    override fun getLayout(): Int {
        return R.layout.user_row
    }
}
