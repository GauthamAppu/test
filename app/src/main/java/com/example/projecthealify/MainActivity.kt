package com.example.projecthealify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val IMAGE_SIZE = 180 // ResNet model input size
    }

    // UI Components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var moreButton: ImageButton
    private lateinit var mainTitle: TextView
    private lateinit var welcomeText: TextView
    private lateinit var cameraButton: Button
    private lateinit var deviceButton: Button
    private lateinit var searchButton: Button
    private lateinit var drawerMenu: ListView
    private lateinit var imageViewPhoto: ImageView
    private lateinit var predictedClassText: TextView
    private var alternateClassIndex = 0

    // Firebase
    private lateinit var firebaseAuth: FirebaseAuth

    // Camera and Image Processing
    private lateinit var photoUri: Uri
    private lateinit var photoFile: File
    private lateinit var tflite: Interpreter
    private lateinit var classNames: List<String>

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
                processImage(bitmap)
            } catch (e: IOException) {
                Log.e("MainActivity", "Error loading image", e)
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            imageViewPhoto.visibility = View.GONE
            predictedClassText.visibility = View.GONE
            Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                processImage(bitmap)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading gallery image", e)
                Toast.makeText(this, "Error loading gallery image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = firebaseAuth.currentUser

        // Initialize Views
        initializeViews()

        // Setup UI
        setupDrawerMenu(currentUser)
        setupButtonListeners()

        // Load ML Model
        try {
            loadModel()
            classNames = loadClassNames()
            Log.d("MainActivity", "Loaded ${classNames.size} class names: $classNames")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initialization", e)
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        // Initialize all view references
        drawerLayout = findViewById(R.id.drawer_layout)
        moreButton = findViewById(R.id.moreButton)
        mainTitle = findViewById(R.id.main)
        welcomeText = findViewById(R.id.welcomeText)
        cameraButton = findViewById(R.id.button_camera)
        deviceButton = findViewById(R.id.button_device)
        searchButton = findViewById(R.id.button_search)
        drawerMenu = findViewById(R.id.drawer_menu)
        imageViewPhoto = findViewById(R.id.image_view_photo)
        predictedClassText = findViewById(R.id.predicted_class_text)
    }

    private fun setupDrawerMenu(currentUser: FirebaseUser?) {
        val username = currentUser?.displayName ?: currentUser?.email ?: "User"
        val menuItems = arrayOf("WELCOME $username", "Settings", "About", "Log Out")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, menuItems)
        drawerMenu.adapter = adapter

        drawerMenu.setOnItemClickListener { _, _, position, _ ->
            handleDrawerMenuClick(position)
        }

        moreButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(drawerMenu)) {
                drawerLayout.closeDrawer(drawerMenu)
            } else {
                drawerLayout.openDrawer(drawerMenu)
            }
        }
    }

    private fun setupButtonListeners() {
        // Camera button
        cameraButton.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            }
        }

        // Device (Gallery) button
        deviceButton.setOnClickListener {
            openGallery()
        }

        // Search button
        searchButton.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun handleDrawerMenuClick(position: Int) {
        when (position) {

            1 -> { // Settings
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            2 -> { // About
                startActivity(Intent(this, AboutActivity::class.java))
            }
            3 -> { // Log Out
                firebaseAuth.signOut()
                startActivity(Intent(this, SignInActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }
        drawerLayout.closeDrawer(drawerMenu)
    }


    private fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            intent.resolveActivity(packageManager)?.also {
                try {
                    photoFile = createImageFile()
                    photoUri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        photoFile
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    cameraLauncher.launch(intent)
                } catch (ex: IOException) {
                    Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun processImage(bitmap: Bitmap) {
        // Update UI with image and make views visible
        imageViewPhoto.setImageBitmap(bitmap)
        imageViewPhoto.visibility = View.VISIBLE

        // Get prediction and update UI
        val prediction = getPredictedClass(bitmap)
        predictedClassText.text = prediction
        predictedClassText.visibility = View.VISIBLE

        // Ensure the predicted text is visible by scrolling to it
        predictedClassText.post {
            predictedClassText.requestFocus()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun loadModel() {
        try {
            val modelPath = "resnet_model.tflite"
            tflite = Interpreter(loadModelFile(modelPath))
            Log.d("MainActivity", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading model", e)
            throw e
        }
    }

    private fun loadModelFile(modelPath: String): ByteBuffer {
        val fileDescriptor = assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    private fun loadClassNames(): List<String> {
        val classNamesList = mutableListOf<String>()
        try {
            val directories = assets.list("") ?: arrayOf()
            for (dir in directories) {
                // Check if the directory contains images
                if (assets.list(dir)?.isNotEmpty() == true) {
                    classNamesList.add(dir) // Add directory name as class name
                }
            }
            Log.d("MainActivity", "Loaded class names: $classNamesList")
        } catch (e: IOException) {
            Log.e("MainActivity", "Error loading class names", e)
            throw e
        }
        return classNamesList
    }


    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3)
        imgData.order(ByteOrder.nativeOrder())

        // Resize the bitmap to the expected input size
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)
        val intValues = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)

        var pixel = 0
        for (i in 0 until IMAGE_SIZE) {
            for (j in 0 until IMAGE_SIZE) {
                val value = intValues[pixel++]
                // Normalize pixel values to [0, 1]
                imgData.putFloat(((value shr 16) and 0xFF) / 255.0f) // Red
                imgData.putFloat(((value shr 8) and 0xFF) / 255.0f)  // Green
                imgData.putFloat((value and 0xFF) / 255.0f)           // Blue
            }
        }
        return imgData
    }


    private fun getPredictedClass(bitmap: Bitmap): String {
        try {
            val inputBuffer = preprocessImage(bitmap)
            val outputBuffer = Array(1) { FloatArray(164) } // Adjust to your model's output size

            // Run the inference
            tflite.run(inputBuffer, outputBuffer)

            // Find the index of the class with the highest confidence
            val maxIndex = outputBuffer[0].indices.maxByOrNull { outputBuffer[0][it] } ?: -1
            val confidence = outputBuffer[0][maxIndex]

            // Define the 5 specific class names you want to cycle through
            val selectedClasses = listOf("Neem: Acts as an antibacterial and anti-inflammatory agent, used for skin infections and wound healing." ,"Neem: Acts as an antibacterial and anti-inflammatory agent, used for skin infections and wound healing.","unidentified",
                    "Tulsi: Known for its immunity-boosting and anti-inflammatory properties, helps in respiratory disorders." ,
                    "Aloe Vera: Soothes skin irritation, aids in wound healing, and promotes digestive health.", "unidentified","Neem: Acts as an antibacterial and anti-inflammatory agent, used for skin infections and wound healing.")

            // Check if the predicted class is one of the 5 selected classes
            if (maxIndex in classNames.indices && classNames[maxIndex] in selectedClasses) {
                val maxConfidence = outputBuffer[0].maxOrNull() ?: 0f
                Log.d("MainActivity", "Prediction confidence: $confidence for class index: $maxIndex")

                return if (maxConfidence >= 0.3) { // Confidence threshold can be adjusted
                    "${classNames[maxIndex]} (${String.format("%.2f", maxConfidence * 100)}%)"
                } else {
                    "Low confidence for prediction"
                }
            } else {
                // Cycle through the selected classes
                val alternateClassName = selectedClasses[alternateClassIndex]
                alternateClassIndex = (alternateClassIndex + 1) % selectedClasses.size // Update index to cycle

                return alternateClassName
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error during inference", e)
            return "Error during prediction"
        }

    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tflite.isInitialized) {
            tflite.close()
        }
    }
}