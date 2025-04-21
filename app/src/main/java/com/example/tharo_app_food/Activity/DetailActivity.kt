package com.example.tharo_app_food.Activity

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.Helper.ManagementCart
import com.example.tharo_app_food.databinding.ActivityDetailBinding
import eightbitlab.com.blurview.RenderScriptBlur

class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var foodObject: Foods
    private var quantity: Int = 1
    private lateinit var managementCart: ManagementCart

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
        // Tạo key ngay khi nhận dữ liệu
        if (foodObject.Key.isEmpty()) {
            foodObject.Key = foodObject.generateKey()
        }
    }

    private fun setupViews() {
        // Hiển thị thông tin sản phẩm
        Glide.with(this)
            .load(foodObject.ImagePath)
            .into(binding.pic)

        binding.titleTxt.text = foodObject.Title
        binding.priceTxt.text = "$${foodObject.Price}"
        binding.descriptionTxt.text = foodObject.Description
        binding.ratingTxt.text = "${foodObject.Star} Rating"
        binding.ratingBar.rating = foodObject.Star.toFloat()
        updateTotalPrice()

        // Xử lý nút tăng/giảm số lượng
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

        // Xử lý nút thêm vào giỏ hàng
        binding.addBtn.setOnClickListener {
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
                }
            }
        }

        binding.backBtn.setOnClickListener { finish() }
    }

    private fun updateTotalPrice() {
        binding.totalTxt.text = "$${"%.2f".format(quantity * foodObject.Price)}"
    }
}