package com.example.tharo_app_food.Domain

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Foods(
    var CategoryId: Int = 0,
    var Description: String = "",
    var BestFood: Boolean = false,
    var Id: Int = 0,
    var LocationId: Int = 0,
    var Price: Double = 0.0,
    var ImagePath: String = "",
    var PriceId: Int = 0,
    var Star: Double = 0.0,
    var TimeId: Int = 0,
    var TimeValue: Int = 0,
    var Title: String = "",
    var numberInChart: Int = 0,
    var Key: String = "",
    var ImageId: String = "",

) : Serializable {
    // Constructor không tham số để Firebase có thể deserialize object
    constructor() : this(0, "", false, 0, 0, 0.0, "", 0, 0.0, 0, 0, "", 0, "","")

    override fun toString(): String {
        return Title.ifEmpty { "Unknown Location" }
    }

    fun generateKey(): String {
        return Title.replace("[^a-zA-Z0-9]".toRegex(), "_")
    }

}
