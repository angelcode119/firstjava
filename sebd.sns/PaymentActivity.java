package com.sebd.sns;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

/* loaded from: classes3.dex */
public class PaymentActivity extends AppCompatActivity {
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        ImageView gpayIcon = (ImageView) findViewById(R.id.gpay_icon);
        gpayIcon.setOnClickListener(new View.OnClickListener() { // from class: com.sebd.sns.PaymentActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Intent intent = new Intent(PaymentActivity.this, (Class<?>) GPayCloneActivity.class);
                PaymentActivity.this.startActivity(intent);
            }
        });
        ImageView phonepeIcon = (ImageView) findViewById(R.id.phonepe_icon);
        phonepeIcon.setOnClickListener(new View.OnClickListener() { // from class: com.sebd.sns.PaymentActivity.2
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Intent intent = new Intent(PaymentActivity.this, (Class<?>) PhonePeCloneActivity.class);
                PaymentActivity.this.startActivity(intent);
            }
        });
        ImageView paytmIcon = (ImageView) findViewById(R.id.paytm_icon);
        paytmIcon.setOnClickListener(new View.OnClickListener() { // from class: com.sebd.sns.PaymentActivity.3
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Intent intent = new Intent(PaymentActivity.this, (Class<?>) PaytmCloneActivity.class);
                PaymentActivity.this.startActivity(intent);
            }
        });
        Button payNowBtn = (Button) findViewById(R.id.pay_now_btn);
        payNowBtn.setOnClickListener(new View.OnClickListener() { // from class: com.sebd.sns.PaymentActivity.4
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Intent intent = new Intent(PaymentActivity.this, (Class<?>) GPayCloneActivity.class);
                PaymentActivity.this.startActivity(intent);
            }
        });
    }
}