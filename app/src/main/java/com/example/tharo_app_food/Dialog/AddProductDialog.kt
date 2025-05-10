package com.example.tharo_app_food.Dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddProductDialog : DialogFragment() {
    private var listener: ((Foods) -> Unit)? = null
    private lateinit var selectedImageUri: Uri
    private var selectedCategoryId: Int = 0
    private var selectedCategoryName: String = ""
    private val categories = mutableListOf<Category>()
    private lateinit var categoryDropdown: AutoCompleteTextView

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
    }

    fun setOnProductAddedListener(listener: (Foods) -> Unit) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialogTheme)
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_food, null)

        categoryDropdown = view.findViewById(R.id.spinnerCategory)
        loadCategoriesFromFirebase()
        setupImagePicker()

        builder.setView(view)
            .setTitle("Thêm món ăn mới")
            .setPositiveButton("Thêm") { _, _ ->
                if (::selectedImageUri.isInitialized && selectedCategoryId != 0) {
                    // uploadImageAndAddFood(view)
                } else {
                    val message = when {
                        !::selectedImageUri.isInitialized -> "Vui lòng chọn ảnh"
                        selectedCategoryId == 0 -> "Vui lòng chọn danh mục"
                        else -> "Vui lòng điền đầy đủ thông tin"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)

        return builder.create()
    }

    private fun setupImagePicker() {
        view?.findViewById<MaterialButton>(R.id.btn_image)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    private fun loadCategoriesFromFirebase() {
        val database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
        val categoriesRef = database.getReference("Category")

        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                // Thêm item mặc định
                categories.add(Category(0, "Chọn danh mục", ""))

                for (categorySnapshot in snapshot.children) {
                    try {
                        val id = categorySnapshot.child("Id").getValue(Int::class.java) ?: 0
                        val name = categorySnapshot.child("Name").getValue(String::class.java) ?: ""
                        val image = categorySnapshot.child("ImagePath").getValue(String::class.java) ?: ""

                        categories.add(Category(id, image, name))
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Lỗi khi tải danh mục", Toast.LENGTH_SHORT).show()
                    }
                }

                // Tạo adapter custom
                val adapter = object : ArrayAdapter<Category>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    categories
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = convertView ?: LayoutInflater.from(context)
                            .inflate(R.layout.item_category_dropdown, parent, false)
                        val category = getItem(position)
                        view.findViewById<TextView>(R.id.categoryName).text = category?.Name
                        if (position == 0) {
                            view.findViewById<TextView>(R.id.categoryName).setTextColor(
                                ContextCompat.getColor(context, R.color.hint_text_color)
                            )
                        }
                        return view
                    }

                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = convertView ?: LayoutInflater.from(context)
                            .inflate(R.layout.item_category_dropdown_expanded, parent, false)
                        val category = getItem(position)
                        view.findViewById<TextView>(R.id.categoryName).text = category?.Name
                        return view
                    }
                        override fun isEnabled(position: Int): Boolean {
                        return position != 0
                    }
                }

                categoryDropdown.setAdapter(adapter)
                categoryDropdown.setOnItemClickListener { _, _, position, _ ->
                    if (position > 0) {
                        selectedCategoryId = categories[position].Id
                        selectedCategoryName = categories[position].Name
                    } else {
                        selectedCategoryId = 0
                        selectedCategoryName = ""
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Lỗi tải danh mục: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data!!
            view?.findViewById<ImageView>(R.id.ivFoodImage)?.setImageURI(selectedImageUri)
        }
    }

    private fun createFoodFromInput(view: View, imageUrl: String): Foods {
        return Foods().apply {
            Title = view.findViewById<TextInputEditText>(R.id.etFoodName).text.toString()
            Description = view.findViewById<TextInputEditText>(R.id.etDescription).text.toString()
            Price = view.findViewById<TextInputEditText>(R.id.etPrice).text.toString().toDoubleOrNull() ?: 0.0
            ImagePath = imageUrl
            Star = view.findViewById<TextInputEditText>(R.id.etRating).text.toString().toDoubleOrNull() ?: 0.0
            TimeValue = view.findViewById<TextInputEditText>(R.id.etTime).text.toString().toIntOrNull() ?: 0
            BestFood = view.findViewById<MaterialCheckBox>(R.id.cbBestFood).isChecked
            CategoryId = selectedCategoryId
            LocationId = 1 // Default location
        }
    }
}