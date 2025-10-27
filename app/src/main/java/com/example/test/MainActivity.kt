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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // تم اصلی برنامه
            MaterialTheme {
                CounterApp()
            }
        }
    }
}

@Composable
fun CounterApp() {
    // متغیر state برای نگه داشتن عدد شمارنده
    var counter by remember { mutableStateOf(0) }

    // پس زمینه صفحه
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        // کارت اصلی که همه چیز داخلش هست
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
                // عنوان برنامه
                Text(
                    text = "شمارنده من 🎯",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // نمایش عدد شمارنده
                Text(
                    text = counter.toString(),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        counter > 0 -> Color(0xFF4CAF50) // سبز
                        counter < 0 -> Color(0xFFFF5252) // قرمز
                        else -> Color(0xFF666666) // خاکستری
                    }
                )

                Spacer(modifier = Modifier.height(50.dp))

                // دکمه‌های کنترل
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // دکمه کاهش (-)
                    FloatingActionButton(
                        onClick = { counter-- },
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

                    // دکمه ریست
                    FloatingActionButton(
                        onClick = { counter = 0 },
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

                    // دکمه افزایش (+)
                    FloatingActionButton(
                        onClick = { counter++ },
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
            }
        }
    }
}

