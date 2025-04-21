package com.example.tharo_app_food.Activity

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
import com.example.tharo_app_food.Helper.ChangeNumberItemsListener
import com.example.tharo_app_food.Helper.ManagementCart
import com.example.tharo_app_food.R
import com.example.tharo_app_food.databinding.ActivityCartBinding
import eightbitlab.com.blurview.RenderScriptBlur

class CartActivity : BaseActivity() {
    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private lateinit var managementCart: ManagementCart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managementCart = ManagementCart(this)
        setupViews()
        loadCartData()
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