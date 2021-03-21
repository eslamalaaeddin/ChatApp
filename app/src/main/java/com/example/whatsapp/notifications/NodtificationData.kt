package com.example.whatsapp.notifications

data class NotificationData (
    val title:String,
    val message:String,
    val uid:String
)

data class VideoChatNotificationData (
    val title:String,
    val message:String,
    val uid:String
)

data class MediaNotificationData (
    val title:String,
    val messageType:String,
    val messageKey:String,
    val uid:String
)
