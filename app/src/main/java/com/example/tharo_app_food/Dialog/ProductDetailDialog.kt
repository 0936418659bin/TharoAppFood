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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.Domain.DeleteRequest
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
            .baseUrl("http://192.168.1.15:3000/")
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
        Log.d(TAG, "Nhận dữ liệu món ăn: $food")

        hienThiDuLieuMonAn()
        setupDropdowns(binding.root)

        binding.btnImage.setOnClickListener {
            Log.d(TAG, "Nhấn nút chọn ảnh")
            moImagePicker()
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialogTheme)
            .setView(binding.root)
            .setTitle("Chỉnh sửa món ăn")
            .setPositiveButton("Cập nhật", null)
            .setNegativeButton("Hủy") { _, _ ->
                Log.d(TAG, "Đã hủy hộp thoại")
                uploadJob?.cancel()
                dismiss()
            }
            .setNeutralButton("Xóa") { _, _ ->
                hienThiXacNhanXoa()
            }
            .create().also { dialog ->
                dialog.setOnShowListener {
                    val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        Log.d(TAG, "Nhấn nút cập nhật")
                        if (uploadJob?.isActive == true) {
                            Log.d(TAG, "Đang tải lên, vui lòng đợi...")
                            hienThiThongBao("Đang tải lên, vui lòng đợi...")
                            return@setOnClickListener
                        }
                        if (kiemTraDuLieuHopLe(binding.root)) {
                            Log.d(TAG, "Dữ liệu hợp lệ, bắt đầu tải lên")
                            uploadAnhVaCapNhatMonAn(binding.root)
                        } else {
                            Log.d(TAG, "Dữ liệu không hợp lệ")
                        }
                    }
                }
            }
    }

    private fun hienThiXacNhanXoa() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa món ăn ${food.Title}?")
            .setPositiveButton("Xóa") { _, _ ->
                xoaMonAn()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun xoaMonAn() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Xóa ảnh từ ImageKit trước (nếu có)
                val oldFileId = layFileIdTuUrl(food.ImagePath)
                oldFileId?.let { path ->
                    try {
                        Log.d(TAG, "Bắt đầu xóa ảnh từ ImageKit với ID: $path")
                        val deleteResponse = imageKitService.deleteImage(path)

                        if (deleteResponse.isSuccessful) {
                            deleteResponse.body()?.let {
                                if (it.success) {
                                    Log.d(TAG, "Xóa ảnh từ ImageKit thành công")
                                } else {
                                    Log.w(TAG, "ImageKit trả về lỗi: ${it.message}")
                                }
                            }
                        } else {
                            Log.w(TAG, "Lỗi khi xóa ảnh từ ImageKit: ${deleteResponse.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi gọi API xóa ảnh", e)
                        // Vẫn tiếp tục xóa dữ liệu Firebase dù xóa ảnh có lỗi
                    }
                }

                // 2. Xóa dữ liệu từ Firebase
                withContext(Dispatchers.Main) {
                    database.getReference("Foods").child(food.Key).removeValue()
                        .addOnSuccessListener {
                            hienThiThongBao("Đã xóa món ăn thành công")
                            dismiss()
                        }
                        .addOnFailureListener { e ->
                            hienThiThongBao("Lỗi khi xóa dữ liệu: ${e.message}")
                        }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hienThiThongBao("Lỗi hệ thống: ${e.message}")
                }
            }
        }
    }

    private fun hienThiDuLieuMonAn() {
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

        choPhepChinhSua()
    }

    private fun choPhepChinhSua() {
        binding.tvFoodName.isEnabled = true
        binding.tvFoodPrice.isEnabled = true
        binding.tvFoodRating.isEnabled = true
        binding.tvFoodTime.isEnabled = true
        binding.tvFoodDescription.isEnabled = true
        binding.cbBestFood.isEnabled = true
    }

    private fun setupDropdowns(view: View) {
        taiDuLieuDropdown(view.findViewById(R.id.spinnerCategory), "Category", Category::class.java) {
            selectedCategory = it
        }

        taiDuLieuDropdown(view.findViewById(R.id.spinnerPrice), "Price", Price::class.java) {
            selectedPrice = it
        }

        taiDuLieuDropdown(view.findViewById(R.id.spinnerTime), "Time", Time::class.java) {
            selectedTime = it
        }

        taiDuLieuDropdown(view.findViewById(R.id.spinnerLocation), "Location", Location::class.java) {
            selectedLocation = it
        }
    }

    private fun <T : Any> taiDuLieuDropdown(
        dropdown: AutoCompleteTextView,
        path: String,
        clazz: Class<T>,
        onSelect: (T) -> Unit
    ) {
        Log.d(TAG, "Đang tải dữ liệu dropdown cho $path")
        database.getReference(path).get().addOnSuccessListener { snapshot ->
            val items = snapshot.children.mapNotNull { it.getValue(clazz) }
            Log.d(TAG, "Đã tải $path items: ${items.size}")

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

            dropdown.setAdapter(taoDropdownAdapter(items))

            currentItem?.let {
                dropdown.setText(hienThiTextItem(it), false)
                onSelect(it)
                Log.d(TAG, "Thiết lập lựa chọn mặc định cho $path: ${hienThiTextItem(it)}")
            }

            dropdown.setOnItemClickListener { _, _, position, _ ->
                onSelect(items[position])
                Log.d(TAG, "Đã chọn $path: ${hienThiTextItem(items[position])}")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Lỗi khi tải $path", it)
            hienThiThongBao("Lỗi tải dữ liệu $path")
        }
    }

    private fun <T> taoDropdownAdapter(items: List<T>): ArrayAdapter<T> {
        return object : ArrayAdapter<T>(requireContext(), R.layout.item_category_dropdown, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_category_dropdown, parent, false)
                view.findViewById<TextView>(R.id.categoryName).text = hienThiTextItem(getItem(position))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_category_dropdown_expanded, parent, false)
                view.findViewById<TextView>(R.id.categoryName).text = hienThiTextItem(getItem(position))
                return view
            }
        }
    }

    private fun <T> hienThiTextItem(item: T?): String = when (item) {
        is Category -> item.Name
        is Price -> item.Value
        is Time -> item.Value
        is Location -> item.loc
        else -> ""
    }

    private fun moImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            Log.d(TAG, "Đã chọn ảnh: $imageUri")
            binding.ivFoodImage3.setImageURI(imageUri)
        } else {
            Log.d(TAG, "Hủy chọn ảnh hoặc có lỗi")
        }
    }

    private fun kiemTraDuLieuHopLe(view: View): Boolean {
        if (binding.tvFoodName.text.toString().trim().isEmpty()) {
            hienThiThongBao("Vui lòng nhập tên món ăn")
            return false
        }
        if (binding.tvFoodPrice.text.toString().trim().isEmpty()) {
            hienThiThongBao("Vui lòng nhập giá món ăn")
            return false
        }
        if (selectedCategory == null || selectedPrice == null || selectedTime == null || selectedLocation == null) {
            hienThiThongBao("Vui lòng chọn đầy đủ thông tin từ các danh mục")
            return false
        }
        return true
    }

    private fun uploadAnhVaCapNhatMonAn(view: View) {
        Log.d(TAG, "Bắt đầu uploadAnhVaCapNhatMonAn()")
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

                imageUri?.let { uri ->
                    val oldFileId = layFileIdTuUrl(food.ImagePath)
                    Log.d(TAG, "Đường dẫn ảnh cũ cần xóa: $oldFileId")

                    Log.d(TAG, "Đang tải ảnh lên từ URI: $uri")
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
                        val response = imageKitService.uploadImage(filePart, fileNamePart, folderPart)
                        Log.d(TAG, "Tải ảnh lên thành công: ${response.url}")
                        updatedFood.ImagePath = response.url

                        oldFileId?.let { path ->
                            try {
                                Log.d(TAG, "Đang thử xóa ảnh cũ với fileid: $path")
                                val deleteResponse = imageKitService.deleteImage(path)
                                if (deleteResponse.isSuccessful) {
                                    deleteResponse.body()?.let {
                                        if (it.success) {
                                            Log.d(TAG, "Đã xóa ảnh cũ thành công")
                                        } else {
                                            Log.w(TAG, "Không thể xóa ảnh cũ: ${it.message ?: "Không có thông báo lỗi"}")
                                        }
                                    }
                                } else {
                                    val errorBody = deleteResponse.errorBody()?.string()
                                    Log.w(TAG, "Lỗi API xóa: ${errorBody ?: "Lỗi không xác định"}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Lỗi khi xóa ảnh cũ", e)
                                withContext(Dispatchers.Main) {
                                    hienThiThongBao("Cảnh báo: Không thể xóa ảnh cũ nhưng ảnh mới đã được cập nhật")
                                }
                            }
                        }
                    } catch (uploadEx: Exception) {
                        Log.e(TAG, "Tải ảnh lên thất bại", uploadEx)
                        throw uploadEx
                    } finally {
                        tempFile.delete()
                        Log.d(TAG, "Đã xóa file tạm")
                    }
                }

                withContext(Dispatchers.Main) {
                    databaseRef.setValue(updatedFood).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Đã cập nhật món ăn trong database thành công")
                            hienThiThongBao("Cập nhật món ăn thành công")
                            dismiss()
                        } else {
                            Log.e(TAG, "Cập nhật database thất bại", task.exception)
                            hienThiThongBao("Cập nhật thất bại: ${task.exception?.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi trong uploadAnhVaCapNhatMonAn()", e)
                withContext(Dispatchers.Main) {
                    hienThiThongBao("Lỗi: ${e.message}")
                }
            }
        }
    }

    private fun layFileIdTuUrl(imageUrl: String?): String? {
        if (imageUrl.isNullOrEmpty()) return null
        try {
            val uri = Uri.parse(imageUrl)
            val pathSegments = uri.pathSegments
            if (pathSegments.size >= 2) {
                val fileName = pathSegments.last()
                return fileName.substringBeforeLast('.')
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích URL ảnh", e)
            return null
        }
    }

    private fun hienThiThongBao(message: String) {
        if (isAdded) { // Kiểm tra Fragment còn gắn với Activity không
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}