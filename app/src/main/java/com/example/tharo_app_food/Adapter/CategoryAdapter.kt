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
import com.example.tharo_app_food.Activity.ListFoodActivity
import com.example.tharo_app_food.Adapter.BestFoodAdapter.ViewHolder
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.R
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur

class CategoryAdapter(val items: ArrayList<Category>) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
    lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryAdapter.ViewHolder {
        context = parent.context
        val view: View = LayoutInflater.from(context).inflate(R.layout.viewholder_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryAdapter.ViewHolder, position: Int) {
        holder.titleTxt.text = items[position].Name

        var radius: Float = 10f
        var decorView: View = (holder.itemView.context as Activity).window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        var windowBackground: Drawable = decorView.background

        holder.blurView.setupWith(rootView, RenderScriptBlur(holder.itemView.context))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
        holder.blurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        holder.blurView.clipToOutline = true

        val drawableResourceId: Int = holder.itemView.resources.getIdentifier(
                items[position].ImagePath,
        "drawable",
        context.packageName
        )

        Glide.with((context))
            .load(drawableResourceId)
            .into(holder.pic)

        holder.itemView.setOnClickListener {
                val intent = Intent(context, ListFoodActivity::class.java)
                intent.putExtra("CategoryId", items.get(position).Id)
                intent.putExtra("CategoryName", items.get(position).Name)
                context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTxt: TextView = itemView.findViewById(R.id.titleCatTxt)
        val pic: ImageView = itemView.findViewById(R.id.imgCate)
        val blurView: BlurView = itemView.findViewById(R.id.blurView)
    }

}