package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserHomepageActivity : AppCompatActivity() {

  private lateinit var fAuth: FirebaseAuth
  private lateinit var fStore: FirebaseFirestore

  // theme selection
  private lateinit var cp: Button // Crops and Plants
  private lateinit var fv: Button // Fruits and Vegetables
  private lateinit var farmAnimals: Button // Farm Animals
  private lateinit var sf: Button // Soil and Fertilizers
  private lateinit var tm: Button // Tools and Machines
  private lateinit var ppd: Button // Pests and Plants Diseases
  private lateinit var logout: Button
  private lateinit var btnAdmin: Button // To admin(if user is admin)

  private var selectedTheme = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_user_home)

    //Initialzie Firebaqse instances
    fAuth = FirebaseAuth.getInstance()
    fStore = FirebaseFirestore.getInstance()

    cp = findViewById(R.id.btnCropAndPLants)
    fv = findViewById(R.id.btnFruitsAndVeg)
    farmAnimals = findViewById(R.id.btnFarmAnimals)
    sf = findViewById(R.id.btnSoilAndFert)
    tm = findViewById(R.id.btnToolsAndMachines)
    ppd = findViewById(R.id.btnPestsAndPlantDiseases)
    logout = findViewById(R.id.btnLogout)
    btnAdmin = findViewById(R.id.btnAdmin)

    fv.setOnClickListener { available("Fruits & Vegetables") }

    cp.setOnClickListener { available("Crops & Plants") }
    farmAnimals.setOnClickListener { notAvailable() }
    sf.setOnClickListener { notAvailable() }
    tm.setOnClickListener { notAvailable() }
    ppd.setOnClickListener { notAvailable() }

    btnAdmin.visibility = Button.GONE

    //check if admin
    val userId = fAuth.currentUser?.uid
    if (userId != null) {
      fStore.collection("users")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
          if (document != null && document.exists()) {
            val role = document.getString("role")
            if (role == "admin") {
              btnAdmin.visibility = Button.VISIBLE
            }
          }
        }
        .addOnFailureListener {
          Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show()
        }
    }

    logout.setOnClickListener {
      AlertDialog.Builder(this)
        .setTitle("Logout")
        .setMessage("Are you sure you want to logout?")
        .setPositiveButton("Yes") {_, _ ->
          logout()
        }
        .setNegativeButton("Cancel", null)
        .show()
    }

    btnAdmin.setOnClickListener {
      toAdmin()
    }
  }

  private fun notAvailable() {
    Toast.makeText(this, "Not available yet!", Toast.LENGTH_SHORT).show()
  }

  private fun available(theme: String) {
    val intent = Intent(this, SoloMainActivity::class.java)
    intent.putExtra("selectedTheme", theme)
    startActivity(intent)
  }

  private fun logout() {
    fAuth.signOut()
    val intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
    finish()
  }

  private fun toAdmin() {
    val intent = Intent(this, AdminHomepageActivity::class.java)
    startActivity(intent)
    finish()
  }
}