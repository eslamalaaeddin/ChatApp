package models

class PrivateMessageModel(var from:String,
                          var message:String,
                          var type:String,
                          var to:String,
                          var messageKey:String,
                          var date:String,
                          var time:String,
                          var fileName:String) {

    constructor() : this ("","","","","","","","")
}