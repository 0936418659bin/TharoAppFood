package com.example.tharo_app_food.Activity

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tharo_app_food.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ResetPassActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var btn_back: ImageView
    private lateinit var btn_send: Button
    private lateinit var et_email: TextInputEditText
    private lateinit var ti_email: TextInputLayout
    private lateinit var progressba: ProgressBar
    private lateinit var tv_instruc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_pass)

        // Khởi tạo Firebase Auth
        auth = Firebase.auth

        btn_back = findViewById(R.id.ivBack)
        btn_send = findViewById(R.id.btnSendResetLink)
        et_email = findViewById(R.id.etEmail)
        ti_email = findViewById(R.id.tilEmail)
        progressba = findViewById(R.id.progressBar)
        tv_instruc = findViewById(R.id.tvInstruction)
        setupViews()
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun setupViews() {
        // Nút quay lại
        btn_back.setOnClickListener { finish() }

        // Nút gửi link reset
        btn_send.setOnClickListener {
            val email = et_email.text.toString().trim()
            if (validateEmail(email)) {
                hideKeyboard()
                sendPasswordResetEmail(email)
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                ti_email.error = "Vui lòng nhập email"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                ti_email.error = "Email không hợp lệ"
                false
            }
            else -> {
                ti_email.error = null
                true
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    showSuccessDialog(email)
                } else {
                    showErrorDialog(task.exception?.message ?: "Gửi email thất bại")
                }
            }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressba.visibility = View.VISIBLE
            btn_send.isEnabled = false
            btn_send.text = "Đang xử lý..."
        } else {
            progressba.visibility = View.GONE
            btn_send.isEnabled = true
            btn_send.text = "Gửi liên kết đặt lại"
        }
    }

    private fun showSuccessDialog(email: String) {
        tv_instruc.visibility = View.VISIBLE

        AlertDialog.Builder(this)
            .setTitle("Email đã được gửi")
            .setMessage("Liên kết đặt lại mật khẩu đã được gửi đến $email. Vui lòng kiểm tra hộp thư đến hoặc thư mục spam.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Lỗi")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}