package codinghacks.org.chatme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore



class ChatListFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ChatListAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for the fragment
        val view = inflater.inflate(R.layout.activity_chat_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)

        adapter = ChatListAdapter { chatItem ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("chatId", chatItem.chatId)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchChatList()

        return view
    }

    private fun fetchChatList() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("chats")
            .whereArrayContains("users", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || snapshot.isEmpty) {
                    adapter.submitList(emptyList())
                    return@addSnapshotListener
                }

                val chatItems = mutableListOf<ChatItem>()
                val chatDocs = snapshot.documents
                var fetchedCount = 0

                for (doc in chatDocs) {
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val users = doc.get("users") as? List<*>
                    val otherUserId = users?.firstOrNull { it != currentUserId } as? String ?: continue

                    Log.d("ChatListDebug", "Chat with $otherUserId, lastMessage: $lastMessage")


                    // Fetch user data from "users" collection
                    db.collection("users").document(otherUserId).get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: "Unknown"
                            val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""

                            chatItems.add(
                                ChatItem(
                                    chatId = doc.id,
                                    userId = otherUserId,
                                    userName = username,
                                    profileImageUrl = profileImageUrl,
                                    lastMessage = lastMessage,
                                    timestamp = timestamp
                                )
                            )
                            fetchedCount++

                            // Update list only when all items are fetched
                            if (fetchedCount == chatDocs.size) {
                                adapter.submitList(chatItems.sortedByDescending { it.timestamp })
                            }
                        }
                        .addOnFailureListener {
                            fetchedCount++
                            if (fetchedCount == chatDocs.size) {
                                adapter.submitList(chatItems.sortedByDescending { it.timestamp })
                            }
                        }
                }
            }
    }


}