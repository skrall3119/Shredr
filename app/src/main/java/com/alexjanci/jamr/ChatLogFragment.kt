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
    val uid get() = auth.uid.orEmpty()

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

        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title =
            arguments?.getString("name") ?: "Conversation"
        val ownId = auth.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(ownId).get()
            .addOnCompleteListener { task ->
                if (view != null && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                    val photo = if (task.isSuccessful) task.result?.getString("pic") else null
                    listenForMessages(photo?.takeIf { it.isNotBlank() }
                        ?: "android.resource://com.alexjanci.jamr/drawable/defaultpic")
                }
            }

        send_button.setOnClickListener {
            performSendMessage()
        }
    }

    var messageListener: ListenerRegistration? = null

    private fun listenForMessages(fromUri: String) {
        val fromId = auth.uid
        val toId: String? = arguments?.getString("id")

        if (fromId == null || toId.isNullOrBlank()) return

        val ref = FirebaseFirestore.getInstance().collection("user-messages").document("$fromId")
            .collection("$toId")
        messageListener = ref.addSnapshotListener { value, error ->
            if (view == null) return@addSnapshotListener
            if (error != null || value == null) {
                android.widget.Toast.makeText(context, "Unable to load messages. Please reopen this conversation.", android.widget.Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
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
        val draft = messageText.text.toString()
        val text = draft.trim()
        if (text.isBlank() || auth.uid == null) return

        val fromId = uid
        val toId = arguments?.getString("id")?.takeIf { it.isNotBlank() } ?: return

        val reference =
            FirebaseFirestore.getInstance().collection("user-messages").document("$fromId")
                .collection("$toId").document()

        val toReference =
            FirebaseFirestore.getInstance().collection("user-messages").document("$toId")
                .collection(
                    "$fromId"
                ).document(reference.id)
        val chatMessage =
            ChatMessage(reference.id, text, fromId, toId!!, System.currentTimeMillis() / 1000)
        send_button.isEnabled = false
        FirebaseFirestore.getInstance().batch().set(reference, chatMessage)
            .set(toReference, chatMessage).commit().addOnCompleteListener { task ->
                if (view == null) return@addOnCompleteListener
                send_button.isEnabled = true
                if (task.isSuccessful) {
                    if (messageText.text.toString() == draft) messageText.setText("")
                    if (adapter.itemCount > 0) recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                } else {
                    android.widget.Toast.makeText(context, "Message not sent. Your draft is still here; please try again.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
    }

    companion object {
        fun newInstance(): ChatLogFragment = ChatLogFragment()
    }
}

class ChatFromItem(val text: String, val uri: Uri) : Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textViewFrom.text = text

        val targetImageView = viewHolder.itemView.imageViewFrom
        Picasso.get().load(uri).placeholder(R.drawable.defaultpic).error(R.drawable.defaultpic).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

class ChatToItem(val text: String, val uri: Uri) : Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textViewTo.text = text

        val targetImageView = viewHolder.itemView.imageViewTo
        Picasso.get().load(uri).placeholder(R.drawable.defaultpic).error(R.drawable.defaultpic).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}
