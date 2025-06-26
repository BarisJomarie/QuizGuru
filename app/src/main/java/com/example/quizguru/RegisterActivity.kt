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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class RegisterActivity : AppCompatActivity(){


  private lateinit var fAuth: FirebaseAuth
  private lateinit var fStore: FirebaseFirestore


  private lateinit var username: EditText
  private lateinit var email: EditText
  private lateinit var password: EditText
  private lateinit var rePassword: EditText
  private lateinit var register: Button
  private lateinit var toMain: Button
  private lateinit var toLogin: TextView
  private lateinit var loadingOverlay: LinearLayout


  private var valid = true


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_register)


    //Initialize firebase instances
    fAuth = FirebaseAuth.getInstance()
    fStore = FirebaseFirestore.getInstance()


    //UI elements
    username = findViewById(R.id.etUsername)
    email = findViewById(R.id.etEmail)
    password = findViewById(R.id.etPassword)
    rePassword = findViewById(R.id.etRepeatPassword)
    register = findViewById(R.id.btnRegister)
    toMain = findViewById(R.id.btnToMain)
    toLogin = findViewById(R.id.tvToLogin)
    loadingOverlay = findViewById(R.id.loadingOverlay)


    register.setOnClickListener {
      register()
    }


    toMain.setOnClickListener {
      val intent = Intent(this, MainActivity::class.java)
      startActivity(intent)
      finish()
    }


    toLogin.setOnClickListener {
      val intent = Intent(this, LoginActivity::class.java)
      startActivity(intent)
      finish()
    }
  }


  private fun checkField(textField: EditText){
    if(textField.text.toString().trim().isEmpty()){
      textField.error = "This field is required"
      valid = false
    }
  }


  private fun resetUI() {
    register.isEnabled = true
    loadingOverlay.visibility = View.GONE
  }


  private fun register() {
    valid = true
    checkField(username)
    checkField(email)
    checkField(password)
    checkField(rePassword)


    if(valid) {
      if(password.text.toString().trim() != rePassword.getText().toString().trim()){
        rePassword.error = "The password is not match"
        return
      }


      register.isEnabled = false
      loadingOverlay.visibility = View.VISIBLE


      //start registration user process.
      fAuth.createUserWithEmailAndPassword(email.text.toString().trim(), password.text.toString().trim())
        .addOnCompleteListener { task ->
          if(task.isSuccessful){
            val userId = fAuth.currentUser?.uid ?: return@addOnCompleteListener
            val user = hashMapOf(
              "username" to username.text.toString(),
              "email" to email.text.toString(),
              "role" to "user",
              "createdAt" to FieldValue.serverTimestamp()
            )


            fStore.collection("users").document(userId).set(user)
              .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
              }
              .addOnFailureListener {
                Toast.makeText(this, "Failed to register user.", Toast.LENGTH_SHORT).show()
                resetUI()
              }
          } else {
            Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            resetUI()
          }
        }
        .addOnFailureListener {
          Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
          resetUI()
        }
    }
  }
}

