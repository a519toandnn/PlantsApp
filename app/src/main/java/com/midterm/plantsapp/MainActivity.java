package com.midterm.plantsapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.midterm.plantsapp.databinding.ActivityMainBinding;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 1;
    private ActivityMainBinding binding;
    private DatabaseReference moistureRef;
    private DatabaseReference pumpStateRef;
    private DatabaseReference tokenRef;
    private boolean isPumpOn = false;
    private static final String TAG = "MainActivity";
    private String databaseURL = "https://plantsapp-58396-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission();
        }

        moistureRef = FirebaseDatabase.getInstance(databaseURL)
                                        .getReference("moisture");
        pumpStateRef = FirebaseDatabase.getInstance(databaseURL)
                                        .getReference("pump_state");
        tokenRef = FirebaseDatabase.getInstance(databaseURL).getReference("device_tokens");

        // Get token of Device
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get token of device
                    String device_token = task.getResult();

                    tokenRef.orderByValue().equalTo(device_token).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) { // device_token is not exist in Realtime Database

                                String tokenKey = UUID.randomUUID().toString();
                                tokenRef.child(tokenKey).setValue(device_token);
                                Log.d(TAG, "Token saved with key: " + tokenKey);

                            }

                            else {
                                Log.d(TAG, "Token already exists in the database.");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w(TAG, "Failed to check token existence.", error.toException());
                        }
                    });
                });


        // Get moisture from Realtime Database
        moistureRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer moisture = dataSnapshot.getValue(Integer.class);
                    if (moisture != null) {
                        binding.moisturePercentage.setText(moisture + "%");
                        binding.waveView.setPercentage(moisture);
                    } else {
                        Log.w("MainActivity", "Moisture value is null");
                    }
                } else {
                    Log.w("MainActivity", "No data available or 'moisture' field does not exist.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("MainActivity", "Failed to read value from database.", databaseError.toException());
            }
        });

        // Get pump status from Realtime Database
        pumpStateRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String state = dataSnapshot.getValue(String.class);
                    isPumpOn = "ON".equals(state);
                    updatePumpButtonText();
                } else {
                    Log.w("MainActivity", "No pump state data available.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("MainActivity", "Failed to read pump state.", databaseError.toException());
            }
        });

        // Turn on/off pump
        binding.waterPumpSwitch.setOnClickListener(v -> togglePumpState());

        // Change to Plants Diseases Screen
        binding.btnPlantsDisease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PlantsDiseases.class);
                startActivity(intent);
            }
        });
    }

    // Check Notification Permission
    private void checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Request Permission if it is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
        } else {
            Log.d(TAG, "Notification permission already granted.");
        }
    }

    // Get result of request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted.");
            } else {
                Log.d(TAG, "Notification permission denied.");
                Toast.makeText(this, "Bạn cần cấp quyền gửi thông báo để sử dụng tính năng cảnh báo!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Update pump status to Realtime Database
    private void togglePumpState() {
        String newState = isPumpOn ? "OFF" : "ON";
        pumpStateRef.setValue(newState).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String message = newState.equals("ON") ? "Máy bơm đã bật" : "Máy bơm đã tắt";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                updatePumpButtonText();
                isPumpOn = (newState.equals("ON") ? true : false);
            } else {
                Toast.makeText(MainActivity.this, "Lỗi khi cập nhật trạng thái máy bơm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Update waterPumpSwitch button's text
    private void updatePumpButtonText() {
        binding.waterPumpSwitch.setText(isPumpOn ? "Tắt máy bơm" : "Bật máy bơm");
    }
}
