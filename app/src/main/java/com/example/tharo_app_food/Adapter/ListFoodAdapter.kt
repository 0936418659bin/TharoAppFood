package com.example.tharo_app_food.Adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.tharo_app_food.Activity.DetailActivity
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.R
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur

class ListFoodAdapter(private val context: Context, private val items: ArrayList<Foods>) : RecyclerView.Adapter<ListFoodAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.viewholder_list_food, parent, false)
        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.titleTxt.text = items[position].Title
        holder.priceTxt.text = "%,.3fđ".format(items[position].Price)
        holder.starTxt.text = "${items[position].Star}"
        holder.timeTxt.text = "${items[position].TimeValue} Phút"

        var radius: Float = 10f
        var decorView: View = (holder.itemView.context as Activity).window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        var windowBackground: Drawable = decorView.background

        holder.blurView.setupWith(rootView, RenderScriptBlur(holder.itemView.context))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
        holder.blurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        holder.blurView.clipToOutline = true

        Glide.with(context)
            .load(items[position].ImagePath)
            .transform(RoundedCorners(30))
            .into(holder.pic)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", items[position])
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTxt: TextView = itemView.findViewById(R.id.titleTxt)
        val priceTxt: TextView = itemView.findViewById(R.id.priceTxt)
        val starTxt: TextView = itemView.findViewById(R.id.starTxt)
        val timeTxt: TextView = itemView.findViewById(R.id.timeTxt)
        val pic: ImageView = itemView.findViewById(R.id.img)
        val blurView: BlurView = itemView.findViewById(R.id.blurView2)
    }
}