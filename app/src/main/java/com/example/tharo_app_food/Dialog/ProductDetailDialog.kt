package com.example.tharo_app_food.Dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProductDetailDialog(private val food: Foods) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_food_detail, null)

        view.findViewById<TextView>(R.id.tvFoodName).text = food.Title
        view.findViewById<TextView>(R.id.tvFoodPrice).text = "$${food.Price}"
        view.findViewById<TextView>(R.id.tvFoodDescription).text = food.Description
        view.findViewById<TextView>(R.id.tvFoodRating).text = "Đánh giá: ${food.Star} ⭐"
        view.findViewById<TextView>(R.id.tvFoodTime).text = "Thời gian: ${food.TimeValue} phút"

        Glide.with(this)
            .load(food.ImagePath)
            .into(view.findViewById(R.id.ivFoodImage))

        builder.setView(view)
            .setTitle("Chi tiết món ăn")
            .setPositiveButton("Đóng", null)

        return builder.create()
    }
}