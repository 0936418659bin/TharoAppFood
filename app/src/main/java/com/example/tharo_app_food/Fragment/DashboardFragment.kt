import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.tharo_app_food.Domain.DeleteRequest
import com.example.tharo_app_food.Domain.ImageKitService
import com.example.tharo_app_food.Domain.User
import com.example.tharo_app_food.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: FirebaseDatabase

    private lateinit var userAvatar: ImageView
    private lateinit var btnAddAvatar: ImageButton
    private lateinit var nameText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        nameText = view.findViewById(R.id.name_text)
        btnAddAvatar = view.findViewById(R.id.btn_add_avatar)
        userAvatar = view.findViewById(R.id.user_avatar)

        userAvatar.setOnClickListener {
            openImagePicker()
        }
        sharedPreferences = requireContext().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app")

        setupStatsCards(view)
        setupUserInfo()
        setupLineChart(view)
        setupPieChart(view)

        btnAddAvatar.setOnClickListener {
            openImagePicker()
        }

        return view
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

    private fun setupUserInfo() {
        val userName = sharedPreferences.getString("user_name", "Admin")
        nameText.text = userName ?: "Admin"

        loadUserAvatar()
    }

    private fun loadUserAvatar() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { userId ->
            database.getReference("Users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.Avatar?.let { avatarUrl ->
                            if (avatarUrl.isNotEmpty()) {
                                Glide.with(this@DashboardFragment)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .into(userAvatar)
                                btnAddAvatar.visibility = View.GONE
                            } else {
                                userAvatar.setImageResource(R.drawable.default_user)
                                btnAddAvatar.visibility = View.VISIBLE
                            }
                        } ?: run {
                            userAvatar.setImageResource(R.drawable.default_user)
                            btnAddAvatar.visibility = View.VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to load avatar", Toast.LENGTH_SHORT).show()
                    }
                })
        } ?: run {
            userAvatar.setImageResource(R.drawable.default_user)
            btnAddAvatar.visibility = View.VISIBLE
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(userAvatar)

        btnAddAvatar.visibility = View.GONE
        changeUserAvatar(uri)
    }

    private fun changeUserAvatar(newAvatarUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        database.getReference("Users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    val oldAvatarUrl = user?.Avatar
                    val oldImageId = user?.AvatarId

                    if (!oldAvatarUrl.isNullOrEmpty() && !oldImageId.isNullOrEmpty()) {
                        deleteOldAvatar(oldImageId) { isSuccess ->
                            if (isSuccess) {
                                uploadImageToImageKit(newAvatarUri)
                            } else {
                                activity?.runOnUiThread {
                                    Toast.makeText(
                                        context,
                                        "Failed to delete old avatar, but will continue with new upload",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    uploadImageToImageKit(newAvatarUri)
                                }
                            }
                        }
                    } else {
                        uploadImageToImageKit(newAvatarUri)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    showAddAvatarButton()
                }
            })
    }

    private fun deleteOldAvatar(imageId: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deleteResponse = imageKitService.deleteImage(DeleteRequest(imageId))
                if (deleteResponse.isSuccessful) {
                    deleteResponse.body()?.let {
                        if (it.success) {
                            Log.d("DeleteImage", "Deleted old avatar successfully")
                            callback(true)
                            return@launch
                        }
                    }
                }
                Log.e("DeleteImage", "Failed to delete old avatar: ${deleteResponse.errorBody()?.string()}")
                callback(false)
            } catch (e: Exception) {
                Log.e("DeleteImage", "Error deleting old avatar", e)
                callback(false)
            }
        }
    }

    private fun uploadImageToImageKit(uri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                val tempFile = File.createTempFile(
                    "avatar_${currentUser.uid}_${System.currentTimeMillis()}",
                    ".jpg",
                    requireContext().cacheDir
                ).apply {
                    outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                }

                val fileRequestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    tempFile.name,
                    fileRequestBody
                )

                val fileNamePart = "avatar_${currentUser.uid}_${System.currentTimeMillis()}.jpg"
                    .toRequestBody("text/plain".toMediaType())
                val folderPart = "/user_avatars/"
                    .toRequestBody("text/plain".toMediaType())

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = imageKitService.uploadImage(
                            file = filePart,
                            fileName = fileNamePart,
                            folder = folderPart
                        )

                        withContext(Dispatchers.Main) {
                            // Cập nhật cả Avatar và ImageId
                            saveAvatarInfoToDatabase(
                                userId = currentUser.uid,
                                imageUrl = response.url,
                                fileId = response.fileId
                            )
                            updateAvatarUI(response.url)
                            Toast.makeText(
                                requireContext(),
                                "Avatar updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showAddAvatarButton()
                            Toast.makeText(
                                requireContext(),
                                "Failed to upload avatar: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } finally {
                        tempFile.delete()
                    }
                }
            } ?: run {
                Toast.makeText(requireContext(), "Failed to read image data", Toast.LENGTH_SHORT).show()
                showAddAvatarButton()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            showAddAvatarButton()
        }
    }

    private fun updateAvatarUI(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .circleCrop()
            .into(userAvatar)
    }

    private fun showAddAvatarButton() {
        btnAddAvatar.visibility = View.VISIBLE
    }

    private fun saveAvatarInfoToDatabase(userId: String, imageUrl: String, fileId: String) {
        val userRef = database.getReference("Users").child(userId)

        val updates = hashMapOf<String, Any>(
            "Avatar" to imageUrl,
            "AvatarId" to fileId
        )

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("SaveAvatar", "Avatar info updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("SaveAvatar", "Failed to save avatar info", e)
                Toast.makeText(context, "Failed to save avatar info", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }



    private fun setupStatsCards(view: View) {
        val totalUsersText = view.findViewById<TextView>(R.id.total_users_text)
        val totalProductsText = view.findViewById<TextView>(R.id.total_products_text)

        // Lấy tổng số người dùng có role là "User"
        database.getReference("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userCount = 0
                for (userSnapshot in snapshot.children) {
                    val role = userSnapshot.child("Role").getValue(String::class.java)
                    if (role == "User") {
                        userCount++
                    }
                }
                totalUsersText.text = userCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                totalUsersText.text = "0"
            }
        })

        // Lấy tổng số sản phẩm
        database.getReference("Foods").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productCount = snapshot.childrenCount
                totalProductsText.text = productCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                totalProductsText.text = "0"
            }
        })
    }



    private fun setupLineChart(view: View) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChart)

        // Sample data
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 100f))
        entries.add(Entry(1f, 200f))
        entries.add(Entry(2f, 150f))
        entries.add(Entry(3f, 300f))
        entries.add(Entry(4f, 280f))
        entries.add(Entry(5f, 400f))
        entries.add(Entry(6f, 350f))

        val lineDataSet = LineDataSet(entries, "User Activity")
        lineDataSet.color = Color.parseColor("#6200EE")
        lineDataSet.valueTextColor = Color.BLACK
        lineDataSet.lineWidth = 2f
        lineDataSet.setCircleColor(Color.parseColor("#6200EE"))
        lineDataSet.circleRadius = 5f
        lineDataSet.setDrawCircleHole(false)
        lineDataSet.valueTextSize = 10f

        val lineData = LineData(lineDataSet)
        lineChart.data = lineData

        // Customize chart
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisRight.isEnabled = false

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(getWeekDays())

        val yAxis = lineChart.axisLeft
        yAxis.setDrawGridLines(false)

        lineChart.animateXY(1000, 1000)
    }

    private fun setupPieChart(view: View) {
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(40f, "Gà"))
        entries.add(PieEntry(30f, "Pizza"))
        entries.add(PieEntry(20f, "Burger"))
        entries.add(PieEntry(10f, "Other"))

        val dataSet = PieDataSet(entries, "Platform Distribution")
        dataSet.colors = listOf(
            Color.parseColor("#6200EE"),
            Color.parseColor("#03DAC5"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF5722")
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        pieChart.data = pieData

        // Customize chart
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.legend.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleAlpha(0)
        pieChart.animateY(1000)
    }

    private fun getWeekDays(): Array<String> {
        val calendar = Calendar.getInstance()
        val days = Array(7) { "" }

        val dateFormat = android.text.format.DateFormat.getDateFormat(context)
        for (i in 0 until 7) {
            calendar.add(Calendar.DAY_OF_WEEK, -6 + i)
            days[i] = android.text.format.DateFormat.format("EEE", calendar).toString()
        }

        return days
    }
}