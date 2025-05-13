package com.example.tharo_app_food.Fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tharo_app_food.Adapter.CategoryAdapter
import com.example.tharo_app_food.Adapter.UserAdapter
import com.example.tharo_app_food.Config.GridSpacingItemDecoration
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.R
import com.google.android.material.search.SearchBar
import com.google.firebase.database.*
import com.google.android.material.search.SearchView

class CategoryFragment : Fragment() {

    private val productCountMap = mutableMapOf<Int, Int>()

    private lateinit var searchBar: SearchBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: UserAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var categoriesRef: DatabaseReference
    private lateinit var searchView: SearchView

    private val categoriesList = mutableListOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
        categoriesRef = database.getReference("Category")

        // Initialize views
        searchBar = view.findViewById(R.id.searchBar)
        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.cateListView)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup search functionality
        setupSearch()

        // Load data from Firebase
        loadCategories()
    }

    private fun setupRecyclerView() {
        // Khởi tạo với productCountMap rỗng ban đầu
        categoryAdapter = UserAdapter(
            ArrayList(categoriesList),
            mutableMapOf(), // Truyền map rỗng ban đầu
            { category ->
                // Handle item click if needed
            }
        )

        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = categoryAdapter
            addItemDecoration(
                GridSpacingItemDecoration(
                    2,
                    resources.getDimensionPixelSize(R.dimen.grid_spacing),
                    true
                )
            )
        }
    }

    private fun setupSearch() {
        // Show SearchView when clicking SearchBar
        searchBar.setOnClickListener {
            searchView.show()
        }

        // Handle search text changes
        searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCategories(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadProductsAndCount() {
        val productsRef = database.getReference("Foods")
        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productCountMap.clear()

                // Đếm số sản phẩm cho mỗi category
                for (productSnapshot in snapshot.children) {
                    val categoryId = productSnapshot.child("CategoryId").getValue(Int::class.java)

                    // Chỉ đếm nếu categoryId hợp lệ (khác null và >= 0)
                    categoryId?.let {
                        if (it >= 0) { // Đảm bảo categoryId không âm
                            productCountMap[it] = productCountMap.getOrDefault(it, 0) + 1
                        }
                    }
                }

                // Kiểm tra debug
                productCountMap.forEach { (categoryId, count) ->
                    Log.d("ProductCount", "Category $categoryId: $count sản phẩm")
                }

                // Cập nhật adapter
                categoryAdapter.updateCategories(ArrayList(categoriesList), productCountMap)

                // Trong onDataChange của loadProductsAndCount
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error counting products: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


private fun loadCategories() {
    // Load categories như bình thường
    categoriesRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            categoriesList.clear()
            for (categorySnapshot in snapshot.children) {
                val category = categorySnapshot.getValue(Category::class.java)
                category?.Id = categorySnapshot.key?.toIntOrNull() ?: 0
                category?.let { categoriesList.add(it) }
            }

            // Sau khi load xong categories, load products để đếm
            loadProductsAndCount()
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

    private fun filterCategories(query: String) {
        val filteredList = if (query.isEmpty()) {
            categoriesList
        } else {
            categoriesList.filter {
                it.Name.contains(query, ignoreCase = true)
            }
        }
        // Truyền cả productCountMap hiện tại
        categoryAdapter.updateCategories(ArrayList(filteredList), productCountMap)
    }
}
