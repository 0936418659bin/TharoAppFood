package com.example.tharo_app_food.Domain

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import java.io.Serializable

@IgnoreExtraProperties
data class Foods(
    @PropertyName("Id") var Id: Int = 0,
    @PropertyName("Title") var Title: String = "",
    @PropertyName("Description") var Description: String = "",
    @PropertyName("Price") var Price: Double = 0.0,
    @PropertyName("ImagePath") var ImagePath: String = "",
    @PropertyName("ImageId") var ImageId: String = "",
    @PropertyName("Star") var Star: Double = 0.0,
    @PropertyName("TimeValue") var TimeValue: Int = 0,
    @PropertyName("BestFood") var BestFood: Boolean = false,
    @PropertyName("CategoryId") var CategoryId: Int = 0,
    @PropertyName("LocationId") var LocationId: Int = 0,
    @PropertyName("PriceId") var PriceId: Int = 0,
    @PropertyName("TimeId") var TimeId: Int = 0,
    @PropertyName("Key") var Key: String = "",
    var numberInChart: Int = 0,
) : Serializable {
    // Constructor không tham số để Firebase có thể deserialize object
    constructor() : this(0, "", "", 0.0, "", "", 0.0, 0, false, 0, 0, 0, 0, "", 0)


    override fun toString(): String {
        return Title.ifEmpty { "Unknown Location" }
    }

    fun generateKey(): String {
        return Title.replace("[^a-zA-Z0-9]".toRegex(), "_")
    }

}
