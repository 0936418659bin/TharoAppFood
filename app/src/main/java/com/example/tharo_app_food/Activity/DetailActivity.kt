package com.example.tharo_app_food.Activity

import android.graphics.drawable.Drawable
import android.os.Bundle
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
    private lateinit var objects: Foods
    private var num: Int = 1
    private lateinit var managementCart: ManagementCart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        managementCart = ManagementCart(this)

        getBundleExtra()
        setVariable()
        setBlurEffect()
    }

    fun setBlurEffect() {
        val radius = 10f
        val decorView: View = this.window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground: Drawable = decorView.background

        binding.blurView.setupWith(rootView, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
        binding.blurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        binding.blurView.clipToOutline = true

        binding.blurView2.setupWith(rootView, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
        binding.blurView2.outlineProvider = ViewOutlineProvider.BACKGROUND
        binding.blurView2.clipToOutline = true
    }

    fun setVariable() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        Log.d("DetailActivity", "Dữ liệu đối tượng: $objects")

        if (!objects.ImagePath.isNullOrEmpty()) {
            Log.d("DetailActivity", "Tải ảnh từ đường dẫn: ${objects.ImagePath}")
            Glide.with(this)
                .load(objects.ImagePath)
                .into(binding.pic)
        } else {
            Log.e("GlideError", "ImagePath is null or empty")
        }

        binding.priceTxt.text = "$${objects.Price}"
        binding.titleTxt.text = objects.Title
        binding.descriptionTxt.text = objects.Description
        binding.ratingTxt.text = "${objects.Star} Rating"
        binding.ratingBar.rating = objects.Star.toFloat()
        binding.totalTxt.text = String.format(java.util.Locale.US, "$%.2f", num * objects.Price)

        binding.plusBtn.setOnClickListener {
            num += 1
            Log.d("DetailActivity", "Số lượng sau khi tăng: $num")
            binding.numTxt.text = String.format(java.util.Locale.US, "%d", num)
            binding.totalTxt.text = String.format(java.util.Locale.US, "$%.2f", num * objects.Price)
        }

        binding.minusBtn.setOnClickListener {
            if (num > 1) {
                num -= 1
                Log.d("DetailActivity", "Số lượng sau khi giảm: $num")
                binding.numTxt.text = String.format(java.util.Locale.US, "%d", num)
                binding.totalTxt.text = String.format(java.util.Locale.US, "$%.2f", num * objects.Price)
            }
        }

        binding.addBtn.setOnClickListener {
            objects.numberInChart = num
            Log.d("DetailActivity", "Thêm vào giỏ hàng: ${objects.Title}, Số lượng: $num")
            managementCart.insertFood(objects)
        }
    }

    fun getBundleExtra() {
        @Suppress("DEPRECATION")
        objects = intent.getSerializableExtra("object") as? Foods ?: Foods()
        Log.d("DetailActivity", "Nhận dữ liệu từ Intent: $objects")
    }
}
