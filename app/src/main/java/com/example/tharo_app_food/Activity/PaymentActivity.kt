package com.example.tharo_app_food.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tharo_app_food.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.snackbar.Snackbar
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var tvTotalAmount: TextView
    private lateinit var btnPay: MaterialButton
    private lateinit var radioMomo: MaterialRadioButton
    private lateinit var radioVNPay: MaterialRadioButton
    private lateinit var radioCredit: MaterialRadioButton
    private lateinit var radioGroupPayment: RadioGroup
    private lateinit var back_btn: ImageButton

    // Định dạng số với dấu . ngăn cách hàng nghìn và dấu , cho phần thập phân
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
        setContentView(R.layout.activity_payment)

        initViews()
        setupPaymentMethods()
        handleBackButton()

        // Nhận và hiển thị tổng tiền
        val totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT",0.0) ?: "0"
        tvTotalAmount.text = "${decimalFormat.format(totalAmount)}đ"

    }

    private fun initViews() {
        btnPay = findViewById(R.id.btnPay)
        radioMomo = findViewById(R.id.radioMomo)
        radioVNPay = findViewById(R.id.radioVNPay)
        radioCredit = findViewById(R.id.radioCredit)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        back_btn = findViewById(R.id.btnBack)
        radioGroupPayment = findViewById(R.id.radioGroupPayment)
    }

    private fun setupPaymentMethods() {
        // Thiết lập radio buttons
        radioMomo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioVNPay.isChecked = false
                radioCredit.isChecked = false
            }
        }

        radioVNPay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioMomo.isChecked = false
                radioCredit.isChecked = false
            }
        }

        radioCredit.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioMomo.isChecked = false
                radioVNPay.isChecked = false
            }
        }

        // Xử lý thanh toán
        btnPay.setOnClickListener {
            when {
                radioMomo.isChecked -> processMomoPayment()
                radioVNPay.isChecked -> processVNPayPayment()
                radioCredit.isChecked -> processCreditCardPayment()
                else -> Toast.makeText(
                    this,
                    "Vui lòng chọn phương thức thanh toán",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun processMomoPayment() {
        // TODO: Tích hợp Momo SDK
        showPaymentSuccess("Thanh toán qua Momo thành công")
    }

    private fun processVNPayPayment() {
        // TODO: Tích hợp VNPay API
        showPaymentSuccess("Thanh toán qua VNPay thành công")
    }

    private fun processCreditCardPayment() {
        val totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT",0.0) ?: "0"
        val intent = Intent(this, PaymentQrActivity::class.java).apply {
            putExtra("TOTAL_AMOUNT1", totalAmount)
        }
        startActivity(intent)
    }

    private fun showPaymentSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish() // Quay lại màn hình trước sau khi thanh toán
    }

    private fun showError(message: String) {
        Snackbar.make(btnPay, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun handleBackButton() {
        back_btn.setOnClickListener { finish() }
    }
}