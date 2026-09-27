package com.editog.novaagent.automation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.editog.novaagent.data.model.ContactRowChoice

class ContactsHelper(private val context: Context) {

    fun searchContacts(queryName: String): List<ContactRowChoice> {
        val results = mutableListOf<ContactRowChoice>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("ContactsHelper", "READ_CONTACTS permission not granted")
            return results
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$queryName%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            var index = 1
            if (cursor != null && cursor.moveToFirst()) {
                val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

                do {
                    val name = cursor.getString(nameCol) ?: "Unknown"
                    val number = cursor.getString(numberCol) ?: ""
                    val typeInt = cursor.getInt(typeCol)
                    val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        typeInt,
                        "Mobile"
                    ).toString()

                    if (number.isNotBlank()) {
                        results.add(
                            ContactRowChoice(
                                rowNumber = index++,
                                name = name,
                                phoneNumber = number.trim(),
                                type = typeLabel
                            )
                        )
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("ContactsHelper", "Error querying contacts", e)
        } finally {
            try {
                cursor?.close()
            } catch (e: Throwable) {}
        }

        return results
    }
}
