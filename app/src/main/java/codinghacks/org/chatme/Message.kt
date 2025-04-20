package codinghacks.org.chatme

data class Message(
    val text: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L
)
