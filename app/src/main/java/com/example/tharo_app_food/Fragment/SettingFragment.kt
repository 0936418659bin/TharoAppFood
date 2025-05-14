package com.example.tharo_app_food.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.example.tharo_app_food.Activity.BaseActivity
import com.example.tharo_app_food.Activity.LoginActivity
import com.example.tharo_app_food.R
import com.google.firebase.auth.FirebaseAuth

class SettingFragment: Fragment() {

    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        btnLogout = view.findViewById(R.id.btnLogout)

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            clearLoginSession()
            Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show()

            // Điều hướng về màn hình đăng nhập
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }

    private fun clearLoginSession() {
        val sharedPreferences = requireContext().getSharedPreferences("user_session", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}

