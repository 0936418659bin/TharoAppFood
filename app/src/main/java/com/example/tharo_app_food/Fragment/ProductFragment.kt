package com.example.tharo_app_food.Fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tharo_app_food.Adapter.ProductAdapter
import com.example.tharo_app_food.Config.GridSpacingItemDecoration
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.Filter.ProductFilterDialog
import com.example.tharo_app_food.Dialog.AddProductDialog
import com.example.tharo_app_food.Dialog.ProductDetailDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.search.SearchBar
import com.google.firebase.database.*
import com.example.tharo_app_food.R
import com.google.android.material.search.SearchView

class ProductFragment : Fragment() {

    private lateinit var searchBar: SearchBar
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var fabAddProduct: ExtendedFloatingActionButton
    private lateinit var productAdapter: ProductAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var foodsRef: DatabaseReference
    private lateinit var searchView: SearchView
    private var foodsList = mutableListOf<Foods>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo Firebase
        database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
        foodsRef = database.getReference("Foods")

        initViews(view)
        setupRecyclerView()
        setupSearchBar()
        setupChipGroup()
        setupFab()
        loadProductsFromFirebase()
    }

    private fun initViews(view: View) {
        searchBar = view.findViewById(R.id.searchBar)
        searchView = view.findViewById(R.id.searchView)
        chipGroup = view.findViewById(R.id.chipGroup)
        recyclerView = view.findViewById(R.id.foodListView)
        progressBar = view.findViewById(R.id.progressBar)
        fabAddProduct = view.findViewById(R.id.fabAddProduct)
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(foodsList) { food ->
            showProductDetail(food)
        }

        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2) // 2 cột
            adapter = productAdapter
            addItemDecoration(
                GridSpacingItemDecoration(
                    2,
                    resources.getDimensionPixelSize(R.dimen.grid_spacing),
                    true
                )
            )
        }
    }

    private fun setupSearchBar() {
        searchBar.inflateMenu(R.menu.product_search_menu)
        searchBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_filter -> {
                    showAdvancedFilterDialog()
                    true
                }
                else -> false
            }
        }

        // Mở SearchView khi click vào SearchBar
        searchBar.setOnClickListener {
            searchView.show()
        }

        // Lắng nghe sự thay đổi nội dung từ ô nhập của SearchView
        searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun setupChipGroup() {
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = group.findViewById<Chip>(checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener)
            when (chip?.text) {
                "Tất cả" -> filterProducts("")
                "Phổ biến" -> filterByBestFood(true)
                "Mới nhất" -> sortByNewest()
                "Bán chạy" -> sortByPopular()
            }
        }
    }

    private fun setupFab() {
        fabAddProduct.setOnClickListener {
            showAddProductDialog()
        }
    }

    private fun loadProductsFromFirebase() {
        progressBar.visibility = View.VISIBLE
        foodsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodsList.clear()
                for (foodSnapshot in snapshot.children) {
                    val food = foodSnapshot.getValue(Foods::class.java)
                    food?.Key = foodSnapshot.key ?: ""
                    food?.let { foodsList.add(it) }
                }
                productAdapter.updateProducts(foodsList)
                progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterProducts(query: String) {
        val filteredList = if (query.isEmpty()) {
            foodsList
        } else {
            foodsList.filter {
                it.Title.contains(query, ignoreCase = true) ||
                        it.Description.contains(query, ignoreCase = true)
            }
        }
        productAdapter.updateProducts(filteredList)
    }

    private fun filterByBestFood(isBest: Boolean) {
        val filteredList = foodsList.filter { it.BestFood == isBest }
        productAdapter.updateProducts(filteredList)
    }

    private fun sortByNewest() {
        val sortedList = foodsList.sortedByDescending { it.TimeValue }
        productAdapter.updateProducts(sortedList)
    }

    private fun sortByPopular() {
        val sortedList = foodsList.sortedByDescending { it.Star }
        productAdapter.updateProducts(sortedList)
    }

    private fun showProductDetail(food: Foods) {
        val dialog = ProductDetailDialog.newInstance(food)
        dialog.show(parentFragmentManager, "ProductDetailDialog")
    }

    private fun showAddProductDialog() {
        AddProductDialog().apply {
            setOnProductAddedListener { newFood ->
                addFoodToFirebase(newFood)
            }
        }.show(parentFragmentManager, "AddProductDialog")
    }

    private fun addFoodToFirebase(food: Foods) {
        val foodKey = food.generateKey()
        foodsRef.child(foodKey).setValue(food)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi khi thêm sản phẩm", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAdvancedFilterDialog() {
        ProductFilterDialog().apply {
            setOnFilterAppliedListener { minPrice, maxPrice, minRating ->
                val filtered = foodsList.filter { food ->
                    food.Price >= minPrice &&
                            food.Price <= maxPrice &&
                            food.Star >= minRating
                }
                productAdapter.updateProducts(filtered)
            }
        }.show(parentFragmentManager, "ProductFilterDialog")
    }
}