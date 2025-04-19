package com.example.tharo_app_food.Domain

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Location(
    var Id: Int = 0,
    var loc: String = ""
)
{
    override fun toString(): String {
        return loc ?: "Unknown Location"
    }

}
