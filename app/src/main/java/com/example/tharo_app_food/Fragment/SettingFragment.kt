package com.example.tharo_app_food.Fragment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Activity.LoginActivity
import com.example.tharo_app_food.Domain.DeleteRequest
import com.example.tharo_app_food.Domain.ImageKitService
import com.example.tharo_app_food.Domain.User
import com.example.tharo_app_food.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class SettingFragment: Fragment() {

    private lateinit var btnLogout: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: FirebaseDatabase

    private lateinit var userAvatar: ImageView
    private lateinit var userMail: TextView
    private lateinit var btnAddAvatar: ImageButton
    private lateinit var nameText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        btnLogout = view.findViewById(R.id.btnLogout)
        nameText = view.findViewById(R.id.name)
        userMail = view.findViewById(R.id.mail_text)
        btnAddAvatar = view.findViewById(R.id.btn_add_avatar)
        userAvatar = view.findViewById(R.id.user_avatar)

        userAvatar.setOnClickListener {
            openImagePicker()
        }

        sharedPreferences = requireContext().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app")

        setupUserInfo()

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            clearLoginSession()
            Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show()

            // Điều hướng về màn hình đăng nhập
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.15:3000/") // Thay bằng URL server Node.js
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val imageKitService by lazy {
        retrofit.create(ImageKitService::class.java)
    }

    private fun setupUserInfo() {
        val userName = sharedPreferences.getString("user_name", "Admin")
        nameText.text = userName ?: "Admin"

        val mail = sharedPreferences.getString("user_email", "Mail")
        userMail.text = mail ?: "Admin"

        loadUserAvatar()
    }

    private fun loadUserAvatar() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { userId ->
            database.getReference("Users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.Avatar?.let { avatarUrl ->
                            if (avatarUrl.isNotEmpty()) {
                                Glide.with(requireContext())
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .into(userAvatar)
                                btnAddAvatar.visibility = View.GONE
                            } else {
                                userAvatar.setImageResource(R.drawable.default_user)
                                btnAddAvatar.visibility = View.VISIBLE
                            }
                        } ?: run {
                            userAvatar.setImageResource(R.drawable.default_user)
                            btnAddAvatar.visibility = View.VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to load avatar", Toast.LENGTH_SHORT).show()
                    }
                })
        } ?: run {
            userAvatar.setImageResource(R.drawable.default_user)
            btnAddAvatar.visibility = View.VISIBLE
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(userAvatar)

        btnAddAvatar.visibility = View.GONE
        changeUserAvatar(uri)
    }

    private fun changeUserAvatar(newAvatarUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        database.getReference("Users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    val oldAvatarUrl = user?.Avatar
                    val oldImageId = user?.AvatarId

                    if (!oldAvatarUrl.isNullOrEmpty() && !oldImageId.isNullOrEmpty()) {
                        deleteOldAvatar(oldImageId) { isSuccess ->
                            if (isSuccess) {
                                uploadImageToImageKit(newAvatarUri)
                            } else {
                                activity?.runOnUiThread {
                                    Toast.makeText(
                                        context,
                                        "Failed to delete old avatar, but will continue with new upload",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    uploadImageToImageKit(newAvatarUri)
                                }
                            }
                        }
                    } else {
                        uploadImageToImageKit(newAvatarUri)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    showAddAvatarButton()
                }
            })
    }

    private fun deleteOldAvatar(imageId: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deleteResponse = imageKitService.deleteImage(DeleteRequest(imageId))
                if (deleteResponse.isSuccessful) {
                    deleteResponse.body()?.let {
                        if (it.success) {
                            Log.d("DeleteImage", "Deleted old avatar successfully")
                            callback(true)
                            return@launch
                        }
                    }
                }
                Log.e("DeleteImage", "Failed to delete old avatar: ${deleteResponse.errorBody()?.string()}")
                callback(false)
            } catch (e: Exception) {
                Log.e("DeleteImage", "Error deleting old avatar", e)
                callback(false)
            }
        }
    }

    private fun uploadImageToImageKit(uri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                val tempFile = File.createTempFile(
                    "avatar_${currentUser.uid}_${System.currentTimeMillis()}",
                    ".jpg",
                    requireContext().cacheDir
                ).apply {
                    outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                }

                val fileRequestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    tempFile.name,
                    fileRequestBody
                )

                val fileNamePart = "avatar_${currentUser.uid}_${System.currentTimeMillis()}.jpg"
                    .toRequestBody("text/plain".toMediaType())
                val folderPart = "/user_avatars/"
                    .toRequestBody("text/plain".toMediaType())

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = imageKitService.uploadImage(
                            file = filePart,
                            fileName = fileNamePart,
                            folder = folderPart
                        )

                        withContext(Dispatchers.Main) {
                            // Cập nhật cả Avatar và ImageId
                            saveAvatarInfoToDatabase(
                                userId = currentUser.uid,
                                imageUrl = response.url,
                                fileId = response.fileId
                            )
                            updateAvatarUI(response.url)
                            Toast.makeText(
                                requireContext(),
                                "Avatar updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showAddAvatarButton()
                            Toast.makeText(
                                requireContext(),
                                "Failed to upload avatar: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } finally {
                        tempFile.delete()
                    }
                }
            } ?: run {
                Toast.makeText(requireContext(), "Failed to read image data", Toast.LENGTH_SHORT).show()
                showAddAvatarButton()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            showAddAvatarButton()
        }
    }

    private fun updateAvatarUI(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .circleCrop()
            .into(userAvatar)
    }

    private fun showAddAvatarButton() {
        btnAddAvatar.visibility = View.VISIBLE
    }

    private fun saveAvatarInfoToDatabase(userId: String, imageUrl: String, fileId: String) {
        val userRef = database.getReference("Users").child(userId)

        val updates = hashMapOf<String, Any>(
            "Avatar" to imageUrl,
            "AvatarId" to fileId
        )

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("SaveAvatar", "Avatar info updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("SaveAvatar", "Failed to save avatar info", e)
                Toast.makeText(context, "Failed to save avatar info", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }


    private fun clearLoginSession() {
        val sharedPreferences = requireContext().getSharedPreferences("user_session", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}

