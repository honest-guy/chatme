package codinghacks.org.chatme

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Date
import java.util.Locale

class ChatListAdapter (
    private val onChatClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    private val chatItems = mutableListOf<ChatItem>()

    fun submitList(newList: List<ChatItem>) {
        chatItems.clear()
        chatItems.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatItems[position])
    }

    override fun getItemCount(): Int = chatItems.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val userName: TextView = itemView.findViewById(R.id.chatUserName)
        private val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private val timeText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(item: ChatItem) {
            userName.text = item.userName
            lastMessage.text = item.lastMessage // Set last message here
            timeText.text = formatTime(item.timestamp)


            // Load profile image (e.g., using Glide)
            Glide.with(itemView).load(item.profileImageUrl)
                .placeholder(R.drawable.baseline_person_outline_24)
                .into(profileImage)

            itemView.setOnClickListener { onChatClick(item) }
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

}