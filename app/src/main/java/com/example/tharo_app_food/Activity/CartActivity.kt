package com.example.tharo_app_food.Activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tharo_app_food.Adapter.CartAdapter
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.Helper.ChangeNumberItemsListener
import com.example.tharo_app_food.Helper.ManagementCart
import com.example.tharo_app_food.R
import com.example.tharo_app_food.databinding.ActivityCartBinding
import com.google.firebase.auth.FirebaseAuth
import eightbitlab.com.blurview.RenderScriptBlur

class CartActivity : BaseActivity() {
    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private lateinit var managementCart: ManagementCart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser == null) {
            showLoginRequiredDialog()
            return
        }
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managementCart = ManagementCart(this)
        setupViews()
        loadCartData()
    }

    private fun setupRecyclerView(list: ArrayList<Foods>) {
        binding.cartView.layoutManager = LinearLayoutManager(this)
        adapter = CartAdapter(list, this, object : ChangeNumberItemsListener {
            override fun change() {
                calculateCart()
            }
        })
        binding.cartView.adapter = adapter

        // Tính toán chiều cao dựa trên số lượng item
        val itemHeight = resources.getDimensionPixelSize(R.dimen.cart_item_height) // 150dp
        val maxHeight = itemHeight * 3 // Giới hạn tối đa 3 items

        // Đặt chiều cao cho RecyclerView
        binding.cartView.layoutParams.height = if (list.size > 3) maxHeight else ViewGroup.LayoutParams.WRAP_CONTENT
        binding.cartView.requestLayout() // Yêu cầu vẽ lại layout
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
                navigateToMain()
            }
            .create()
            .show()
    }

    private fun navigateToMain() {
        // Thay thế bằng Intent đến màn hình đăng nhập của bạn
        finish()
    }


    private fun navigateToLogin() {
        // Thay thế bằng Intent đến màn hình đăng nhập của bạn
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupViews() {
        binding.backBtn.setOnClickListener { finish() }

        // Khởi tạo RecyclerView
        binding.cartView.layoutManager = LinearLayoutManager(this)
        adapter = CartAdapter(arrayListOf(), this, object : ChangeNumberItemsListener {
            override fun change() {
                calculateCart()
            }
        })
        binding.cartView.adapter = adapter
    }

    private fun loadCartData() {
        managementCart.getListCart { list ->
            if (list.isEmpty()) {
                binding.emptyTxt.visibility = View.VISIBLE
                binding.scrollview.visibility = View.GONE
            } else {
                binding.emptyTxt.visibility = View.GONE
                binding.scrollview.visibility = View.VISIBLE
                adapter.updateList(list)
                setupRecyclerView(list)
                calculateCart()
            }
        }
    }

    private fun calculateCart() {
        managementCart.getListCart { list ->
            val percentTax = 0.02
            val delivery = 10.0
            val subtotal = list.sumOf { it.Price * it.numberInChart }
            val tax = subtotal * percentTax
            val total = subtotal + tax + delivery

            binding.totalFeeTxt.text = "$${"%.2f".format(subtotal)}"
            binding.taxTxt.text = "$${"%.2f".format(tax)}"
            binding.delivery.text = "$${"%.2f".format(delivery)}"
            binding.totalTxt.text = "$${"%.2f".format(total)}"
        }
    }
}