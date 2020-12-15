package com.alexjanci.jamr

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import kotlinx.android.synthetic.main.fragment_chat_log.*
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class ChatLogFragment : Fragment() {

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser!!.uid

    val TAG = "ChatLog"

    val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_log, container, false)
        return view
    }

    override fun onStart() {
        super.onStart()

        recyclerview_chat_log.adapter = adapter

        listenForMessages()

        send_button.setOnClickListener {
            performSendMessage()
        }
    }

    var messageListener: ListenerRegistration? = null

    private fun listenForMessages() {
        val fromId = auth.uid
        val toId: String? = arguments?.getString("id")
        val fromUri = FirebaseStorage.getInstance()
            .getReference("/users/$uid/profile.jpg").downloadUrl.toString()

        val ref = FirebaseFirestore.getInstance().collection("user-messages").document("$fromId")
            .collection("$toId")
        messageListener = ref.addSnapshotListener { value, error ->
            adapter.clear()
            val messages = ArrayList<ChatMessage>()
            value!!.forEach {
                val chatMessage = it.toObject(ChatMessage::class.java)
                messages.add(chatMessage)
            }
            val sortedMessages = messages.sorted()
            for (message in sortedMessages) {
                if (message.fromId == uid) {
                    adapter.add(ChatToItem(message.text, Uri.parse(fromUri) as Uri))
                } else {
                    val uri = requireArguments().get("uri").toString()
                    adapter.add(ChatFromItem(message.text, Uri.parse(uri) as Uri))
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        messageListener?.remove()
    }

    class ChatMessage(
        val id: String,
        val text: String,
        val fromId: String,
        val toId: String,
        val timeStamp: Long
    ) : Comparable<ChatMessage> {
        constructor() : this("", "", "", "", -1)

        override fun compareTo(other: ChatMessage) = compareValuesBy(this, other, { it.timeStamp })
    }

    private fun performSendMessage() {
        val text = messageText.text.toString()

        val fromId = uid
        val toId: String? = arguments?.getString("id")

        val reference =
            FirebaseFirestore.getInstance().collection("user-messages").document("$fromId")
                .collection("$toId").document()

        val toReference =
            FirebaseFirestore.getInstance().collection("user-messages").document("$toId")
                .collection(
                    "$fromId"
                ).document()
        val chatMessage =
            ChatMessage(reference.id, text, fromId, toId!!, System.currentTimeMillis() / 1000)
        reference.set(chatMessage)
        toReference.set(chatMessage)

        messageText.setText("")
        recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
    }

    companion object {
        fun newInstance(): ChatLogFragment = ChatLogFragment()
    }
}

class ChatFromItem(val text: String, val uri: Uri) : Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textViewFrom.text = text

        val targetImageView = viewHolder.itemView.imageViewFrom
        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

class ChatToItem(val text: String, val uri: Uri) : Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textViewTo.text = text

        val targetImageView = viewHolder.itemView.imageViewTo
        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}