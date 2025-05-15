package com.example.tharo_app_food.Adapter

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.tharo_app_food.Activity.ListFoodActivity
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.R

class CategoryAdapter(val items: ArrayList<Category>) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view: View = LayoutInflater.from(context).inflate(R.layout.viewholder_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = items[position]
        Log.d("CategoryAdapter", "Category data: ${category.toString()}")
        holder.titleTxt.text = category.Name


        val imagePath = category.ImagePath?.trim() ?: ""

        // Thêm log để debug URL ảnh
        Log.d("CategoryAdapter", "Loading image: $imagePath")

        if (imagePath.startsWith("http")) {
            Glide.with(context)
                .load(imagePath)
                .placeholder(R.drawable.default_user)
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // Tạm thời tắt cache để test
                .skipMemoryCache(true)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("CategoryAdapter", "Image load failed: $imagePath", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("CategoryAdapter", "Image loaded successfully: $imagePath")
                        return false
                    }
                })
                .into(holder.pic)
        } else {
            val drawableResourceId: Int = context.resources.getIdentifier(
                imagePath, "drawable", context.packageName
            )

            if (drawableResourceId != 0) {
                Glide.with(context)
                    .load(drawableResourceId)
                    .placeholder(R.drawable.default_user)
                    .error(R.drawable.default_user)
                    .into(holder.pic)
            } else {
                holder.pic.setImageResource(R.drawable.default_user)
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ListFoodActivity::class.java)
            intent.putExtra("CategoryId", category.Id)
            intent.putExtra("CategoryName", category.Name)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTxt: TextView = itemView.findViewById(R.id.titleCatTxt)
        val pic: ImageView = itemView.findViewById(R.id.imgCate)
    }
}