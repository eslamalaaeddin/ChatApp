package notifications

data class PushNotification(
    val data: NotificationData,
    val to: String
)