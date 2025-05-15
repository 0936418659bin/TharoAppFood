package com.example.tharo_app_food.Activity


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Adapter.BestFoodAdapter
import com.example.tharo_app_food.Adapter.CategoryAdapter
import com.example.tharo_app_food.Config.GridSpacingItemDecoration
import com.example.tharo_app_food.Domain.AuthRequest
import com.example.tharo_app_food.Domain.AuthResponse
import com.example.tharo_app_food.Domain.Category
import com.example.tharo_app_food.Domain.DeleteRequest
import com.example.tharo_app_food.Domain.Foods
import com.example.tharo_app_food.Domain.ImageKitService
import com.example.tharo_app_food.Domain.Location
import com.example.tharo_app_food.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.example.tharo_app_food.Domain.Price
import com.example.tharo_app_food.Domain.Time
import com.example.tharo_app_food.Domain.User
import com.example.tharo_app_food.Helper.ManagementCart
import com.example.tharo_app_food.R
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.imagekit.android.ImageKit
import com.imagekit.android.ImageKitCallback
import com.imagekit.android.entity.TransformationPosition
import com.imagekit.android.entity.UploadError
import com.imagekit.android.entity.UploadPolicy
import com.imagekit.android.entity.UploadResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Xử lý khi chọn ảnh thành công
            handleSelectedImage(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        ImageKit.init(
            context = applicationContext,
            publicKey = "public_mohYA+PnccoVCPKjMsCPtk3jCjQ=",
            urlEndpoint = "https://ik.imagekit.io/thaongc6302",
            transformationPosition = TransformationPosition.PATH,
            defaultUploadPolicy = UploadPolicy.Builder()
                .requireNetworkType(UploadPolicy.NetworkType.ANY)
                .build()
        )


        initNavigationDrawer()
        initLocation()
        initTime()
        initPrice()
        initBestFood()
        initCategory()
        checkLoginStatus()
        setVariable()
    }

    private fun setVariable() {
        binding.cartBtn.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }
        binding.searchBtn.setOnClickListener {
            val text = binding.searchEdt.text.toString()
            if (text.isNotEmpty()) {
                val intent = Intent(this@MainActivity, ListFoodActivity::class.java)
                intent.putExtra("text", text)
                intent.putExtra("isSearch", true)
                startActivity(intent)
            }
        }
    }


    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.15:3000/") // Thay bằng URL server Node.js
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val imageKitService by lazy {
        retrofit.create(ImageKitService::class.java)
    }

    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleSelectedImage(uri: Uri) {
        // Hiển thị ảnh đã chọn tạm thời
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val imgAvatar = headerView.findViewById<ImageView>(R.id.img_avatar)

        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(imgAvatar)

        // Ẩn nút thêm avatar tạm thời
        val btnAddAvatar = headerView.findViewById<ImageButton>(R.id.btn_add_avatar)
        btnAddAvatar.visibility = View.GONE

        // Bắt đầu quá trình thay đổi avatar
        changeUserAvatar(uri)
    }

    private fun changeUserAvatar(newAvatarUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Lấy thông tin user để kiểm tra có avatar cũ không
        FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("Users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.AvatarId?.let { oldImageId ->
                        // Nếu có avatar cũ, xóa nó trước
                        deleteOldAvatar(oldImageId) {
                            // Sau khi xóa xong, upload avatar mới
                            uploadImageToImageKit(newAvatarUri)
                        }
                    } ?: run {
                        // Nếu không có avatar cũ, upload luôn
                        uploadImageToImageKit(newAvatarUri)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    showAddAvatarButton()
                }
            })
    }

    private fun deleteOldAvatar(imageId: String, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deleteResponse = imageKitService.deleteImage(DeleteRequest(imageId))
                if (deleteResponse.isSuccessful) {
                    Log.d("DeleteImage", "Deleted old avatar successfully")
                } else {
                    Log.e("DeleteImage", "Failed to delete old avatar: ${deleteResponse.message()}")
                }
            } catch (e: Exception) {
                Log.e("DeleteImage", "Error deleting old avatar", e)
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    private fun loadUserAvatar(imgAvatar: ImageView, btnAddAvatar: ImageButton) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { userId ->
            FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("Users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.Avatar?.let { avatarUrl ->
                            if (avatarUrl.isNotEmpty()) {
                                Glide.with(this@MainActivity)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .into(imgAvatar)
                                btnAddAvatar.visibility = View.GONE
                            } else {
                                imgAvatar.setImageResource(R.drawable.default_user)
                                btnAddAvatar.visibility = View.VISIBLE
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainActivity, "Failed to load avatar", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
    private fun uploadImageToImageKit(uri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Log.e("UploadImage", "User not logged in")
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("UploadImage", "Starting image upload for user: ${currentUser.uid}")

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                Log.d("UploadImage", "Opened input stream for URI: $uri")

                // Tạo file tạm thời để upload
                val tempFile = File.createTempFile(
                    "avatar_${currentUser.uid}_${System.currentTimeMillis()}",
                    ".jpg",
                    cacheDir
                ).apply {
                    outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                }

                Log.d("UploadImage", "Created temp file: ${tempFile.absolutePath}")

                // Tạo request body cho file
                val fileRequestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    tempFile.name,
                    fileRequestBody
                )

                // Tạo request body cho các tham số khác
                val fileNamePart = "avatar_${currentUser.uid}_${System.currentTimeMillis()}.jpg"
                    .toRequestBody("text/plain".toMediaType())
                val folderPart = "/user_avatars/"
                    .toRequestBody("text/plain".toMediaType())

                Log.d("UploadImage", "Prepared all multipart parts")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d("UploadImage", "Calling imageKitService.uploadImage...")

                        val response = imageKitService.uploadImage(
                            file = filePart,
                            fileName = fileNamePart,
                            folder = folderPart
                        )

                        Log.d("UploadImage", "Upload successful. Image URL: ${response.url}, File ID: ${response.fileId}")

                        withContext(Dispatchers.Main) {
                            updateAvatarUI(response.url)
                            saveAvatarInfoToDatabase(currentUser.uid, response.url, response.fileId)
                            Toast.makeText(
                                this@MainActivity,
                                "Avatar updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("UploadImage", "Upload failed", e)

                        val errorMessage = when (e) {
                            is HttpException -> {
                                val errorBody = e.response()?.errorBody()?.string()
                                "Server error: ${e.code()}, $errorBody"
                            }
                            is IOException -> "Network error: ${e.message}"
                            else -> "Error: ${e.message}"
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to upload avatar: $errorMessage",
                                Toast.LENGTH_LONG
                            ).show()
                            showAddAvatarButton()
                        }
                    } finally {
                        // Xóa file tạm dù thành công hay thất bại
                        try {
                            tempFile.delete()
                            Log.d("UploadImage", "Deleted temp file: ${tempFile.absolutePath}")
                        } catch (e: Exception) {
                            Log.e("UploadImage", "Error deleting temp file", e)
                        }
                    }
                }
            } ?: run {
                Log.e("UploadImage", "InputStream is null for URI: $uri")
                Toast.makeText(this, "Failed to read image data", Toast.LENGTH_SHORT).show()
                showAddAvatarButton()
            }
        } catch (e: Exception) {
            Log.e("UploadImage", "Error in upload process", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            showAddAvatarButton()
        }
    }


    private fun updateAvatarUI(imageUrl: String) {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val imgAvatar = headerView.findViewById<ImageView>(R.id.img_avatar)

        Glide.with(this)
            .load(imageUrl)
            .circleCrop()
            .into(imgAvatar)
    }

    private fun showAddAvatarButton() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val btnAddAvatar = headerView.findViewById<ImageButton>(R.id.btn_add_avatar)
        btnAddAvatar.visibility = View.VISIBLE
    }

    private fun saveAvatarInfoToDatabase(userId: String, imageUrl: String, fileId: String) {
        val userRef = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("Users").child(userId)

        // Tạo map chứa cả URL và ID của ảnh
        val updates = mapOf(
            "Avatar" to imageUrl,
            "ImageId" to fileId
        )

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Avatar updated successfully", Toast.LENGTH_SHORT).show()

                // Ẩn nút thêm avatar sau khi upload thành công
                runOnUiThread {
                    val navView: NavigationView = findViewById(R.id.nav_view)
                    val headerView = navView.getHeaderView(0)
                    val btnAddAvatar = headerView.findViewById<ImageButton>(R.id.btn_add_avatar)
                    btnAddAvatar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save avatar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun initNavHeader() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val imgAvatar = headerView.findViewById<ImageView>(R.id.img_avatar)
        val btnAddAvatar = headerView.findViewById<ImageButton>(R.id.btn_add_avatar)
        val tvUsername = headerView.findViewById<TextView>(R.id.tv_username)
        val tvEmail = headerView.findViewById<TextView>(R.id.tv_email)
        val btnLogin = headerView.findViewById<Button>(R.id.btn_login)

        val isLoggedIn = checkIfUserIsLoggedIn()

        if (!isLoggedIn) {
            tvUsername.visibility = View.GONE
            tvEmail.visibility = View.GONE
            btnLogin.visibility = View.VISIBLE
            imgAvatar.setImageResource(R.drawable.default_user)
            btnAddAvatar.visibility = View.GONE
            btnLogin.setOnClickListener {
                navigateToLoginScreen()
            }
        } else {
            tvUsername.visibility = View.VISIBLE
            tvEmail.visibility = View.VISIBLE
            btnLogin.visibility = View.GONE

            val userName = getUserName()
            val email = getEmail()

            tvUsername.text = userName
            tvEmail.text = email

            // Load avatar nếu có
            loadUserAvatar(imgAvatar, btnAddAvatar)

            // Xử lý sự kiện click nút thêm avatar
            btnAddAvatar.setOnClickListener {
                openImagePicker()
            }

            // Xử lý sự kiện click vào avatar
            imgAvatar.setOnClickListener {
                openImagePicker()
            }
        }
    }


    private fun initNavigationDrawer() {

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Thiết lập listener cho navigation view
        navView.setNavigationItemSelectedListener(this)

        // Thiết lập toggle cho drawer
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Xử lý sự kiện click nút setting để mở drawer
        binding.setting.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        initNavHeader()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Xử lý khi chọn item trong menu
        when (item.itemId) {
            R.id.nav_profile -> {
                // Mở màn hình profile
//                startActivity(Intent(this, ProfileActivity::class.java))
            }
            R.id.nav_orders -> {
                // Mở màn hình đơn hàng
//                startActivity(Intent(this, OrdersActivity::class.java))
            }
            R.id.nav_favorites -> {
                // Mở màn hình yêu thích
//                startActivity(Intent(this, FavoritesActivity::class.java))
            }
            R.id.nav_settings -> {
                // Mở màn hình cài đặt
//                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.nav_logout -> {
                // Đăng xuất
                performLogout()
            }
        }

        // Đóng drawer sau khi chọn
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun checkLoginStatus() {
        // Giả sử bạn có một phương thức kiểm tra đăng nhập
        val isLoggedIn = checkIfUserIsLoggedIn()

        if (isLoggedIn) {
            // Hiển thị tên người dùng
            binding.welcome.visibility = View.VISIBLE
            binding.logout.visibility = View.VISIBLE
            binding.nameUser.visibility = View.VISIBLE

            // Lấy tên người dùng từ SharedPreferences hoặc từ server
            val userName = getUserName()
            binding.nameUser.text = userName

            binding.logout.setOnClickListener{
                performLogout()
            }
        } else {
            // Hiển thị nút đăng nhập
            binding.nameUser.visibility = View.GONE
            binding.welcome.visibility = View.GONE
            binding.logout.visibility = View.GONE
        }
    }

    private fun checkIfUserIsLoggedIn(): Boolean {
        // Kiểm tra trạng thái đăng nhập, ví dụ từ SharedPreferences
        // Trả về true nếu đã đăng nhập, ngược lại trả về false
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    private fun getUserName(): String {
        // Lấy tên người dùng từ SharedPreferences hoặc từ server
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        return sharedPreferences.getString("user_name", "User") ?: "User"
    }

    private fun getEmail(): String {

        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val email = sharedPreferences.getString("user_email", "Email")
        Log.d("DEBUG_EMAIL", "Email lấy được: $email")
        return email ?: "Email"
    }

    private fun navigateToLoginScreen() {
        // Điều hướng đến màn hình đăng nhập
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        ManagementCart(this).clear()
        clearLoginSession()
        // Điều hướng đến màn hình đăng nhập
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Đóng activity hiện tại
    }

    fun initCategory() {
        println("🔥 initCategory() has been called")
        val myref: DatabaseReference = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("Category")

        binding.progressBarCategory.visibility = View.VISIBLE
        val list: ArrayList<Category> = ArrayList()

        myref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("🔥 DataSnapshot toàn bộ: ${snapshot.value}")
                if (snapshot.exists()) {
                    println("Snapshot exists: ${snapshot.childrenCount} items found")

                    for (issu in snapshot.children) {
                        val cateItem = issu.getValue(Category::class.java)
                        if (cateItem != null) {
                            list.add(cateItem)
                            println("Cate item added: $cateItem")
                        } else {
                            println("Null Cate item encountered")
                        }
                    }

                    if (list.isNotEmpty()) {
                        // Thay đổi LinearLayoutManager thành GridLayoutManager
                        binding.categoryView.layoutManager = GridLayoutManager(
                            this@MainActivity, 2, GridLayoutManager.HORIZONTAL, false
                        )
//                        binding.categoryView.addItemDecoration(
//                            GridSpacingItemDecoration(4, 72, true) // 4 cột, khoảng cách 24dp
//                        )
                        val adapterCategory = CategoryAdapter(list)
                        binding.categoryView.adapter = adapterCategory
                        println("Adapter set with ${list.size} items")
                    } else {
                        println("List is empty, no items to display")
                    }

                    binding.progressBarCategory.visibility = View.GONE
                } else {
                    println("Snapshot does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
            }
        })
    }


    fun initBestFood() {
        println("🔥 initLocation() has been called")
        val myref: DatabaseReference = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Foods")
        binding.progressBarBestFood.visibility = View.VISIBLE
        val list: ArrayList<Foods> = ArrayList()
        val query: Query = myref.orderByChild("BestFood").equalTo(true)

        println( "Querying database for BestFood items..."+query)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("🔥 DataSnapshot toàn bộ: ${snapshot.value}")
                if (snapshot.exists()) {
                    println("Snapshot exists: ${snapshot.childrenCount} items found")

                    for (issu in snapshot.children) {
                        val foodItem = issu.getValue(Foods::class.java)
                        if (foodItem != null) {
                            list.add(foodItem)
                            println("Food item added: $foodItem")
                        } else {
                            println("Null food item encountered")
                        }
                    }
                    if (list.isNotEmpty()) {
                        binding.bestFoodView.layoutManager = LinearLayoutManager(
                            this@MainActivity,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        val adapterBestFood = BestFoodAdapter(list)
                        binding.bestFoodView.adapter = adapterBestFood
                        println("Adapter set with ${list.size} items")
                    } else {
                        println( "List is empty, no items to display")
                    }

                    binding.progressBarBestFood.visibility = View.GONE
                } else {
                    println( "Snapshot does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
            }
        })
    }


    fun initLocation() {
        val myref: DatabaseReference = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Location")

        val list: ArrayList<Location> = ArrayList()

        myref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    println("Firebase: Dữ liệu nhận được: ${snapshot.value}") // Debug dữ liệu từ Firebase

                    for (issue in snapshot.children) {
                        val location = issue.getValue(Location::class.java)
                        if (location != null) {
                            list.add(location)
                            println("Firebase: Thêm vào list: $location") // Debug từng phần tử
                        }
                    }

                    val adapter: ArrayAdapter<Location> = ArrayAdapter(
                        this@MainActivity,
                        R.layout.sp_item,
                        list // Gán dữ liệu vào adapter
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.localtionSp.adapter = adapter

                    println("Firebase: Adapter đã được set thành công")
                } else {
                    println("Firebase: Không tìm thấy dữ liệu!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase: Lỗi khi lấy dữ liệu: ${error.message}")
            }
        })
    }

    fun initTime() {
        val myref: DatabaseReference = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Time")


        val list: ArrayList<Time> = ArrayList()

        myref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (issue in snapshot.children) {
                        val location = issue.getValue(Time::class.java)
                        if (location != null) {
                            list.add(location)
                        }
                    }

                    val adapter: ArrayAdapter<Time> = ArrayAdapter(
                        this@MainActivity,
                        R.layout.sp_item,
                        list // Gán dữ liệu vào adapter
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.TimeSp.adapter = adapter

                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase: Lỗi khi lấy dữ liệu: ${error.message}")
            }
        })
    }

    fun initPrice() {
        val myref: DatabaseReference = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Price")

        val list: ArrayList<Price> = ArrayList()

        myref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (issue in snapshot.children) {
                        val location = issue.getValue(Price::class.java)
                        if (location != null) {
                            list.add(location)
                        }
                    }

                    val adapter: ArrayAdapter<Price> = ArrayAdapter(
                        this@MainActivity,
                        R.layout.sp_item,
                        list // Gán dữ liệu vào adapter
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.DollarSp.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase: Lỗi khi lấy dữ liệu: ${error.message}")
            }
        })
    }

}


