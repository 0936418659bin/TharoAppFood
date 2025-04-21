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
    private lateinit var adapter: RecyclerView.Adapter<*>
    private lateinit var managmentCart: ManagementCart
    private var tax: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managmentCart = ManagementCart(this)

        setVariable()
        calculateCart()
        setBlurEffect()

        initList()
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

    private fun initList() {
        if (managmentCart.getListCart().isEmpty()) {
            binding.emptyTxt.visibility = View.VISIBLE
            binding.scrollview.visibility = View.GONE
        } else {
            binding.emptyTxt.visibility = View.GONE
            binding.scrollview.visibility = View.VISIBLE
        }

        binding.cartView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        adapter = CartAdapter(managmentCart.getListCart(), this, object :
            ChangeNumberItemsListener {
            override fun change() {
                calculateCart()
            }
        })

        binding.cartView.adapter = adapter
    }


    private fun setVariable(){
        binding.backBtn.setOnClickListener{
            finish()
        }
    }

    private fun calculateCart() {
        val percentTax: Double = 0.02
        val delivery: Double = 10.0

        tax = Math.round(managmentCart.getTotalFee() * percentTax * 100.0) / 100.0

        val total: Double = Math.round(managmentCart.getTotalFee() + tax + delivery) / 100.0
        val itemTotal: Double = Math.round(managmentCart.getTotalFee() *100) / 100.0

        binding.totalFeeTxt.setText("$" + itemTotal)
        binding.taxTxt.setText("$" + tax)
        binding.delivery.setText("$" + delivery)
        binding.totalTxt.setText("$" + total)



    }
}