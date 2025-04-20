package codinghacks.org.chatme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth


class ChatActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private val db = FirebaseFirestore.getInstance()
    private lateinit var chatId: String
    private lateinit var btnSend: Button
    private lateinit var etMessage: EditText
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 👇 Initialize your views
        recyclerView = findViewById(R.id.recyclerView)
        btnSend = findViewById(R.id.btnSend)
        etMessage = findViewById(R.id.etMessage)

        chatId = intent.getStringExtra("chatId")!!

        adapter = MessageAdapter(currentUserId = FirebaseAuth.getInstance().currentUser!!.uid)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java) }
                if (messages != null) {
                    adapter.submitList(messages)
                    recyclerView.scrollToPosition(messages.size - 1) // optional: auto-scroll
                }
            }

        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isEmpty()) return@setOnClickListener

            val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
            val message = Message(
                text = messageText,
                senderId = currentUserId,
                timestamp = System.currentTimeMillis()
            )

            val chatDocRef = db.collection("chats").document(chatId)
            val messagesRef = chatDocRef.collection("messages")

            messagesRef.add(message)
                .addOnSuccessListener {
                    // ✅ Update lastMessage and timestamp in parent chat doc
                    chatDocRef.update(
                        mapOf(
                            "lastMessage" to message.text,
                            "timestamp" to message.timestamp
                        )
                    )
                    etMessage.setText("")
                }
                .addOnFailureListener { e ->
                    Log.e("SendMessageError", "Failed to send message: ${e.message}")
                }
        }


    }
}