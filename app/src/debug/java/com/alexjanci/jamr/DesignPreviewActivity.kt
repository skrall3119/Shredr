package com.alexjanci.jamr

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Debug-only layout inspection. No authentication, network calls, or persisted data. */
class DesignPreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "signin"
        val layout = when (screen) {
            "signup" -> R.layout.fragment_sign_up
            "profile" -> R.layout.fragment_profile
            "chat" -> R.layout.fragment_chat_log
            "messages" -> R.layout.fragment_messages
            else -> R.layout.fragment_sign_in
        }
        setContentView(layout)
        if (screen == "profile") {
            findViewById<TextView>(R.id.profileName).text = "Sample profile"
            findViewById<TextView>(R.id.profileCity).text = "Design preview"
            findViewById<TextView>(R.id.profileEmail).text = "preview@example.com"
        }
    }
}
