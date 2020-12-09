package com.alexjanci.jamr

import android.net.Uri

data class User(
    val name: String = "name",
    val city: String = "city",
    val age: String = "age",
    val bio: String = "bio",
    val uid: String = "id",
    val pic: Uri
)