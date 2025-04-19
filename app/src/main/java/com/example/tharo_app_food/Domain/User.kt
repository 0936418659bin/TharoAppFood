package com.example.tharo_app_food.Domain

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var Id: Int = 0,
    var UserName: String = "",
    var Email: String = "",
    var Role: String = "User",
    var Avatar: String = "",
     // Thay bằng List
) {
    constructor() : this(0, "", "", "User", "")
}