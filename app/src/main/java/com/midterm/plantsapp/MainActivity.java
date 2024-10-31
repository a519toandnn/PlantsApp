package com.midterm.plantsapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseReference sensorDataRef;
    private DatabaseReference pumpStateRef;
    private boolean isPumpOn = false;
    private static final String TAG = "MainActivity";

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

        FirebaseMessaging.getInstance().subscribeToTopic("humidity_alerts")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Đăng ký topic thành công!");
                    } else {
                        Log.d("FCM", "Đăng ký topic thất bại!");
                    }
                });

        sensorDataRef = FirebaseDatabase.getInstance("https://plantsapp-58396-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                        .getReference("sensor_data");
        pumpStateRef = FirebaseDatabase.getInstance("https://plantsapp-58396-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                        .getReference("pump_state");

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Lấy token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    // Bạn có thể gửi token này đến server hoặc lưu vào SharedPreferences
                });

        // Lắng nghe thay đổi dữ liệu độ ẩm
        sensorDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("humidity")) {
                    Integer humidity = dataSnapshot.child("humidity").getValue(Integer.class);
                    if (humidity != null) { // Kiểm tra nếu độ ẩm không null
                        binding.moisturePercentage.setText(humidity + "%");
                        binding.waveView.setPercentage(humidity);
                    } else {
                        Log.w("MainActivity", "Humidity value is null");
                    }
                } else {
                    Log.w("MainActivity", "No data available or 'humidity' field does not exist.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("MainActivity", "Failed to read value.", databaseError.toException());
            }
        });

        // Lắng nghe trạng thái máy bơm từ Firebase để cập nhật nút bật/tắt
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

        // Chuyển đổi trạng thái máy bơm khi nhấn nút
        binding.waterPumpSwitch.setOnClickListener(v -> togglePumpState());
    }

    private void togglePumpState() {
        String newState = isPumpOn ? "OFF" : "ON";
        pumpStateRef.setValue(newState).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String message = newState.equals("ON") ? "Máy bơm đã bật" : "Máy bơm đã tắt";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                isPumpOn = !isPumpOn;
                updatePumpButtonText();
            } else {
                Toast.makeText(MainActivity.this, "Lỗi khi cập nhật trạng thái máy bơm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePumpButtonText() {
        binding.waterPumpSwitch.setText(isPumpOn ? "Tắt máy bơm" : "Bật máy bơm");
    }
}
