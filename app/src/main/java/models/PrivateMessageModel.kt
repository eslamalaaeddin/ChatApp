package models

class PrivateMessageModel(var from:String,
                          var message:String,
                          var type:String,
                          var to:String,
                          var seen:String,
                          var messageKey:String,
                          var date:String,
                          var time:String,
                          var messageTime:String,
                          var fileName:String) {

    constructor() : this ("","","","","","","","","","")
}