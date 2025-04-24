package com.example.tharo_app_food.Helper

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.tharo_app_food.Activity.BaseActivity
import com.example.tharo_app_food.Domain.Foods

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ManagementCart(private val context: Context) {
    private val TAG = "ManagementCart"
    private val auth = FirebaseAuth.getInstance()
    private val databases = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Carts")
    private var currentUserId: String? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        currentUserId = firebaseAuth.currentUser?.uid
        Log.d("AuthState", "User ID changed: $currentUserId (User exists: ${firebaseAuth.currentUser != null})")
    }

    init {
        // Thêm listener để theo dõi thay đổi trạng thái đăng nhập
        auth.addAuthStateListener(authStateListener)
        currentUserId = auth.currentUser?.uid
        Log.d(TAG, "Initialized with authStateListener")

    }

    fun clear() {
        Log.d(TAG, "Clearing cart management")
        auth.removeAuthStateListener(authStateListener)
        currentUserId = null
    }

    fun insertFood(item: Foods, callback: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "DENIED - No user logged in")
            Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        Log.d( TAG, "User authenticated. UID: $currentUserId")

        // Tạo key nếu chưa có
        val foodKey = if (item.Key.isEmpty()) item.generateKey() else item.Key

        databases.child(userId).child(foodKey).setValue(item)
            .addOnSuccessListener {
                Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                callback(true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    fun getListCart(callback: (ArrayList<Foods>) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(arrayListOf())
            return
        }

        databases.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val list = arrayListOf<Foods>()
                for (data in snapshot.children) {
                    val item = data.getValue(Foods::class.java)
                    item?.let {
                        it.Key = data.key ?: "" // Lưu lại key của item
                        list.add(it)
                    }
                }
                callback(list)
            }
            .addOnFailureListener {
                callback(arrayListOf())
            }
    }

    fun updateCart(listItem: ArrayList<Foods>, callback: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false)
            return
        }

        // Xóa toàn bộ giỏ hàng cũ và cập nhật lại
        databases.child(userId).removeValue().addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                val updateTasks = listItem.map { item ->
                    val foodKey = if (item.Key.isEmpty()) item.generateKey() else item.Key
                    databases.child(userId).child(foodKey).setValue(item)
                }

                Tasks.whenAllSuccess<Void>(updateTasks)
                    .addOnSuccessListener {
                        callback(true)
                    }
                    .addOnFailureListener {
                        callback(false)
                    }
            } else {
                callback(false)
            }
        }
    }

    fun removeItem(item: Foods, callback: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false)
            return
        }

        if (item.Key.isEmpty()) {
            callback(false)
            return
        }

        databases.child(userId).child(item.Key).removeValue()
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}