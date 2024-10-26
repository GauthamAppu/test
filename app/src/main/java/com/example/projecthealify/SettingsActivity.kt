package com.example.projecthealify

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var darkModeSwitch: Switch
    private lateinit var deleteAccountButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences to store dark mode state
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        // Initialize views
        darkModeSwitch = findViewById(R.id.dark_mode_switch)
        deleteAccountButton = findViewById(R.id.delete_account_button)

        // Set dark mode state based on saved preference
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Handle Dark Mode toggle
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("dark_mode", isChecked)
            editor.apply()

            // Toggle dark mode
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Handle Delete Account button
        deleteAccountButton.setOnClickListener {
            firebaseAuth.currentUser?.let { user ->
                user.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Redirect to sign-in screen after account deletion
                        startActivity(Intent(this, SignInActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    } else {

                    }
                }
            }
        }
    }
}
