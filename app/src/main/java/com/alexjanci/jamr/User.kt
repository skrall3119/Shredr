package com.alexjanci.jamr

import android.net.Uri

data class User(
    val fname: String = "name",
    val city: String = "city",
    val age: String = "age",
    val bio: String = "bio",
    val id: String = "id",
    val pic: String = "android.resource://com.alexjanci.jamr/drawable/defaultpic"
)