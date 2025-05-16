package com.example.tharo_app_food.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tharo_app_food.Domain.User
import com.example.tharo_app_food.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var register: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var reset_pass: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        // Khởi tạo Firebase với KTX
        auth = Firebase.auth
        database = Firebase.database("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("Users")

        emailEditText = findViewById(R.id.signup_email)
        passwordEditText = findViewById(R.id.signup_pass)
        loginButton = findViewById(R.id.btn_login)
        register = findViewById(R.id.textViewRegister)
        reset_pass = findViewById(R.id.reset_pass)

        loginButton.setOnClickListener {
            val emailOrUsername = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (emailOrUsername.isEmpty() || password.isEmpty()) {
                showToast("Vui lòng điền đầy đủ thông tin")
                return@setOnClickListener
            }

            loginUser(emailOrUsername, password)
        }

        register.setOnClickListener {
            navigateToRegisterScreen()
        }
        reset_pass.setOnClickListener {
            navigateToResetPass()
        }
    }

    private fun loginUser(emailOrUsername: String, password: String) {
        Log.d(TAG, "Attempting login with: $emailOrUsername")

        if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
            // Xử lý đăng nhập bằng email
            auth.signInWithEmailAndPassword(emailOrUsername, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = task.result?.user?.uid
                        if (userId != null) {
                            // Thêm log để kiểm tra
                            Log.d(TAG, "Login with email success, UID: $userId")
                            fetchUserData(userId)
                        } else {
                            showToast("Không thể lấy thông tin người dùng")
                        }
                    } else {
                        Log.e(TAG, "Email login failed", task.exception)
                        showToast("Đăng nhập thất bại: ${task.exception?.message ?: "Lỗi không xác định"}")
                    }
                }
        } else {
            // Xử lý đăng nhập bằng username
            database.orderByChild("UserName")
                .equalTo(emailOrUsername)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            showToast("Tài khoản không tồn tại")
                            return
                        }

                        snapshot.children.firstOrNull()?.let { userSnapshot ->
                            val user = userSnapshot.getValue(User::class.java)
                            user?.Email?.let { email ->
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            fetchUserData(userSnapshot.key ?: "")
                                        } else {
                                            Log.w(TAG, "Username login failed", task.exception)
                                            showToast("Sai mật khẩu")
                                        }
                                    }
                            } ?: showToast("Không tìm thấy email đăng ký")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Database error: ${error.message}")
                        showToast("Lỗi hệ thống")
                    }
                })
        }
    }

    private fun fetchUserData(userId: String) {
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    handleLoginSuccess(user)
                } else {
                    showToast("Không thể lấy thông tin người dùng")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Fetch user failed: ${error.message}")
                showToast("Lỗi khi tải thông tin người dùng")
            }
        })
    }

    private fun handleLoginSuccess(user: User) {
        Log.d(TAG, "Login successful for user: ${user.UserName} with role: ${user.Role}")

        // Lưu thông tin phiên
        getSharedPreferences("user_session", MODE_PRIVATE).edit().apply {
            putBoolean("is_logged_in", true)
            putInt("user_id", user.Id)
            putString("user_name", user.UserName)
            putString("user_role", user.Role)
            putString("user_email", user.Email)
            putString("user_image", user.Avatar)
            apply()
        }

        // Chuyển hướng
        val destination = when (user.Role) {
            "Admin" -> {
                showToast("Chào mừng Admin")
                Intent(this, AdminActivity::class.java)
            }
            else -> Intent(this, MainActivity::class.java)
        }

        startActivity(destination)
        finish()
    }

    private fun navigateToRegisterScreen() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun navigateToResetPass() {
        startActivity(Intent(this, ResetPassActivity::class.java))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}