package com.example.tharo_app_food.Domain

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Category(
    @PropertyName("Id") var Id: Int,
    @PropertyName("Name") var Name: String,
    @PropertyName("ImagePath") var ImagePath: String,
    @PropertyName("ImageId") var ImageId: String
) {
    constructor() : this(0, "", "", "")

    override fun toString(): String {
        return Name
    }

    fun generateKey(): String {
        return Name.replace("[^a-zA-Z0-9]".toRegex(), "_")
    }
}