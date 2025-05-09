package com.example.tharo_app_food.Handler

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.R
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class AddProductDialog : DialogFragment() {
    private var listener: ((Foods) -> Unit)? = null

    fun setOnProductAddedListener(listener: (Foods) -> Unit) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_food, null)

        builder.setView(view)
            .setTitle("Thêm món ăn mới")
            .setPositiveButton("Thêm") { _, _ ->
                val newFood = createFoodFromInput(view)
                listener?.invoke(newFood)
            }
            .setNegativeButton("Hủy", null)

        return builder.create()
    }

    private fun createFoodFromInput(view: View): Foods {
        return Foods().apply {
            Title = view.findViewById<TextInputEditText>(R.id.etFoodName).text.toString()
            Description = view.findViewById<TextInputEditText>(R.id.etDescription).text.toString()
            Price = view.findViewById<TextInputEditText>(R.id.etPrice).text.toString().toDoubleOrNull() ?: 0.0
            ImagePath = view.findViewById<TextInputEditText>(R.id.etImageUrl).text.toString()
            Star = view.findViewById<TextInputEditText>(R.id.etRating).text.toString().toDoubleOrNull() ?: 0.0
            TimeValue = view.findViewById<TextInputEditText>(R.id.etTime).text.toString().toIntOrNull() ?: 0
            BestFood = view.findViewById<MaterialCheckBox>(R.id.cbBestFood).isChecked
            CategoryId = 1 // Default category
            LocationId = 1 // Default location
        }
    }
}