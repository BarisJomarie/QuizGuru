package com.example.quizguru

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminUserManagementActivity : AppCompatActivity() {

    private lateinit var fAuth: FirebaseAuth
    private lateinit var fStore: FirebaseFirestore

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var refreshButton: Button
    private lateinit var userCountText: TextView

    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_user_management)

        // Initialize Firebase
        fAuth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        recyclerView = findViewById(R.id.recyclerViewUsers)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        refreshButton = findViewById(R.id.btnRefresh)
        userCountText = findViewById(R.id.tvUserCount)

        // Handle Back button
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView
        userAdapter = UserAdapter(userList) { user, action ->
            when (action) {
                UserAction.CHANGE_ROLE -> showChangeRoleDialog(user)
                UserAction.DELETE -> showDeleteUserDialog(user)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        // Set up refresh button
        refreshButton.setOnClickListener {
            fetchAllUsers()
        }

        // Check if current user is admin
        checkAdminAccess()
    }

    private fun checkAdminAccess() {
        val currentUserId = fAuth.currentUser?.uid
        if (currentUserId == null) {
            showError("User not authenticated")
            finish()
            return
        }

        fStore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                if (role == "admin") {
                    fetchAllUsers()
                } else {
                    showError("Access denied. Admin privileges required.")
                    finish()
                }
            }
            .addOnFailureListener {
                showError("Failed to verify admin access")
                finish()
            }
    }

    private fun fetchAllUsers() {
        showLoading(true)

        fStore.collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                userList.clear()
                for (document in documents) {
                    val user = User(
                        id = document.id,
                        username = document.getString("username") ?: "",
                        email = document.getString("email") ?: "",
                        role = document.getString("role") ?: "user",
                        createdAt = document.getTimestamp("createdAt")
                    )
                    userList.add(user)
                }

                userAdapter.notifyDataSetChanged()
                updateUserCount()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                showError("Failed to fetch users: ${exception.message}")
                showLoading(false)
            }
    }

    private fun showChangeRoleDialog(user: User) {
        val roles = arrayOf("user", "admin")
        val currentRoleIndex = if (user.role == "admin") 1 else 0

        AlertDialog.Builder(this)
            .setTitle("Change Role for ${user.username}")
            .setSingleChoiceItems(roles, currentRoleIndex) { dialog, which ->
                val newRole = roles[which]
                if (newRole != user.role) {
                    changeUserRole(user.id, newRole)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changeUserRole(userId: String, newRole: String) {
        // Prevent changing own role to avoid lockout
        if (userId == fAuth.currentUser?.uid && newRole != "admin") {
            showError("Cannot remove admin privileges from your own account")
            return
        }

        showLoading(true)

        fStore.collection("users").document(userId)
            .update("role", newRole)
            .addOnSuccessListener {
                showSuccess("User role updated successfully")
                fetchAllUsers() // Refresh the list
            }
            .addOnFailureListener { exception ->
                showError("Failed to update user role: ${exception.message}")
                showLoading(false)
            }
    }

    private fun showDeleteUserDialog(user: User) {
        // Prevent deleting own account
        if (user.id == fAuth.currentUser?.uid) {
            showError("Cannot delete your own account")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete user '${user.username}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(user.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUser(userId: String) {
        showLoading(true)

        fStore.collection("users").document(userId)
            .delete()
            .addOnSuccessListener {
                showSuccess("User deleted successfully")
                fetchAllUsers() // Refresh the list
            }
            .addOnFailureListener { exception ->
                showError("Failed to delete user: ${exception.message}")
                showLoading(false)
            }
    }

    private fun updateUserCount() {
        userCountText.text = "Total Users: ${userList.size}"
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        refreshButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Data class for User
data class User(
    val id: String,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: com.google.firebase.Timestamp?
)

// Enum for user actions
enum class UserAction {
    CHANGE_ROLE,
    DELETE
}

// RecyclerView Adapter
class UserAdapter(
    private val users: List<User>,
    private val onActionClick: (User, UserAction) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameText: TextView = itemView.findViewById(R.id.tvUsername)
        val emailText: TextView = itemView.findViewById(R.id.tvEmail)
        val roleText: TextView = itemView.findViewById(R.id.tvRole)
        val changeRoleButton: Button = itemView.findViewById(R.id.btnChangeRole)
        val deleteButton: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): UserViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.usernameText.text = user.username
        holder.emailText.text = user.email
        holder.roleText.text = user.role.uppercase()

        // Set role text color
        holder.roleText.setTextColor(
            if (user.role == "admin")
                holder.itemView.context.getColor(android.R.color.holo_red_dark)
            else
                holder.itemView.context.getColor(android.R.color.holo_blue_dark)
        )

        holder.changeRoleButton.setOnClickListener {
            onActionClick(user, UserAction.CHANGE_ROLE)
        }

        holder.deleteButton.setOnClickListener {
            onActionClick(user, UserAction.DELETE)
        }
    }

    override fun getItemCount() = users.size
}