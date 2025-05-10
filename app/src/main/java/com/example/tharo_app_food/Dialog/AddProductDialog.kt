package com.example.tharo_app_food.Dialog

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.Domain.ImageKitService
import com.example.tharo_app_food.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class AddProductDialog : DialogFragment() {
    private var listener: ((Foods) -> Unit)? = null
    private lateinit var selectedImageUri: Uri
    private var selectedCategoryId: Int = 0
    private var selectedCategoryName: String = ""
    private val categories = mutableListOf<Category>()
    private lateinit var categoryDropdown: AutoCompleteTextView
    private var uploadJob: Job? = null

    // ImageKit service
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.56.1:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val imageKitService by lazy {
        retrofit.create(ImageKitService::class.java)
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
        private const val TAG = "AddProductDialog"
        private const val REQUEST_READ_STORAGE_PERMISSION = 101
    }

    fun setOnProductAddedListener(listener: (Foods) -> Unit) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog: Creating dialog")
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialogTheme)
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_food, null)

        categoryDropdown = view.findViewById(R.id.spinnerCategory)
        Log.d(TAG, "Loading categories from Firebase")
        loadCategoriesFromFirebase()
        setupImagePicker(view)

        builder.setView(view)
            .setTitle("Thêm món ăn mới")
            .setPositiveButton("Thêm") { _, _ ->
                if (validateInputs(view)) {
                    uploadImageAndAddProduct(view)
                }
            }
            .setNegativeButton("Hủy") { _, _ ->
                uploadJob?.cancel()
            }

        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uploadJob?.cancel()
    }

    private fun validateInputs(view: View): Boolean {
        if (!isAdded) return false

        return when {
            !::selectedImageUri.isInitialized -> {
                showToast("Vui lòng chọn ảnh")
                false
            }
            selectedCategoryId == 0 -> {
                showToast("Vui lòng chọn danh mục")
                false
            }
            view.findViewById<TextInputEditText>(R.id.etFoodName).text.isNullOrEmpty() -> {
                showToast("Vui lòng nhập tên món ăn")
                false
            }
            view.findViewById<TextInputEditText>(R.id.etPrice).text.isNullOrEmpty() -> {
                showToast("Vui lòng nhập giá")
                false
            }
            else -> true
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupImagePicker(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_image).setOnClickListener {
            if (isAdded && ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else if (isAdded) {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_READ_STORAGE_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_READ_STORAGE_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            isAdded
        ) {
            openImagePicker()
        } else if (isAdded) {
            showToast("Cần cấp quyền truy cập để chọn ảnh")
        }
    }

    private fun openImagePicker() {
        try {
            if (!isAdded) return
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening image picker", e)
            if (isAdded) {
                showToast("Không thể mở thư viện ảnh")
            }
        }
    }

    private fun loadCategoriesFromFirebase() {
        if (!isAdded) return

        val database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
        val categoriesRef = database.getReference("Category")

        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                categories.clear()
                categories.add(Category(0, "Chọn danh mục", ""))

                for (categorySnapshot in snapshot.children) {
                    try {
                        val id = categorySnapshot.child("Id").getValue(Int::class.java) ?: 0
                        val name = categorySnapshot.child("Name").getValue(String::class.java) ?: ""
                        val image = categorySnapshot.child("ImagePath").getValue(String::class.java) ?: ""

                        categories.add(Category(id, image, name))
                    } catch (e: Exception) {
                        if (isAdded) {
                            showToast("Lỗi khi tải danh mục")
                        }
                    }
                }

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
                if (isAdded) {
                    showToast("Lỗi tải danh mục: ${error.message}")
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isAdded) return

        if (requestCode == PICK_IMAGE_REQUEST) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.data?.let { uri ->
                        try {
                            selectedImageUri = Uri.parse(uri.toString())
                            displaySelectedImage(selectedImageUri)
                            view?.findViewById<MaterialButton>(R.id.btn_image)?.visibility = View.GONE
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing image URI", e)
                            showErrorToast("Lỗi xử lý ảnh: ${e.localizedMessage}")
                        }
                    } ?: run {
                        showErrorToast("Không thể lấy ảnh đã chọn")
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "Image selection cancelled by user")
                }
            }
        }
    }

    private fun displaySelectedImage(uri: Uri) {
        if (!isAdded) return

        try {
            dialog?.window?.decorView?.let { dialogView ->
                dialogView.findViewById<ImageView>(R.id.ivFoodImage3)?.let { imageView ->
                    Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .into(imageView)

                    dialogView.findViewById<MaterialButton>(R.id.btn_image)?.visibility = View.GONE
                } ?: run {
                    showErrorToast("Lỗi hệ thống: Không tìm thấy view hiển thị ảnh")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying image", e)
            showErrorToast("Lỗi hiển thị ảnh: ${e.localizedMessage}")
        }
    }

    private fun showErrorToast(message: String) {
        if (isAdded) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadImageAndAddProduct(view: View) {
        if (!isAdded) return

        uploadJob = lifecycleScope.launch {
            try {
                val context = requireContext().applicationContext
                showToast("Đang tải lên...")

                val imageUrl = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(selectedImageUri)?.use { inputStream ->
                        val tempFile = File.createTempFile(
                            "food_${System.currentTimeMillis()}",
                            ".jpg",
                            context.cacheDir
                        ).apply {
                            outputStream().use { output -> inputStream.copyTo(output) }
                        }

                        try {
                            val filePart = MultipartBody.Part.createFormData(
                                "file",
                                tempFile.name,
                                tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                            )

                            val fileNamePart = "food_${System.currentTimeMillis()}.jpg"
                                .toRequestBody("text/plain".toMediaType())
                            val folderPart = "/food_images/"
                                .toRequestBody("text/plain".toMediaType())

                            imageKitService.uploadImage(filePart, fileNamePart, folderPart).url
                        } finally {
                            tempFile.delete()
                        }
                    } ?: throw IllegalStateException("Không thể đọc ảnh")
                }

                val newFood = createFoodFromInput(view, imageUrl)

                withContext(Dispatchers.IO) {
                    saveFoodToFirebase(newFood)
                }

                if (isAdded) {
                    withContext(Dispatchers.Main) {
                        showToast("Thêm món ăn thành công")
                        listener?.invoke(newFood)
                        dismiss()
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    val errorMessage = when (e) {
                        is HttpException -> {
                            val errorBody = e.response()?.errorBody()?.string()
                            "Lỗi server: ${e.code()}, $errorBody"
                        }
                        else -> "Lỗi: ${e.message ?: "Không xác định"}"
                    }

                    withContext(Dispatchers.Main) {
                        showToast(errorMessage)
                    }
                }
            }
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
            LocationId = 1
            PriceId = 0
            TimeId = 0
            numberInChart = 0
            Key = "${Title}_${System.currentTimeMillis()}"
        }
    }

    private fun saveFoodToFirebase(food: Foods) {
        val database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
        val foodsRef = database.getReference("Foods")

        val newFoodRef = foodsRef.push()
        food.Id = newFoodRef.key?.toIntOrNull() ?: System.currentTimeMillis().toInt()

        newFoodRef.setValue(food)
            .addOnSuccessListener {
                Log.d(TAG, "Food saved to Firebase: ${food.Id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save food to Firebase", e)
            }
    }
}