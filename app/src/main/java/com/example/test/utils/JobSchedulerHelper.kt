package com.example.test.utils

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.test.HeartbeatJobService
import com.example.test.ServerConfig

/**
 * ⭐ Helper برای schedule کردن JobScheduler
 */
object JobSchedulerHelper {

    private const val TAG = "JobSchedulerHelper"

    /**
     * Schedule کردن Heartbeat Job
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun scheduleHeartbeatJob(context: Context) {
        try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            
            // اگه قبلاً schedule شده، لغوش کن
            jobScheduler.cancel(HeartbeatJobService.JOB_ID)
            
            // فاصله Heartbeat از Remote Config
            val intervalMs = ServerConfig.getHeartbeatInterval()
            val intervalMinutes = (intervalMs / 60000).toInt() // به دقیقه تبدیل کن
            
            // حداقل 15 دقیقه برای Android 7+
            val finalInterval = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                maxOf(intervalMinutes, 15)
            } else {
                intervalMinutes
            }
            
            Log.d(TAG, "📅 Scheduling heartbeat job every $finalInterval minutes")
            
            val componentName = ComponentName(context, HeartbeatJobService::class.java)
            
            val jobInfo = JobInfo.Builder(HeartbeatJobService.JOB_ID, componentName)
                .setPeriodic(finalInterval * 60 * 1000L)  // به میلی‌ثانیه
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)  // نیاز به اینترنت
                .setPersisted(true)  // ⭐ بعد از reboot هم بمونه
                .setRequiresCharging(false)  // حتی بدون شارژر
                .setRequiresDeviceIdle(false)  // حتی وقتی استفاده میشه
                .apply {
                    // Android 7+ - Backoff برای retry
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setBackoffCriteria(
                            30000,  // 30 ثانیه
                            JobInfo.BACKOFF_POLICY_EXPONENTIAL
                        )
                    }
                }
                .build()

            val result = jobScheduler.schedule(jobInfo)
            
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "✅ Heartbeat job scheduled successfully")
            } else {
                Log.e(TAG, "❌ Failed to schedule heartbeat job")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling job: ${e.message}", e)
        }
    }

    /**
     * لغو کردن همه Job‌ها
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun cancelAllJobs(context: Context) {
        try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancelAll()
            Log.d(TAG, "🗑️ All jobs cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling jobs: ${e.message}")
        }
    }

    /**
     * چک کردن وضعیت Job
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun isJobScheduled(context: Context): Boolean {
        return try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val pendingJobs = jobScheduler.allPendingJobs
            
            val isScheduled = pendingJobs.any { it.id == HeartbeatJobService.JOB_ID }
            
            Log.d(TAG, "📊 Job scheduled: $isScheduled")
            isScheduled
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking job status: ${e.message}")
            false
        }
    }
}
