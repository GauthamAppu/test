package com.example.projecthealify

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about) // Create this layout file

        // Set up your About information
        val aboutTextView = findViewById<TextView>(R.id.aboutTextView)
        aboutTextView.text = "About Us\n\n" +
                "Welcome to Healify!\n" +
                "\n" +
                "At Healify, we believe in the power of nature and the healing properties of medicinal plants. Our mission is to connect individuals with the rich heritage of natural remedies through cutting-edge technology and a user-friendly mobile application.\n" +
                "\n" +
                "Who We Are:\n" +
                "\n" +
                "Healify is a passionate team of botanists, tech enthusiasts, and wellness advocates committed to empowering users with knowledge about medicinal plants. We understand that in our fast-paced world, people often overlook the natural remedies that have been used for centuries. Our app aims to bridge that gap by providing instant access to information about various medicinal plants at your fingertips.\n" +
                "\n" +
                "What We Do:\n" +
                "\n" +
                "Our application harnesses advanced machine learning algorithms to accurately identify medicinal plants through your device’s camera. By simply capturing an image of a plant, users can receive instant insights into its uses, benefits, and potential precautions. Each plant entry is accompanied by rich descriptions, images, and additional resources to help users deepen their understanding of natural medicine.. \n\n" +
                "Version: 1.0 \n" +
                "Developed by: Gautham & Karthik"
    }
}
