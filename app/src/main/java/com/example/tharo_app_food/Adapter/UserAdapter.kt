package com.example.tharo_app_food.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView

class UserAdapter(
    private var categories: List<Category>,
    private var productCountMap: Map<Int, Int>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<UserAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val imageView: ShapeableImageView = itemView.findViewById(R.id.ivCategoryImage)
        private val nameTextView: MaterialTextView = itemView.findViewById(R.id.tvCategoryName)
        private val productCountTextView: MaterialTextView = itemView.findViewById(R.id.tvProductCount)
        private val itemCountTextView: MaterialTextView = itemView.findViewById(R.id.tvItemCount)

        fun bind(category: Category) {
            // Load image using Glide or your preferred image loading library
            val context = itemView.context
            val resId = context.resources.getIdentifier(category.ImagePath, "drawable", context.packageName)
            if (resId != 0) {
                Glide.with(context)
                    .load(resId)
                    .placeholder(R.drawable.default_user)
                    .into(imageView)
            } else {
                Glide.with(context)
                    .load(R.drawable.default_user)
                    .into(imageView)
            }

            nameTextView.text = category.Name
            val productCount = productCountMap[category.Id] ?: 0
            productCountTextView.text = "$productCount sản phẩm"
            itemCountTextView.text = productCount.toString()

            // Set click listener
            cardView.setOnClickListener {
                onItemClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_card, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<Category>, newProductCountMap: Map<Int, Int>) {
        categories = newCategories
        productCountMap = newProductCountMap
        notifyDataSetChanged()
    }
}