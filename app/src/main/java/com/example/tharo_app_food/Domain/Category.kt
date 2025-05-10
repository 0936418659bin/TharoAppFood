package com.example.tharo_app_food.Domain

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Category(
    var Id: Int = 0,
    var ImagePath: String = "",
    var Name: String = ""
) {
    constructor() : this(0, " ","")

    override fun toString(): String {
        return Name
    }
}