package com.example.tharo_app_food.Helper

import android.content.Context
import android.widget.Toast
import com.example.tharo_app_food.Activity.BaseActivity
import com.example.tharo_app_food.Domain.Foods

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ManagementCart(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://tharo-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Carts")

    fun insertFood(item: Foods, callback: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        // Tạo key nếu chưa có
        val foodKey = if (item.Key.isEmpty()) item.generateKey() else item.Key

        database.child(userId).child(foodKey).setValue(item)
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

        database.child(userId).get()
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
        database.child(userId).removeValue().addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                val updateTasks = listItem.map { item ->
                    val foodKey = if (item.Key.isEmpty()) item.generateKey() else item.Key
                    database.child(userId).child(foodKey).setValue(item)
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

        database.child(userId).child(item.Key).removeValue()
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}