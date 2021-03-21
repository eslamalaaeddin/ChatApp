package com.example.whatsapp

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class BaseApplication : Application() {
    private lateinit var auth: FirebaseAuth
    private lateinit var rootReference:DatabaseReference

    override fun onCreate() {
        super.onCreate()
        initializeFirebaseDatabase()
        initializeAuth()
    }

    companion object {
        private var databaseInstance: DatabaseReference? = null
        private var authInstance : FirebaseAuth? = null

        private fun initializeFirebaseDatabase() {
            if (databaseInstance == null) {
                databaseInstance = FirebaseDatabase.getInstance().reference
            }
        }
        fun getRootReference(): DatabaseReference {
            return databaseInstance ?: throw IllegalStateException("Database reference must be initialized")
        }

        private fun initializeAuth() {
            if (authInstance == null) {
                authInstance = FirebaseAuth.getInstance()
            }
        }
        fun getAuth(): FirebaseAuth{
            return authInstance ?: throw IllegalStateException("Firebase auth must be initialized")
        }

    }



}