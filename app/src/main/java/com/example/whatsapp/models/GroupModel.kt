package com.example.whatsapp.models

class GroupModel(var name:String, var image:String, var status:String,var gid : String ,var participants:String,var admin:String) {
    constructor() : this("", "", "", "","","")
}