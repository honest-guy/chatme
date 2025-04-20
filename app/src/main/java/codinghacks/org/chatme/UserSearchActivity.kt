package codinghacks.org.chatme

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class UserSearchFragment : Fragment() {

    private lateinit var btnSearch: Button
    private lateinit var etSearch: EditText
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_user_search, container, false)

        btnSearch = view.findViewById(R.id.btnSearch)
        etSearch = view.findViewById(R.id.etSearch)

        recyclerView = view.findViewById(R.id.recyclerView1)
        adapter = UserAdapter(userList) { user ->
            startChatWith(user)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        btnSearch.setOnClickListener {
            val queryText = etSearch.text.toString().trim()

            if (queryText.isEmpty()) {
                Toast.makeText(requireContext(), "Enter username or email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users")
                .whereIn("email", listOf(queryText))
                .get()
                .addOnSuccessListener { emailResults ->
                    db.collection("users")
                        .whereEqualTo("username", queryText)
                        .get()
                        .addOnSuccessListener { usernameResults ->
                            val users = (emailResults.documents + usernameResults.documents)
                                .distinctBy { it.id } // avoid duplicates
                                .mapNotNull { it.toObject(User::class.java) }

                            userList.clear()
                            userList.addAll(users)
                            adapter.notifyDataSetChanged()
                        }
                }
        }


        return view
    }

    private fun startChatWith(user: User) {
        val currentUid = FirebaseAuth.getInstance().currentUser!!.uid
        val otherUid = user.uid

        val chatQuery = db.collection("chats")
            .whereEqualTo("users", listOf(currentUid, otherUid))

        chatQuery.get().addOnSuccessListener {
            val chatId = if (it.isEmpty) {
                val newChat = hashMapOf(
                    "users" to listOf(currentUid, otherUid),
                    "lastMessage" to "",
                    "timestamp" to System.currentTimeMillis()
                )
                val doc = db.collection("chats").document()
                doc.set(newChat)
                doc.id
            } else {
                it.documents.first().id
            }

            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("chatId", chatId)
            startActivity(intent)
        }
    }
}
