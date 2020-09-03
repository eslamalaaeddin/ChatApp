package models

class ContactsModel(var name:String, var image:String, var status:String,var uid : String ,var phoneNumber:String){
    constructor() : this ("","","","","")


    class ContactsIdModel (var uid:String ) {
        constructor() :this ("")
    }
}