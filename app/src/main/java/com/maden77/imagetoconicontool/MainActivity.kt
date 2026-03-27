package com.maden77.imagetoconicontool

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedImageBitmap: Bitmap? = null
    private val REQUEST_PERMISSION = 100
    private val REQUEST_IMAGE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sizes = arrayOf("16x16", "32x32", "64x64", "128x128", "256x256")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sizes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sizeSpinner.adapter = adapter

        binding.btnSelectImage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Aktifkan izin file penuh di pengaturan!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
                } else openImagePicker()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION)
                } else openImagePicker()
            }
        }

        binding.btnConvert.setOnClickListener {
            val selectedSize = binding.sizeSpinner.selectedItem.toString()
            val (width, height) = selectedSize.split("x").map { it.toInt() }
            
            if (selectedImageBitmap == null) {
                Toast.makeText(this, "Pilih gambar dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val icon = Bitmap.createScaledBitmap(selectedImageBitmap!!, width, height, true)
                saveIcon(icon, selectedSize)
                Toast.makeText(this, "✅ Ikon disimpan di Pictures/ImageToIconTool", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnOpenLocation.setOnClickListener {
            val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ImageToIconTool")
            if (!folder.exists()) folder.mkdirs()
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(this, "${packageName}.fileprovider", folder), "resource/folder").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }
    }

    private fun openImagePicker() {
        startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQUEST_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
            binding.imagePreview.setImageBitmap(selectedImageBitmap)
        }
    }

    private fun saveIcon(icon: Bitmap, size: String) {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ImageToIconTool")
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, "Icon_${System.currentTimeMillis()}_$size.png")
        FileOutputStream(file).use { it -> icon.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(Uri.fromFile(file)))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) openImagePicker()
        else Toast.makeText(this, "Izin diperlukan!", Toast.LENGTH_SHORT).show()
    }
}
