package com.example.whatsapp

import models.GroupModel

//to delegate the response to clicks to the hosting activity
interface Callback {
    //for group fragment clicks
    fun onGroupClicked(groupName:GroupModel)

    //for chat fragment clicks
    fun onUserChatClicked(userName:String, userId:String ,userImage:String)

    //for status  clicks
    fun onStatusClicked(id:String)

}