package com.example.callscribe.recording

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactLookupHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun lookupContactName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        return cursor.getString(columnIndex)
                    }
                }
            }
            null
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun isKnownContact(phoneNumber: String?): Boolean {
        return lookupContactName(phoneNumber) != null
    }
}
