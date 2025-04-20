package codinghacks.org.chatme

data class ChatItem(
    val chatId: String = "",
    val userId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L
)
