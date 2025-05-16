package com.example.tharo_app_food.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView

class ProductAdapter(
    private var foods: List<Foods>,
    private val onFoodClick: (Foods) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ShapeableImageView = itemView.findViewById(R.id.ivFoodImage)
        private val nameTextView: MaterialTextView = itemView.findViewById(R.id.tvFoodName)
        private val priceTextView: MaterialTextView = itemView.findViewById(R.id.tvFoodPrice)
        private val ratingTextView: MaterialTextView = itemView.findViewById(R.id.tvFoodRating)
        private val timeTextView: MaterialTextView = itemView.findViewById(R.id.tvFoodTime)
        private val bestFoodBadge: MaterialTextView = itemView.findViewById(R.id.tvBestFoodBadge)

        fun bind(food: Foods) {
            Glide.with(itemView.context)
                .load(food.ImagePath)
                .placeholder(R.drawable.default_user)
                .into(imageView)

            nameTextView.text = food.Title
            priceTextView.text = "%,.3fđ".format(food.Price)
            ratingTextView.text = "⭐ ${food.Star}"
            timeTextView.text = "${food.TimeValue} phút"

            if (food.BestFood) {
                bestFoodBadge.visibility = View.VISIBLE
                bestFoodBadge.text = "Phổ biến"
            } else {
                bestFoodBadge.visibility = View.GONE
            }

            itemView.setOnClickListener { onFoodClick(food) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(foods[position])
    }

    override fun getItemCount(): Int = foods.size

    fun updateProducts(newFoods: List<Foods>) {
        foods = newFoods
        notifyDataSetChanged()
    }
}