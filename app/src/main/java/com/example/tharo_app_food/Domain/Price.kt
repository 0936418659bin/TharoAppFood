package com.example.tharo_app_food.Domain

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Price (
    var Id: Int = 0,
    var Value: String = ""
)
{
    override fun toString(): String {
        return Value ?: "Unknown Price"
    }
}