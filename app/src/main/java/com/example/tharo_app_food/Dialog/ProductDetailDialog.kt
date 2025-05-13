package com.example.tharo_app_food.Dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.Domain.ImageKitService
import com.example.tharo_app_food.Domain.Location
import com.example.tharo_app_food.Domain.Price
import com.example.tharo_app_food.Domain.Time
import com.example.tharo_app_food.R
import com.example.tharo_app_food.databinding.DialogFoodDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class ProductDetailDialog : DialogFragment() {
    private lateinit var binding: DialogFoodDetailBinding
    private lateinit var food: Foods
    private var uploadJob: Job? = null
    private var imageUri: Uri? = null

    private var selectedCategory: Category? = null
    private var selectedPrice: Price? = null
    private var selectedTime: Time? = null
    private var selectedLocation: Location? = null

    private val database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")

    companion object {
        private const val ARG_FOOD = "food"
        private const val TAG = "ProductDetailDialog"
        private const val REQUEST_IMAGE_PICK = 100

        fun newInstance(food: Foods): ProductDetailDialog {
            val args = Bundle()
            args.putSerializable(ARG_FOOD, food)
            val fragment = ProductDetailDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.6:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val imageKitService by lazy {
        retrofit.create(ImageKitService::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog called")
        val inflater = requireActivity().layoutInflater
        binding = DialogFoodDetailBinding.inflate(inflater)
        food = arguments?.getSerializable(ARG_FOOD) as Foods
        Log.d(TAG, "Received food object: $food")

        displayFoodData()
        setupDropdowns(binding.root)

        binding.btnImage.setOnClickListener {
            Log.d(TAG, "Image picker button clicked")
            openImagePicker()
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialogTheme)
            .setView(binding.root)
            .setTitle("Chỉnh sửa món ăn")
            .setPositiveButton("Cập nhật", null)
            .setNegativeButton("Hủy") { _, _ ->
                Log.d(TAG, "Dialog canceled")
                uploadJob?.cancel()
                dismiss()
            }
            .create().also { dialog ->
                dialog.setOnShowListener {
                    val button = dialog.getButton(Dialog.BUTTON_POSITIVE)
                    button.setOnClickListener {
                        Log.d(TAG, "Update button clicked")
                        if (uploadJob?.isActive == true) {
                            Log.d(TAG, "Upload is already in progress")
                            showToast("Đang tải lên, vui lòng đợi...")
                            return@setOnClickListener
                        }
                        if (validateInputs(binding.root)) {
                            Log.d(TAG, "Inputs are valid, starting upload")
                            uploadImageAndAddProduct(binding.root)
                        } else {
                            Log.d(TAG, "Validation failed")
                        }
                    }
                }
            }
    }

    private fun displayFoodData() {
        Glide.with(requireContext())
            .load(food.ImagePath)
            .placeholder(R.drawable.add_photo)
            .into(binding.ivFoodImage3)

        binding.tvFoodName.setText(food.Title)
        binding.tvFoodPrice.setText(food.Price.toString())
        binding.tvFoodRating.setText(food.Star.toString())
        binding.tvFoodTime.setText(food.TimeValue.toString())
        binding.tvFoodDescription.setText(food.Description)
        binding.cbBestFood.isChecked = food.BestFood

        enableEditing()
    }

    private fun enableEditing() {
        binding.tvFoodName.isEnabled = true
        binding.tvFoodPrice.isEnabled = true
        binding.tvFoodRating.isEnabled = true
        binding.tvFoodTime.isEnabled = true
        binding.tvFoodDescription.isEnabled = true
        binding.cbBestFood.isEnabled = true
    }

    private fun setupDropdowns(view: View) {
        loadDropdownData(view.findViewById(R.id.spinnerCategory), "Category", Category::class.java) {
            selectedCategory = it
        }

        loadDropdownData(view.findViewById(R.id.spinnerPrice), "Price", Price::class.java) {
            selectedPrice = it
        }

        loadDropdownData(view.findViewById(R.id.spinnerTime), "Time", Time::class.java) {
            selectedTime = it
        }

        loadDropdownData(view.findViewById(R.id.spinnerLocation), "Location", Location::class.java) {
            selectedLocation = it
        }
    }

    private fun <T : Any> loadDropdownData(
        dropdown: AutoCompleteTextView,
        path: String,
        clazz: Class<T>,
        onSelect: (T) -> Unit
    ) {
        Log.d(TAG, "Loading dropdown data for $path")
        database.getReference(path).get().addOnSuccessListener { snapshot ->
            val items = snapshot.children.mapNotNull { it.getValue(clazz) }
            Log.d(TAG, "Loaded $path items: ${items.size}")

            val currentId = when (path) {
                "Category" -> food.CategoryId
                "Price" -> food.PriceId
                "Time" -> food.TimeId
                "Location" -> food.LocationId
                else -> 0
            }

            val currentItem = items.firstOrNull {
                when (it) {
                    is Category -> it.Id == currentId
                    is Price -> it.Id == currentId
                    is Time -> it.Id == currentId
                    is Location -> it.Id == currentId
                    else -> false
                }
            }

            dropdown.setAdapter(createDropdownAdapter(items))

            currentItem?.let {
                dropdown.setText(getItemDisplayText(it), false)
                onSelect(it)
                Log.d(TAG, "Set default selection for $path: ${getItemDisplayText(it)}")
            }

            dropdown.setOnItemClickListener { _, _, position, _ ->
                onSelect(items[position])
                Log.d(TAG, "Selected $path: ${getItemDisplayText(items[position])}")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Error loading $path", it)
            showToast("Lỗi tải dữ liệu $path")
        }
    }

    private fun <T> createDropdownAdapter(items: List<T>): ArrayAdapter<T> {
        return object : ArrayAdapter<T>(requireContext(), R.layout.item_category_dropdown, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_category_dropdown, parent, false)
                view.findViewById<TextView>(R.id.categoryName).text = getItemDisplayText(getItem(position))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_category_dropdown_expanded, parent, false)
                view.findViewById<TextView>(R.id.categoryName).text = getItemDisplayText(getItem(position))
                return view
            }
        }
    }

    private fun <T> getItemDisplayText(item: T?): String = when (item) {
        is Category -> item.Name
        is Price -> item.Value
        is Time -> item.Value
        is Location -> item.loc
        else -> ""
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            Log.d(TAG, "Image selected: $imageUri")
            binding.ivFoodImage3.setImageURI(imageUri)
        } else {
            Log.d(TAG, "Image selection canceled or failed")
        }
    }

    private fun validateInputs(view: View): Boolean {
        if (binding.tvFoodName.text.toString().trim().isEmpty()) {
            showToast("Vui lòng nhập tên món ăn")
            return false
        }
        if (binding.tvFoodPrice.text.toString().trim().isEmpty()) {
            showToast("Vui lòng nhập giá món ăn")
            return false
        }
        if (selectedCategory == null || selectedPrice == null || selectedTime == null || selectedLocation == null) {
            showToast("Vui lòng chọn đầy đủ thông tin từ các danh mục")
            return false
        }
        return true
    }

    private fun uploadImageAndAddProduct(view: View) {
        Log.d(TAG, "Starting uploadImageAndAddProduct()")
        val databaseRef = database.getReference("Foods").child(food.Key)

        uploadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val updatedFood = food.copy(
                    Title = binding.tvFoodName.text.toString(),
                    Price = binding.tvFoodPrice.text.toString().toDouble(),
                    Star = binding.tvFoodRating.text.toString().toDoubleOrNull() ?: 0.0,
                    TimeValue = binding.tvFoodTime.text.toString().toInt(),
                    Description = binding.tvFoodDescription.text.toString(),
                    BestFood = binding.cbBestFood.isChecked,
                    CategoryId = selectedCategory?.Id ?: 0,
                    PriceId = selectedPrice?.Id ?: 0,
                    TimeId = selectedTime?.Id ?: 0,
                    LocationId = selectedLocation?.Id ?: 0
                )

                // Nếu có ảnh mới, xử lý upload và xóa ảnh cũ
                imageUri?.let { uri ->
                    // Lấy filePath từ URL ảnh cũ (nếu có)
                    val oldFilePath = extractFilePathFromUrl(food.ImagePath)
                    Log.d(TAG, "Old file path to delete: $oldFilePath")

                    // Upload ảnh mới
                    Log.d(TAG, "Uploading image from URI: $uri")
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                        ?: throw Exception("Không thể mở ảnh")

                    val tempFile = File.createTempFile("food_${System.currentTimeMillis()}", ".jpg", requireContext().cacheDir).apply {
                        outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                    }

                    val fileRequestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val filePart = MultipartBody.Part.createFormData("file", tempFile.name, fileRequestBody)

                    val fileNamePart = "food_${System.currentTimeMillis()}.jpg".toRequestBody("text/plain".toMediaType())
                    val folderPart = "/food_images/".toRequestBody("text/plain".toMediaType())

                    try {
                        // Upload ảnh mới
                        val response = imageKitService.uploadImage(filePart, fileNamePart, folderPart)
                        Log.d(TAG, "Image uploaded successfully: ${response.url}")
                        updatedFood.ImagePath = response.url

                        // Xóa ảnh cũ nếu có
                        oldFilePath?.let { path ->
                            try {
                                Log.d(TAG, "Attempting to delete old image with path: $path")
                                val deleteResponse = imageKitService.deleteImage(path)
                                if (deleteResponse.success) {
                                    Log.d(TAG, "Old image deleted successfully: $path")
                                } else {
                                    Log.w(TAG, "Failed to delete old image: $path")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting old image", e)
                                withContext(Dispatchers.Main) {
                                    showToast("Lỗi khi xóa ảnh cũ, nhưng ảnh mới đã được cập nhật")
                                }
                            }
                        }
                    } catch (uploadEx: Exception) {
                        Log.e(TAG, "Image upload failed", uploadEx)
                        throw uploadEx
                    } finally {
                        tempFile.delete()
                        Log.d(TAG, "Temp file deleted")
                    }
                }

                withContext(Dispatchers.Main) {
                    databaseRef.setValue(updatedFood).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Food updated in database successfully")
                            showToast("Cập nhật món ăn thành công")
                            dismiss()
                        } else {
                            Log.e(TAG, "Database update failed", task.exception)
                            showToast("Cập nhật thất bại: ${task.exception?.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during uploadImageAndAddProduct()", e)
                withContext(Dispatchers.Main) {
                    showToast("Lỗi: ${e.message}")
                }
            }
        }
    }

    private fun extractFilePathFromUrl(imageUrl: String?): String? {
        if (imageUrl.isNullOrEmpty()) return null

        // URL ImageKit có dạng: https://ik.imagekit.io/your_imagekit_id/rest_of_path.jpg
        // Hoặc: https://ik.imagekit.io/your_imagekit_id/rest_of_path.jpg?tr=params...

        try {
            val uri = Uri.parse(imageUrl)
            val pathSegments = uri.pathSegments

            if (pathSegments.size < 2) return null

            // Bỏ phần imagekit_id (phần đầu tiên)
            val filePath = pathSegments.drop(1).joinToString("/")

            // Loại bỏ query parameters nếu có
            return filePath.substringBefore("?")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing image URL", e)
            return null
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}