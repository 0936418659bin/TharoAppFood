package com.example.tharo_app_food.Activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tharo_app_food.Adapter.ListFoodAdapter
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.databinding.ActivityListFoodBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class ListFoodActivity : BaseActivity() {

    private lateinit var binding: ActivityListFoodBinding
    private lateinit var adapterListFood: RecyclerView.Adapter<*>

    private var categoryId: Int = 0
    private var categoryName: String = ""
    private var searchText: String = ""
    private var isSearch: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentExtra()
        initList()
    }

    private fun getIntentExtra() {
        categoryId = intent.getIntExtra("CategoryId", 0)
        categoryName = intent.getStringExtra("CategoryName") ?: ""
        searchText = intent.getStringExtra("text") ?: ""
        isSearch = intent.getBooleanExtra("isSearch", false)

        binding.titleTxt.text = categoryName
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun initList() {
        val myref: DatabaseReference = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Foods")
        binding.progressBar.visibility = View.VISIBLE
        val list = ArrayList<Foods>()

        val query: Query = if (isSearch) {
            myref.orderByChild("Title").startAt(searchText).endAt("$searchText\uf8ff")
        } else {
            myref.orderByChild("CategoryId").equalTo(categoryId.toDouble())
        }

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (issue in snapshot.children) {
                        issue.getValue(Foods::class.java)?.let { list.add(it) }
                    }
                }

                if (list.isNotEmpty()) {
                    binding.foodListView.layoutManager = GridLayoutManager(this@ListFoodActivity, 2)
                    adapterListFood = ListFoodAdapter(this@ListFoodActivity,list)
                    binding.foodListView.adapter = adapterListFood
                }

                binding.progressBar.visibility = View.GONE

            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu cần
            }
        })
    }

}
