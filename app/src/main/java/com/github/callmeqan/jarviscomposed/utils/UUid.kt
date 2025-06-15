package com.github.callmeqan.jarviscomposed.utils
import android.content.Context
import java.util.*
import androidx.core.content.edit

class UUid(private val context: Context) {

    private val prefs = context.getSharedPreferences("uid_prefs", Context.MODE_PRIVATE)
    private var rawUid: String? = null

    fun hasUid(): Boolean = !rawUid.isNullOrBlank()

    fun getUid(): String? = rawUid

    fun initialize(): Boolean {
        rawUid = prefs.getString("device_uid", null)

        if (rawUid == null) {
            rawUid = UUID.randomUUID().toString()
            prefs.edit { putString("device_uid", rawUid) }

            // #TODO: Save rawUid to your backend or use it in your app logic
            println("Generated and saved new UID: $rawUid")
        } else {
            println("Loaded existing UID: $rawUid")
        }

        return hasUid()
    }
}
