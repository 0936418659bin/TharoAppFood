package com.example.tharo_app_food.Activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.Helper.ManagementCart
import com.example.tharo_app_food.databinding.ActivityDetailBinding
import com.google.firebase.auth.FirebaseAuth
import eightbitlab.com.blurview.RenderScriptBlur

class DetailActivity : BaseActivity() {
    private val TAG = "DetailActivity"
    private lateinit var binding: ActivityDetailBinding
    private lateinit var foodObject: Foods
    private var quantity: Int = 1
    private lateinit var managementCart: ManagementCart
    private val auth = FirebaseAuth.getInstance() // Thêm FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managementCart = ManagementCart(this)
        getBundleExtra()
        setupViews()
    }

    private fun getBundleExtra() {
        foodObject = intent.getSerializableExtra("object") as? Foods ?: Foods()
        if (foodObject.Key.isEmpty()) {
            foodObject.Key = foodObject.generateKey()
        }
    }

    private fun setupViews() {
        Log.d(TAG, "Setting up views for product: ${foodObject.Title}")
        // Hiển thị thông tin sản phẩm (giữ nguyên)
        Glide.with(this)
            .load(foodObject.ImagePath)
            .into(binding.pic)

        binding.titleTxt.text = foodObject.Title
        binding.priceTxt.text = "%,.3fđ".format(foodObject.Price)
        binding.descriptionTxt.text = foodObject.Description
        binding.timeTxt.text = "${foodObject.TimeValue} phút"
        binding.ratingTxt.text = "${foodObject.Star} Đánh giá"
        binding.ratingBar.rating = foodObject.Star.toFloat()
        updateTotalPrice()

        // Xử lý nút tăng/giảm số lượng (giữ nguyên)
        binding.plusBtn.setOnClickListener {

            quantity++
            binding.numTxt.text = quantity.toString()
            updateTotalPrice()
        }

        binding.minusBtn.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.numTxt.text = quantity.toString()
                updateTotalPrice()
            }
        }

        // Xử lý nút thêm vào giỏ hàng (cập nhật)
        binding.addBtn.setOnClickListener {
            Log.d(TAG, "Add to cart button clicked")
            if (FirebaseAuth.getInstance().currentUser == null) {
                Log.w(TAG, "User not authenticated - showing login dialog")
                showLoginRequiredDialog()
                return@setOnClickListener
            }
            addToCart()
        }

        binding.backBtn.setOnClickListener { finish() }
    }

    private fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    private fun showLoginRequiredDialog() {
        // Sử dụng AlertDialog.Builder của AndroidX
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Yêu cầu đăng nhập")
            .setMessage("Bạn cần đăng nhập để thêm sản phẩm vào giỏ hàng")
            .setPositiveButton("Đăng nhập") { dialog, _ ->
                dialog.dismiss()
                navigateToLogin()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun navigateToLogin() {
        // Thay thế bằng Intent đến màn hình đăng nhập của bạn
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun addToCart() {
        foodObject.numberInChart = quantity
        binding.addBtn.isEnabled = false
        binding.addBtn.text = "Đang thêm..."

        managementCart.insertFood(foodObject) { success ->
            if (success) {
                binding.addBtn.text = "Đã thêm"
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 1000)
            } else {
                binding.addBtn.isEnabled = true
                binding.addBtn.text = "Thêm vào giỏ"
                Toast.makeText(this, "Có lỗi xảy ra, vui lòng thử lại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTotalPrice() {
        binding.totalTxt.text = "${"%.3fđ".format(quantity * foodObject.Price)}"
    }

    override fun onDestroy() {
        managementCart.clear()
        super.onDestroy()
    }
}