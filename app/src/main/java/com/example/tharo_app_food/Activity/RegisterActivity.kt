package com.example.tharo_app_food.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tharo_app_food.Domain.User
import com.example.tharo_app_food.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // Các view
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var readyHaveAccount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Khởi tạo Firebase
        auth = Firebase.auth
        database = Firebase.database("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("Users")

        // Ánh xạ view
        usernameEditText = findViewById(R.id.signup_username)
        emailEditText = findViewById(R.id.signup_email)
        passwordEditText = findViewById(R.id.pass_pw)
        confirmPasswordEditText = findViewById(R.id.pass_cpw)
        registerButton = findViewById(R.id.register_button)
        readyHaveAccount = findViewById(R.id.readyHaveAccount)

        readyHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // optional: đóng màn đăng ký nếu không muốn quay lại
        }

        registerButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (!validateInputs(username, email, password, confirmPassword)) {
            return
        }

        // Bước 1: Lấy ID lớn nhất hiện có
        database.orderByChild("Id").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var maxId = 0

                    // Tìm ID lớn nhất hiện có
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            user?.Id?.let { currentId ->
                                if (currentId > maxId) {
                                    maxId = currentId
                                }
                            }
                        }
                    }

                    // Tăng lên 1 đơn vị cho user mới
                    val newId = maxId + 1

                    // Bước 2: Tạo tài khoản với Firebase Auth
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this@RegisterActivity) { task ->
                            if (task.isSuccessful) {
                                val userId = task.result?.user?.uid ?: ""

                                // Tạo đối tượng User với ID mới
                                val newUser = User(
                                    Id = newId,
                                    UserName = username,
                                    Email = email,
                                    Role = "User",
                                    Avatar = ""
                                )

                                // Lưu thông tin user vào Realtime Database
                                saveUserToDatabase(userId, newUser)
                            } else {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Đăng ký thất bại: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Lỗi khi lấy dữ liệu: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun validateInputs(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (username.isEmpty()) {
            usernameEditText.error = "Vui lòng nhập tên đăng nhập"
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Email không hợp lệ"
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            passwordEditText.error = "Mật khẩu phải có ít nhất 6 ký tự"
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Mật khẩu không khớp"
            return false
        }

        return true
    }

    private fun saveUserToDatabase(userId: String, user: User) {
        val userMap = mapOf(
            "Id" to user.Id,
            "UserName" to user.UserName,
            "Email" to user.Email,
            "Role" to user.Role,
            "Avatar" to user.Avatar
        )

        database.child(userId).setValue(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi lưu thông tin: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}