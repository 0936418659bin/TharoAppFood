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

class CartAdapter(
    private val listItemSelected: ArrayList<Foods>,
    context: Context,
    private val changeNumberItemsListener: ChangeNumberItemsListener
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    private val managementCart = ManagementCart(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_cart, parent, false)
        return ViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        var radius: Float = 10f
        var decorView: View = (holder.itemView.context as Activity).window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        var windowBackground: Drawable = decorView.background

        holder.blurView.setupWith(rootView, RenderScriptBlur(holder.itemView.context))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
        holder.blurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        holder.blurView.clipToOutline = true

        Glide.with(holder.itemView.context)
            .load(listItemSelected[position].ImagePath)
            .transform(CenterCrop(), RoundedCorners(30))
            .into(holder.pic)

        holder.title.text = listItemSelected[position].Title
        holder.feeEachItem.text = "$" + listItemSelected[position].Price.toString()

        holder.totalEachItem.text = "${listItemSelected[position].numberInChart} x $${listItemSelected[position].Price}"

        holder.num.text = listItemSelected[position].numberInChart.toString()

        holder.plusItem.setOnClickListener {
            managementCart.plusNumberItem(listItemSelected, position, object : ChangeNumberItemsListener {
                override fun change() {
                    changeNumberItemsListener.change()
                    notifyDataSetChanged()
                }
            })
        }

        holder.minusItem.setOnClickListener {
            managementCart.minusNumberItem(listItemSelected, position, object : ChangeNumberItemsListener {
                override fun change() {
                    changeNumberItemsListener.change()
                    notifyDataSetChanged()
                }
            })
        }


    }

    override fun getItemCount(): Int {
        return listItemSelected.size
    }

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
