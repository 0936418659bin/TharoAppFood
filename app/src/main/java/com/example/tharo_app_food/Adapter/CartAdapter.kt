package com.example.tharo_app_food.Adapter

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.Helper.ChangeNumberItemsListener
import com.example.tharo_app_food.Helper.ManagementCart
import com.example.tharo_app_food.R
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CartAdapter(
    private var listItemSelected: ArrayList<Foods>,
    private val context: Context,
    private val changeNumberItemsListener: ChangeNumberItemsListener
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    private val managementCart = ManagementCart(context)
    private val decimalFormat: DecimalFormat by lazy {
        val formatSymbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
            decimalSeparator = '.'
        }
        DecimalFormat("#,##0.##", formatSymbols).apply {
            minimumFractionDigits = 3
            maximumFractionDigits = 3
        }
    }

    fun updateList(newList: ArrayList<Foods>) {
        listItemSelected = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_cart, parent, false)
        return ViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listItemSelected[position]

        // Hiển thị thông tin sản phẩm
        Glide.with(holder.itemView.context)
            .load(item.ImagePath)
            .into(holder.pic)

        holder.title.text = item.Title

        // Định dạng tiền Việt Nam: dấu . ngăn cách hàng nghìn, dấu , cho phần thập phân
        holder.feeEachItem.text = "${decimalFormat.format(item.Price * item.numberInChart)}đ"
        holder.totalEachItem.text = "${item.numberInChart} x ${decimalFormat.format(item.Price)}đ"
        holder.num.text = item.numberInChart.toString()

        // Xử lý tăng số lượng
        holder.plusItem.setOnClickListener {
            item.numberInChart++
            updateCartAndRefresh()
        }

        // Xử lý giảm số lượng
        holder.minusItem.setOnClickListener {
            if (item.numberInChart > 1) {
                item.numberInChart--
                updateCartAndRefresh()
            } else {
                // Nếu số lượng = 1 mà click giảm thì xóa item
                managementCart.removeItem(item) { success ->
                    if (success) {
                        listItemSelected.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, listItemSelected.size)
                        changeNumberItemsListener.change()
                    }
                }
            }
        }
    }

    private fun updateCartAndRefresh() {
        managementCart.updateCart(listItemSelected) { success ->
            if (success) {
                notifyDataSetChanged()
                changeNumberItemsListener.change()
            }
        }
    }

    override fun getItemCount(): Int = listItemSelected.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.titleTxt)
        val pic: ImageView = itemView.findViewById(R.id.pic)
        val feeEachItem: TextView = itemView.findViewById(R.id.feeEachItem)
        val totalEachItem: TextView = itemView.findViewById(R.id.totalEachItem)
        val plusItem: TextView = itemView.findViewById(R.id.plusBtn2)
        val minusItem: TextView = itemView.findViewById(R.id.minusBtn2)
        val num: TextView = itemView.findViewById(R.id.numTxt2)
        val blurView: BlurView = itemView.findViewById(R.id.blurView)
    }
}