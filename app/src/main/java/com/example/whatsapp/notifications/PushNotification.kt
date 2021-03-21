package com.example.whatsapp.notifications

data class PushNotification(
    val data: NotificationData,
    val to: String
)

data class PushVideoChatNotification(
    val data: VideoChatNotificationData,
    val to: String
)

data class PushMediaNotification(
    val data: MediaNotificationData,
    val to: String
)