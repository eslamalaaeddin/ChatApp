package com.example.whatsapp.notifications


import com.example.whatsapp.helpers.Utils.CONTENT_TYPE
import com.example.whatsapp.helpers.Utils.SERVER_KEY
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationApi {
    @Headers("Authorization: key=$SERVER_KEY", "Content-Type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postNotification(@Body notification: PushNotification): Response<ResponseBody>

    @Headers("Authorization: key=$SERVER_KEY", "Content-Type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postVideoNotification(@Body notification: PushVideoChatNotification): Response<ResponseBody>


    @Headers("Authorization: key=$SERVER_KEY", "Content-Type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postMediaNotification(@Body notification: PushMediaNotification): Response<ResponseBody>
}