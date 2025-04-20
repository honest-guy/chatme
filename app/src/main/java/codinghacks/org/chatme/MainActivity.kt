package codinghacks.org.chatme

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import codinghacks.org.chatme.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load default fragment
        loadFragment(ChatListFragment())

        checkForExistingChats()

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_chats -> loadFragment(ChatListFragment())
                //R.id.nav_search -> loadFragment(UserSearchFragment())
                // Add more tabs if needed
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
      //  val searchView = searchItem.actionView as SearchView

        searchItem.setOnMenuItemClickListener {
            loadSearchFragment(UserSearchFragment())
            true
        }

        return true


    }


    private fun loadSearchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // Optional: adds to back stack
            .commit()


    }


    private fun searchUsers(query: String) {
        val db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("users")

        // First search by email
        usersRef.whereEqualTo("email", query)
            .get()
            .addOnSuccessListener { emailResults ->
                if (!emailResults.isEmpty) {
                    val user = emailResults.documents.first().toObject(User::class.java)
                    // Show in UI or start chat
                    user?.let { promptToChatWith(it) }
                } else {
                    // If not found by email, try username
                    usersRef.whereEqualTo("username", query)
                        .get()
                        .addOnSuccessListener { usernameResults ->
                            if (!usernameResults.isEmpty) {
                                val user =
                                    usernameResults.documents.first().toObject(User::class.java)
                                user?.let { promptToChatWith(it) }
                            } else {
                                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

    }


    private fun startChatWith(user: User) {
        val currentUid = FirebaseAuth.getInstance().currentUser!!.uid
        val otherUid = user.uid
        val db = FirebaseFirestore.getInstance()

        // Check if there's an existing chat between the two users
        db.collection("chats")
            .whereArrayContains("users", currentUid)
            .get()
            .addOnSuccessListener { result ->
                val existingChat = result.documents.firstOrNull { doc ->
                    val users = doc.get("users") as? List<*> ?: emptyList<Any>()
                    users.containsAll(listOf(currentUid, otherUid)) && users.size == 2
                }

                val chatId = if (existingChat != null) {
                    // If chat exists, return the existing chat ID
                    existingChat.id
                } else {
                    // If no chat exists, create a new chat
                    val newChat = hashMapOf(
                        "users" to listOf(currentUid, otherUid),
                        "lastMessage" to "",
                        "timestamp" to System.currentTimeMillis()
                    )
                    val doc = db.collection("chats").document()
                    doc.set(newChat)
                    doc.id
                }

                // Start the chat activity with the chat ID
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                startActivity(intent)
            }
            .addOnFailureListener {
                // Handle failure if any
                Toast.makeText(this, "Error checking chat: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }




    private fun promptToChatWith(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Start Chat?")
            .setMessage("Chat with ${user.username} (${user.email})?")
            .setPositiveButton("Yes") { _, _ -> startChatWith(user) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun checkForExistingChats() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("chats")
            .whereArrayContains("users", currentUid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // No chats yet, prompt to search users
                    AlertDialog.Builder(this)
                        .setTitle("No Chats Yet")
                        .setMessage("Would you like to start a new chat?")
                        .setPositiveButton("Find Users") { _, _ ->
                            loadFragment(UserSearchFragment()) // Or navigate tab directly

                        }
                        .setNegativeButton("Later", null)
                        .show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to check chats: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }



}