package com.example.tharo_app_food.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tharo_app_food.R
import com.google.android.material.appbar.MaterialToolbar
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class PaymentQrActivity: AppCompatActivity() {

    private lateinit var tvAmount: TextView
    private lateinit var back_btn: MaterialToolbar

    private val decimalFormat: DecimalFormat by lazy {
        val formatSymbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
            decimalSeparator = '.'
        }
        DecimalFormat("#,##0.###", formatSymbols).apply {
            minimumFractionDigits = 3
            maximumFractionDigits = 3
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_payment)

        initViews()
        handleBackButton()

        // Nhận và hiển thị tổng tiền
        val totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT1",0.0) ?: "0"
        tvAmount.text = "${decimalFormat.format(totalAmount)}đ"

    }

    private fun initViews() {
        tvAmount = findViewById(R.id.tvAmount)
        back_btn = findViewById(R.id.toolbar)
    }

    private fun handleBackButton() {
        back_btn.setOnClickListener { finish() }
    }
}