package com.example.tharo_app_food.Dialog

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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

class AddCategoryDialog : DialogFragment() {

    private var listener: ((Category) -> Unit)? = null
    private var uploadJob: Job? = null
    private lateinit var selectedImageUri: Uri
    private var progressDialog: ProgressDialog? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
        private const val REQUEST_READ_STORAGE_PERMISSION = 101
    }

    private val database by lazy {
        FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
    }

    private val imageApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.4:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImageKitService::class.java)
    }

    fun setOnProductAddedListener(listener: (Category) -> Unit) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null)
        setupImagePicker(view)
        setupDuplicateCleaner()

        return MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialogTheme)
            .setView(view)
            .setTitle("Thêm danh mục mới")
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

    private fun validateInputs(view: View): Boolean {
        val nameInput = view.findViewById<TextInputEditText>(R.id.etCategoryName).text?.toString()?.trim()
        return when {
            !::selectedImageUri.isInitialized -> {
                showToast("Vui lòng chọn ảnh")
                false
            }
            nameInput.isNullOrEmpty() -> {
                showToast("Vui lòng nhập tên danh mục")
                false
            }
            else -> true
        }
    }

    private fun setupImagePicker(view: View) {
        view.findViewById<MaterialButton>(R.id.btnSelectImage).setOnClickListener {
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
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                displaySelectedImage(uri)
                view?.findViewById<MaterialButton>(R.id.btnSelectImage)?.visibility = View.GONE
            } ?: showToast("Không thể lấy ảnh đã chọn")
        }
    }

    private fun displaySelectedImage(uri: Uri) {
        dialog?.findViewById<ImageView>(R.id.ivCategoryImage)?.let {
            Glide.with(this).load(uri).centerCrop().into(it)
        }
    }

    private fun uploadImageAndAddProduct(view: View) {
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Đang tải ảnh...")
            setCancelable(false)
            show()
        }

        uploadJob = requireActivity().lifecycleScope.launch {
            try {
                cleanDuplicateCate()

                val (imageUrl, imageId) = uploadImage() ?: throw Exception("Upload ảnh thất bại")
                val food = createFood(view, imageUrl, imageId)

                if (food.Name.isBlank()) throw Exception("Tên không hợp lệ")

                saveFood(food)

                withContext(Dispatchers.Main) {
                    showToast("Đã thêm danh mục")
                    listener?.invoke(food)
                    dismissAllowingStateLoss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Lỗi: ${e.message}")
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
            val fileName = "cate_${System.currentTimeMillis()}.jpg"
            tempFile = File.createTempFile("temp_", ".jpg", activity?.cacheDir)
            tempFile.outputStream().use { output -> inputStream.copyTo(output) }

            Log.d(TAG, "File tạm được tạo: ${tempFile.absolutePath}")

            // Tạo request multipart
            val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
            val fileNamePart = fileName.toRequestBody("text/plain".toMediaTypeOrNull())
            val folderPart = "category_images".toRequestBody("text/plain".toMediaTypeOrNull()) // thư mục lưu trên ImageKit

            // Gọi API upload
            val uploadResponse = imageApi.uploadImage(
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

    private fun createFood(view: View, imageUrl: String, imageId: String): Category {
        return Category().apply {
            Name = view.findViewById<TextInputEditText>(R.id.etCategoryName).text.toString()
            ImagePath = imageUrl
            ImageId = imageId // Lưu ID ảnh từ ImageKit
        }
    }

    private suspend fun saveFood(cate: Category) = withContext(Dispatchers.IO) {
        val nextId = getNextFoodId()
        cate.Id = nextId

        // Tạo map dữ liệu với tên trường chính xác
        val categoryData = mapOf(
            "Id" to cate.Id,
            "Name" to cate.Name,
            "ImagePath" to cate.ImagePath,
            "ImageId" to cate.ImageId
        )

        // Lưu cả thông tin ảnh bao gồm ImageId
        database.getReference("Category").child(nextId.toString()).setValue(categoryData).await()
        Log.d(TAG, "Đã lưu danh mục với ID: ${cate.Id}")
        deleteDuplicateImmediately(cate.Name, nextId.toString())
    }

    private suspend fun getNextFoodId(): Int = withContext(Dispatchers.IO) {
        val snapshot = database.getReference("Category").get().await()
        val ids = snapshot.children.mapNotNull { it.key?.toIntOrNull() }
        return@withContext (ids.maxOrNull() ?: 0) + 1
    }

    private suspend fun deleteDuplicateImmediately(title: String, originalId: String) {
        try {
            // Kiểm tra và xóa sản phẩm có key bằng title nhưng khác với ID gốc
            val snapshot = database.getReference("Category").child(title).get().await()

            if (snapshot.exists() && snapshot.key != originalId) {
                database.getReference("Category").child(title).removeValue().await()
                Log.d(TAG, "Đã xóa NGAY sản phẩm trùng lặp với Name: $title")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xóa sản phẩm trùng lặp ngay lập tức", e)
        }
    }

    suspend fun cleanDuplicateCate() = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.getReference("Category").get().await()

            snapshot.children.forEach { child ->
                // Thay đổi từ "Name" sang "name" để phù hợp với cách Firebase lưu trữ
                val name = child.child("name").getValue(String::class.java)
                val key = child.key

                // Kiểm tra null và điều kiện
                if (name != null && key != null && key == name && !key.matches(Regex("\\d+"))) {
                    child.ref.removeValue().await()
                    Log.d(TAG, "Đã xóa danh mục trùng lặp: $key")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi dọn dẹp danh mục trùng lặp", e)
        }
    }

    private fun setupDuplicateCleaner() {
        database.getReference("Category").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                lifecycleScope.launch { checkAndCleanDuplicate(snapshot) }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private suspend fun checkAndCleanDuplicate(snapshot: DataSnapshot) {
        // Thay đổi từ "Name" sang "name" để phù hợp với cách Firebase lưu trữ
        val name = snapshot.child("name").getValue(String::class.java)
        val key = snapshot.key

        if (name != null && key != null && key == name && !key.matches(Regex("\\d+"))) {
            // Đây là bản trùng lặp, cần xóa
            try {
                // Kiểm tra xem đã có bản gốc chưa
                val originalExists = database.getReference("Category").child(key).get().await().exists()

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

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog?.dismiss()
    }
}