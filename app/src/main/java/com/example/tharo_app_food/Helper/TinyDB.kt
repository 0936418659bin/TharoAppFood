package com.example.tharo_app_food.helper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import com.example.tharo_app_food.Domain.Foods
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TinyDB(context: Context) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var defaultAppImageDataDirectory: String = ""
    private var lastImagePath: String = ""

    fun getImage(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSavedImagePath(): String {
        return lastImagePath
    }

    fun putImage(folder: String, imageName: String, bitmap: Bitmap): String? {
        defaultAppImageDataDirectory = folder
        val fullPath = setupFullPath(imageName)
        return if (fullPath.isNotEmpty()) {
            lastImagePath = fullPath
            if (saveBitmap(fullPath, bitmap)) fullPath else null
        } else null
    }

    fun putImageWithFullPath(fullPath: String, bitmap: Bitmap): Boolean {
        return fullPath.isNotEmpty() && saveBitmap(fullPath, bitmap)
    }

    private fun setupFullPath(imageName: String): String {
        val folder = File(Environment.getExternalStorageDirectory(), defaultAppImageDataDirectory)
        return if (isExternalStorageReadable() && isExternalStorageWritable() && !folder.exists() && !folder.mkdirs()) {
            Log.e("ERROR", "Failed to setup folder")
            ""
        } else {
            "${folder.path}/$imageName"
        }
    }

    private fun saveBitmap(fullPath: String, bitmap: Bitmap): Boolean {
        val imageFile = File(fullPath)
        if (imageFile.exists()) imageFile.delete()

        return try {
            imageFile.createNewFile()
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun getInt(key: String): Int = preferences.getInt(key, 0)
    fun getLong(key: String): Long = preferences.getLong(key, 0)
    fun getFloat(key: String): Float = preferences.getFloat(key, 0f)
    fun getString(key: String): String = preferences.getString(key, "") ?: ""
    fun getBoolean(key: String): Boolean = preferences.getBoolean(key, false)

    fun getListString(key: String): ArrayList<String> {
        return ArrayList(TextUtils.split(preferences.getString(key, "") ?: "", "‚‗‚").toList())
    }

    fun getListObject(key: String): ArrayList<Foods> {
        val gson = Gson()
        val objStrings = getListString(key)
        val objList = ArrayList<Foods>()
        for (json in objStrings) {
            val obj = gson.fromJson(json, Foods::class.java)
            objList.add(obj)
        }
        return objList
    }

    fun <T> getObject(key: String, classOfT: Class<T>): T? {
        val json = getString(key)
        return Gson().fromJson(json, classOfT)
    }

    fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    fun putFloat(key: String, value: Float) {
        preferences.edit().putFloat(key, value).apply()
    }

    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    fun putListString(key: String, stringList: ArrayList<String>) {
        preferences.edit().putString(key, TextUtils.join("‚‗‚", stringList)).apply()
    }

    fun putListObject(key: String, objList: ArrayList<Foods>) {
        val gson = Gson()
        val objStrings = objList.map { gson.toJson(it) } as ArrayList<String>
        putListString(key, objStrings)
    }

    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        fun isExternalStorageWritable(): Boolean {
            return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        }

        fun isExternalStorageReadable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }
    }
}
