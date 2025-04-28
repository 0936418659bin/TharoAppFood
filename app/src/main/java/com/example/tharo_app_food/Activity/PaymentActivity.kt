package com.example.tharo_app_food.Activity

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
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var tvTotalAmount: TextView

    private lateinit var btnPay: MaterialButton
    private lateinit var radioMomo: MaterialRadioButton
    private lateinit var radioVNPay: MaterialRadioButton
    private lateinit var radioCredit: MaterialRadioButton
    private lateinit var radioGroupPayment: RadioGroup
    private lateinit var back_btn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        btnPay = findViewById(R.id.btnPay)


        // Init views
        btnPay = findViewById(R.id.btnPay)
        radioMomo = findViewById(R.id.radioMomo)
        radioVNPay = findViewById(R.id.radioVNPay)
        radioCredit = findViewById(R.id.radioCredit)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        back_btn = findViewById(R.id.btnBack)

        val totalAmount = intent.getStringExtra("TOTAL_AMOUNT") ?: "0 VND"

        tvTotalAmount.text = formatCurrency(totalAmount)

        // Payment method selection
        val paymentCards = listOf(
            findViewById<MaterialCardView>(R.id.cardMomo),
            findViewById<MaterialCardView>(R.id.cardVNPay),
            findViewById<MaterialCardView>(R.id.cardCredit)
        )

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


        // Pay button click
        btnPay.setOnClickListener {
            val selectedId = radioGroupPayment.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (selectedId) {
                R.id.radioMomo -> processMomoPayment()
                R.id.radioVNPay -> processVNPayPayment()
                R.id.radioCredit -> processCreditCardPayment()
            }
        }
        backBtn()
    }

    private fun formatCurrency(amount: String): String {
        return try {
            // Loại bỏ tất cả ký tự không phải số hoặc dấu chấm
            val cleanAmount = amount.replace("[^\\d.]".toRegex(), "")

            // Kiểm tra nếu chuỗi rỗng
            if (cleanAmount.isEmpty()) return "0 VND"

            // Chuyển đổi sang Double
            val numericValue = cleanAmount.toDouble()

            // Format thành VND (ví dụ: 10.0 -> 240.000 VND)
            val vndAmount = (numericValue * 24000).toInt()
            "%,d VND".format(vndAmount).replace(",", ".")
        } catch (e: Exception) {
            e.printStackTrace()
            "0 VND" // Trả về giá trị mặc định nếu có lỗi
        }
    }

    private fun processMomoPayment() {
        // TODO: Integrate Momo SDK
    }

    private fun processVNPayPayment() {
        // TODO: Integrate VNPay API
    }

    private fun processCreditCardPayment() {
        // TODO: Integrate Stripe/VNPay Card
    }

    private fun showError(message: String) {
        Snackbar.make(btnPay, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun backBtn() {
        back_btn.setOnClickListener{finish()}
    }
}