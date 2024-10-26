package com.example.projecthealify

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    private lateinit var resultImageView: ImageView
    private lateinit var predictedClassTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        resultImageView = findViewById(R.id.result_image_view)
        predictedClassTextView = findViewById(R.id.predicted_class_text_view)

        // Get the bitmap and predicted class from the intent
        val bitmap = intent.getParcelableExtra<Bitmap>("imageBitmap")
        val predictedClass = intent.getStringExtra("predictedClass")

        // Set the bitmap and predicted class to the views
        resultImageView.setImageBitmap(bitmap)
        predictedClassTextView.text = predictedClass ?: "Unknown"
    }
}
