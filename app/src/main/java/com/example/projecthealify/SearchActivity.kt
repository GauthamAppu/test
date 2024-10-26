package com.example.projecthealify

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var searchResults: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize views
        searchEditText = findViewById(R.id.edit)
        searchButton = findViewById(R.id.button_search)
        imageView1 = findViewById(R.id.image_view1)
        imageView2 = findViewById(R.id.image_view2)
        searchResults = findViewById(R.id.search_results)

        // Clear search results initially
        searchResults.text = ""

        searchButton.setOnClickListener {
            val className = searchEditText.text.toString().trim()
            loadImages(className)
        }
    }

    private fun loadImages(className: String) {
        try {
            val assetManager = assets
            // List all images in the specified class folder
            val images = assetManager.list(className)

            if (images != null && images.isNotEmpty()) {
                // Update the search results TextView
                searchResults.text = "Search results for: $className"

                // Load the last 2 images
                val count = minOf(images.size, 2) // Get the number of images to load (1 or 2)
                for (i in 0 until count) {
                    val imageName = images[images.size - count + i] // Get the last images
                    val inputStream = assetManager.open("$className/$imageName") // Open the image file
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    // Logic to display the images
                    when (i) {
                        0 -> imageView1.setImageBitmap(bitmap) // Display the first image
                        1 -> imageView2.setImageBitmap(bitmap) // Display the second image
                    }
                }
            } else {
                // Update search results text if no images found
                searchResults.text = "No results found for: $className"
                Toast.makeText(this, "No images found in class: $className", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading images: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
