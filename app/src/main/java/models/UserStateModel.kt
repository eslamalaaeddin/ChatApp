package models

class UserStateModel(var date:String, var state:String, var time:String,var chatting:String,var typing:String) {
    constructor() : this ("","","","","")
}