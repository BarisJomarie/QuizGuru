package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

  private lateinit var email: EditText
  private lateinit var password: EditText
  private lateinit var login: Button
  private lateinit var toMain: Button
  private lateinit var toRegister: TextView
  private lateinit var loadingOverlay: LinearLayout

  private lateinit var fAuth: FirebaseAuth
  private lateinit var fStore: FirebaseFirestore

  private var valid = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)

    email = findViewById(R.id.etEmail)
    password = findViewById(R.id.etPassword)
    login = findViewById(R.id.btnLogin)
    toMain = findViewById(R.id.btnToMain)
    toRegister = findViewById(R.id.tvToRegister)
    loadingOverlay = findViewById(R.id.loadingOverlay)

    //Initialize firebase instances
    fAuth = FirebaseAuth.getInstance()
    fStore = FirebaseFirestore.getInstance()

    login.setOnClickListener {
      login()
    }

    toMain.setOnClickListener {
      val intent = Intent(this, MainActivity::class.java)
      startActivity(intent)
      finish()
    }

    toRegister.setOnClickListener {
      val intent = Intent(this, RegisterActivity::class.java)
      startActivity(intent)
      finish()
    }
  }

  private fun login() {
    valid = true
    checkField(email)
    checkField(password)

    if(valid) {
      login.isEnabled = false
      loadingOverlay.visibility = View.VISIBLE

      fAuth.signInWithEmailAndPassword(
        email.text.toString().trim(),
        password.text.toString().trim()
      ).addOnSuccessListener {
        val userId = fAuth.currentUser?.uid
        Toast.makeText(this, "Logged in Successfully!", Toast.LENGTH_SHORT).show()
        //check if admin or regular user
        if(userId != null) {
          checkUserAccess(userId)
        }
      }
        .addOnFailureListener {
          Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
          resetUI()
        }
    } else {
      return
    }
  }

  private fun checkField(textField: EditText) {
    if(textField.text.toString().trim().isEmpty()){
      textField.error = "This field is required"
      valid = false
    }
  }

  private fun resetUI(){
    login.isEnabled = true
    loadingOverlay.visibility = View.GONE
  }

  private fun checkUserAccess(userId: String) {
    fStore.collection("users").document(userId).get()
      .addOnSuccessListener { document ->
        if(document.exists()) {
          val role = document.getString("role")
          when (role) {
            "admin" -> {
              val intent = Intent(this, AdminHomepageActivity::class.java)
              startActivity(intent)
              finish()
            }
            "user" -> {
              val intent = Intent(this, UserHomepageActivity::class.java)
              startActivity(intent)
              finish()
            }
            else -> {
              Toast.makeText(this, "User role is not defined: $role", Toast.LENGTH_SHORT).show()
              resetUI()
            }
          }
        } else {
          Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show()
          resetUI()
        }
      }
      .addOnFailureListener { exception ->
        Toast.makeText(this, "Failed to get user info: ${exception.message}", Toast.LENGTH_SHORT).show()
        resetUI()
      }
  }
}