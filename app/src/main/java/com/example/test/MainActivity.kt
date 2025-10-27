package com.example.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics  // ← ktx
import com.google.firebase.analytics.ktx.logEvent   // ← ktx
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    // Firebase Analytics instance
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // راه‌اندازی Firebase Analytics
        analytics = Firebase.analytics

        // لاگ باز شدن اپلیکیشن
        analytics.logEvent("app_opened") {
            param("screen_name", "counter_screen")
        }

        setContent {
            MaterialTheme {
                CounterApp(analytics)
            }
        }
    }
}

@Composable
fun CounterApp(analytics: FirebaseAnalytics) {
    var counter by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "شمارنده من 🎯",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = counter.toString(),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        counter > 0 -> Color(0xFF4CAF50)
                        counter < 0 -> Color(0xFFFF5252)
                        else -> Color(0xFF666666)
                    }
                )

                Spacer(modifier = Modifier.height(50.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // دکمه کاهش با Analytics
                    FloatingActionButton(
                        onClick = {
                            counter--
                            // لاگ کلیک روی دکمه منفی
                            analytics.logEvent("button_clicked") {
                                param("button_type", "minus")
                                param("counter_value", counter.toLong())
                            }
                        },
                        containerColor = Color(0xFFFF5252),
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "-",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // دکمه ریست با Analytics
                    FloatingActionButton(
                        onClick = {
                            val oldValue = counter
                            counter = 0
                            // لاگ ریست
                            analytics.logEvent("counter_reset") {
                                param("previous_value", oldValue.toLong())
                            }
                        },
                        containerColor = Color(0xFF9E9E9E),
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "↺",
                            fontSize = 28.sp,
                            color = Color.White
                        )
                    }

                    // دکمه افزایش با Analytics
                    FloatingActionButton(
                        onClick = {
                            counter++
                            // لاگ کلیک روی دکمه مثبت
                            analytics.logEvent("button_clicked") {
                                param("button_type", "plus")
                                param("counter_value", counter.toLong())
                            }
                        },
                        containerColor = Color(0xFF4CAF50),
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "+",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // نمایش اطلاعات
                Text(
                    text = "🔥 Firebase Analytics فعال است",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}