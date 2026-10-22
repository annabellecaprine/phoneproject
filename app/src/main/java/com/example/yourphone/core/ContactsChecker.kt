package com.example.yourphone.core

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

object ContactsChecker {
    private const val TAG = "ContactsChecker"

    fun isNumberInContacts(context: Context, number: String?): Boolean {
        if (number.isNullOrEmpty()) return false
        
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "READ_CONTACTS permission not granted. Treating as not in contacts.")
            return false
        }

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts provider", e)
        }
        return false
    }
}
