package com.example.whatsapp
//to delegate the response to clicks to the hosting activity
interface Callback {
    //for group fragment clicks
    fun onGroupClicked(groupName:String)

    //for chat fragment clicks
    fun onUserChatClicked(userName:String, userId:String )

}