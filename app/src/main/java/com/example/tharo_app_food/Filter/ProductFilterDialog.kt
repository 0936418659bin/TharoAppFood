package com.example.tharo_app_food.Filter

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.tharo_app_food.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider

class ProductFilterDialog : DialogFragment() {
    private var listener: ((minPrice: Double, maxPrice: Double, minRating: Double) -> Unit)? = null

    fun setOnFilterAppliedListener(listener: (minPrice: Double, maxPrice: Double, minRating: Double) -> Unit) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_filter_food, null)

        val sliderPrice = view.findViewById<RangeSlider>(R.id.sliderPrice)
        val sliderRating = view.findViewById<Slider>(R.id.sliderRating)

        builder.setView(view)
            .setTitle("Bộ lọc nâng cao")
            .setPositiveButton("Áp dụng") { _, _ ->
                listener?.invoke(
                    sliderPrice.values[0].toDouble(),
                    sliderPrice.values[1].toDouble(),
                    sliderRating.value.toDouble()
                )
            }
            .setNegativeButton("Hủy", null)

        return builder.create()
    }
}