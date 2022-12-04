package com.simplemobiletools.calendar.pro.firebase

import android.net.Uri
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

object UploadSave  {
    val storage = Firebase.storage
    fun uploadFile(uid : String, uri: Uri,onFailure:(String)->Unit,onSuccessListener:()->Unit) {
        val filePath = storage.reference.child(uid)
        try {
            filePath.putFile(uri).addOnFailureListener { p0 -> onFailure.invoke(p0.message ?: "Unknown error") }.addOnSuccessListener {
                    task->onSuccessListener.invoke()
            }
        }catch (e:Exception){
            onFailure.invoke(e.message?:"Unknown error")
        }
    }

    fun downloadFile(uid : String, uri: Uri,onFailure:(String)->Unit,onSuccessListener:()->Unit) {
        val filePath = storage.reference.child(uid)
        try {
            filePath.getFile(uri).addOnFailureListener { p0 -> onFailure.invoke(p0.message ?: "Unknown error") }.addOnSuccessListener {
                    task->onSuccessListener.invoke()
            }
        }catch (e:Exception){
            onFailure.invoke(e.message?:"Unknown error")
        }
    }
}
