package com.example.tharo_app_food.Dialog

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
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
import com.example.tharo_app_food.Domain.*
import com.example.tharo_app_food.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File



class AddProductDialog : DialogFragment() {
    private var listener: ((Foods) -> Unit)? = null
    private lateinit var selectedImageUri: Uri
    private var selectedCategory = Category(0, "", "")
    private var selectedPrice = Price(0, "")
    private var selectedTime = Time(0, "")
    private var selectedLocation = Location(0, "")
    private var progressDialog: ProgressDialog? = null
    private var uploadJob: Job? = null

    private val TAG = "AddProductDialog"

    private val database by lazy {
        FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
    }

    private suspend fun getNextFoodId(): Int = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.getReference("Foods").get().await()
            if (!snapshot.exists()) {
                Log.d(TAG, "Không có sản phẩm nào, bắt đầu với ID 1")
                return@withContext 1
            }

            val numericKeys = snapshot.children.mapNotNull { it.key?.toIntOrNull() }
            val nextId = if (numericKeys.isNotEmpty()) numericKeys.maxOrNull()!! + 1 else 1

            Log.d(TAG, "ID tiếp theo sẽ là: $nextId")
            return@withContext nextId
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy ID tiếp theo", e)
            return@withContext 1
        }
    }

    private val imageKitService by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.4:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImageKitService::class.java)
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
        private const val REQUEST_READ_STORAGE_PERMISSION = 101
    }

    fun setOnProductAddedListener(listener: (Foods) -> Unit) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_food, null)

        setupDropdowns(view)
        setupImagePicker(view)
        setupDuplicateCleaner()

        return MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialogTheme)
            .setView(view)
            .setTitle("Thêm món ăn mới")
            .setPositiveButton("Thêm") { _, _ ->
                if (uploadJob?.isActive == true) {
                    showToast("Đang tải lên, vui lòng đợi...")
                    return@setPositiveButton
                }
                if (validateInputs(view)) {
                    uploadImageAndAddProduct(view)
                }
            }
            .setNegativeButton("Hủy") { _, _ -> uploadJob?.cancel() }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog?.dismiss()
        Log.e("AddProductDialog", "onDestroyView được gọi, nhưng không cancel uploadJob nữa")
    }

    private fun setupDropdowns(view: View) {
        loadDropdownData(
            view.findViewById(R.id.spinnerCategory),
            "Category",
            Category::class.java
        ) { selectedCategory = it }

        loadDropdownData(
            view.findViewById(R.id.spinnerPrice),
            "Price",
            Price::class.java
        ) { selectedPrice = it }

        loadDropdownData(
            view.findViewById(R.id.spinnerTime),
            "Time",
            Time::class.java
        ) { selectedTime = it }

        loadDropdownData(
            view.findViewById(R.id.spinnerLocation),
            "Location",
            Location::class.java
        ) { selectedLocation = it }
    }

    private fun <T : Any> loadDropdownData(
        dropdown: AutoCompleteTextView,
        path: String,
        clazz: Class<T>,
        onSelect: (T) -> Unit
    ) {
        database.getReference(path).get().addOnSuccessListener { snapshot ->
            val items = snapshot.children.mapNotNull { it.getValue(clazz) }
            Log.d(TAG, "Loaded $path items: ${items.size}")
            dropdown.setAdapter(createDropdownAdapter(items))
            dropdown.setOnItemClickListener { _, _, position, _ ->
                onSelect(items[position])
                Log.d(TAG, "Selected $path: ${items[position]}")
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

    private fun setupImagePicker(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_image).setOnClickListener {
            if (hasStoragePermission()) openImagePicker() else requestStoragePermission()
        }
    }

    private fun hasStoragePermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestStoragePermission() {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_STORAGE_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_READ_STORAGE_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            showToast("Cần cấp quyền truy cập để chọn ảnh")
        }
    }

    private fun openImagePicker() {
        Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            startActivityForResult(this, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                Log.d(TAG, "Image picked: $selectedImageUri")
                displaySelectedImage(uri)
                view?.findViewById<MaterialButton>(R.id.btn_image)?.visibility = View.GONE
            } ?: showToast("Không thể lấy ảnh đã chọn")
        }
    }

    private fun displaySelectedImage(uri: Uri) {
        dialog?.findViewById<ImageView>(R.id.ivFoodImage3)?.let {
            Glide.with(this).load(uri).centerCrop().into(it)
        }
    }

    private fun validateInputs(view: View): Boolean {
        val nameInput = view.findViewById<TextInputEditText>(R.id.etFoodName).text?.toString()?.trim()
        val priceInput = view.findViewById<TextInputEditText>(R.id.etPrice).text?.toString()?.trim()

        return when {
            !::selectedImageUri.isInitialized -> {
                Log.e(TAG, "Chưa chọn ảnh")
                showToast("Vui lòng chọn ảnh")
                false
            }
            selectedCategory.Id == -1 -> { // Giả định -1 là "chưa chọn"
                Log.e(TAG, "Danh mục chưa được chọn")
                showToast("Vui lòng chọn danh mục")
                false
            }
            nameInput.isNullOrEmpty() -> {
                Log.e(TAG, "Tên món ăn chưa được nhập")
                showToast("Vui lòng nhập tên món ăn")
                false
            }
            priceInput.isNullOrEmpty() -> {
                Log.e(TAG, "Giá món ăn chưa được nhập")
                showToast("Vui lòng nhập giá")
                false
            }
            else -> true
        }
    }


    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
    private fun uploadImageAndAddProduct(view: View) {
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Đang tải lên...")
            setCancelable(false)
            show()
        }

        uploadJob = requireActivity().lifecycleScope.launch {
            try {
                cleanDuplicateProducts()

                // Nhận cả URL và ImageId từ hàm upload
                val (imageUrl, imageId) = uploadImage() ?: throw Exception("Lỗi upload ảnh")

                // Tạo đối tượng food với cả imageUrl và imageId
                val food = createFood(view, imageUrl, imageId)

                if (food.Title.isBlank()) {
                    throw Exception("Tên món ăn không được để trống")
                }

                saveFood(food)

                withContext(Dispatchers.Main) {
                    showToast("Thêm món ăn thành công")
                    listener?.invoke(food)
                    dismissAllowingStateLoss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Lỗi: ${e.message ?: "Không xác định"}")
                }
            } finally {
                progressDialog?.dismiss()
            }
        }
    }
    private suspend fun uploadImage(): Pair<String, String>? = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val resolver = activity?.contentResolver ?: return@withContext null
            val inputStream = resolver.openInputStream(selectedImageUri)
                ?: return@withContext null.also {
                    Log.e(TAG, "Không thể mở InputStream từ Uri")
                }

            // Tạo file tạm từ Uri
            val fileName = "food_${System.currentTimeMillis()}.jpg"
            tempFile = File.createTempFile("temp_", ".jpg", activity?.cacheDir)
            tempFile.outputStream().use { output -> inputStream.copyTo(output) }

            Log.d(TAG, "File tạm được tạo: ${tempFile.absolutePath}")

            // Tạo request multipart
            val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
            val fileNamePart = fileName.toRequestBody("text/plain".toMediaTypeOrNull())
            val folderPart = "food_images".toRequestBody("text/plain".toMediaTypeOrNull()) // thư mục lưu trên ImageKit

            // Gọi API upload
            val uploadResponse = imageKitService.uploadImage(
                file = filePart,
                fileName = fileNamePart,
                folder = folderPart
            )

            if (uploadResponse.url.isBlank() || uploadResponse.fileId.isBlank()) {
                Log.e(TAG, "Upload thành công nhưng URL hoặc fileId rỗng")
                return@withContext null
            }

            Log.d(TAG, "Upload thành công. URL: ${uploadResponse.url}, FileId: ${uploadResponse.fileId}")
            return@withContext Pair(uploadResponse.url, uploadResponse.fileId)

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong quá trình upload", e)
            return@withContext null
        } finally {
            tempFile?.delete()
        }
    }


    suspend fun cleanDuplicateProducts() = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.getReference("Foods").get().await()

            snapshot.children.forEach { child ->
                val title = child.child("title").getValue(String::class.java)
                val key = child.key

                // Kiểm tra null và điều kiện
                if (title != null && key != null && key == title && !key.matches(Regex("\\d+"))) {
                    child.ref.removeValue().await()
                    Log.d(TAG, "Đã xóa sản phẩm trùng lặp: $key")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi dọn dẹp sản phẩm trùng lặp", e)
        }
    }


    private fun createFood(view: View, imageUrl: String, imageId: String): Foods = Foods().apply {
        Title = view.findViewById<TextInputEditText>(R.id.etFoodName).text.toString()
        Description = view.findViewById<TextInputEditText>(R.id.etDescription).text.toString()
        Price = view.findViewById<TextInputEditText>(R.id.etPrice).text.toString().toDoubleOrNull() ?: 0.0
        ImagePath = imageUrl
        ImageId = imageId // Lưu ID ảnh từ ImageKit
        Star = view.findViewById<TextInputEditText>(R.id.etRating).text.toString().toDoubleOrNull() ?: 0.0
        TimeValue = view.findViewById<TextInputEditText>(R.id.etTime).text.toString().toIntOrNull() ?: 0
        BestFood = view.findViewById<MaterialCheckBox>(R.id.cbBestFood).isChecked
        CategoryId = selectedCategory.Id
        LocationId = selectedLocation.Id
        PriceId = selectedPrice.Id
        TimeId = selectedTime.Id
    }

    private fun setupDuplicateCleaner() {
        database.getReference("Foods").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                lifecycleScope.launch {
                    checkAndCleanDuplicate(snapshot)
                }
            }

            // Các phương thức khác có thể để trống
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private suspend fun checkAndCleanDuplicate(snapshot: DataSnapshot) {
        val title = snapshot.child("title").getValue(String::class.java)
        val key = snapshot.key

        if (title != null && key != null && key == title && !key.matches(Regex("\\d+"))) {
            // Đây là bản trùng lặp, cần xóa
            try {
                // Kiểm tra xem đã có bản gốc chưa
                val originalExists = database.getReference("Foods").child(key).get().await().exists()

                if (!originalExists) {
                    // Nếu không có bản gốc, không xóa (trường hợp thêm mới)
                    return
                }

                // Xóa bản trùng lặp
                snapshot.ref.removeValue().await()
                Log.d(TAG, "Đã xóa TỰ ĐỘNG sản phẩm trùng lặp: $key")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi xóa tự động sản phẩm trùng lặp", e)
            }
        }
    }

    private suspend fun deleteDuplicateImmediately(title: String, originalId: String) {
        try {
            // Kiểm tra và xóa sản phẩm có key bằng title nhưng khác với ID gốc
            val snapshot = database.getReference("Foods").child(title).get().await()

            if (snapshot.exists() && snapshot.key != originalId) {
                database.getReference("Foods").child(title).removeValue().await()
                Log.d(TAG, "Đã xóa NGAY sản phẩm trùng lặp với title: $title")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xóa sản phẩm trùng lặp ngay lập tức", e)
        }
    }

    private suspend fun saveFood(food: Foods) = withContext(Dispatchers.IO) {
        try {
            // Bước 1: Lấy ID tiếp theo
            val nextId = getNextFoodId()
            food.Id = nextId

            // Bước 2: Lưu sản phẩm mới
            database.getReference("Foods").child(nextId.toString()).setValue(food).await()
            Log.d(TAG, "Đã lưu sản phẩm với ID: ${food.Id}")

            // Bước 3: Xóa ngay lập tức sản phẩm trùng lặp (nếu có)
            deleteDuplicateImmediately(food.Title, nextId.toString())

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lưu sản phẩm", e)
            throw e
        }
    }
}
