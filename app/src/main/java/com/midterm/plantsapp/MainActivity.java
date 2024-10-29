package com.midterm.plantsapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.midterm.plantsapp.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "Humidity Alert";
    private static final String SERVER_URL = "http://192.168.2.103:5000"; // Thay đổi IP nếu cần
    private ActivityMainBinding binding;
    private Socket socket;
    private static final String[] PERMISSIONS = {
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

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

        checkNetworkStatus();

        createNotificationChannel();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            requestNotificationPermission();
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestLocationPermissions();
//        }
        requestPermissions();
        binding.waveView.setPercentage(70);
        binding.btnPlantsDisease.setOnClickListener(view1 -> {
            Intent intent = new Intent(MainActivity.this, PlantsDiseases.class);
            startActivity(intent);
        });


        // Kết nối đến Socket.IO server
        try {
            connectSocketIO();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void checkNetworkStatus() {
        if (isNetworkAvailable(this)) {
            Log.d("Network Status", "Kết nối mạng có sẵn.");
        } else {
            Log.e("Network Status", "Không có kết nối mạng.");
        }
    }

    private void requestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Kiểm tra quyền thông báo (chỉ yêu cầu nếu API >= 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Kiểm tra quyền vị trí chính xác và vị trí chung (chỉ yêu cầu nếu API >= 23)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            try {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 1);
            } catch (Exception e) {
                Log.e("Permission Request", "Error requesting permissions: " + e.getMessage());
            }
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void connectSocketIO() throws URISyntaxException {
        socket = IO.socket(SERVER_URL);

        // Kết nối thành công
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d("SocketIO", "Kết nối thành công với server");
            runOnUiThread(() -> binding.connectionStatus.setText("Status: Connected"));
        });

        // Nhận dữ liệu độ ẩm từ server
        socket.on("soil_moisture_data", args -> {
            JSONObject data = (JSONObject) args[0];
            try {
                int moisture = data.getInt("moisture");
                if (moisture < 40 || moisture > 70) {
                    sendNotification("Độ ẩm hiện tại: " + moisture + "%");
                }
                runOnUiThread(() -> updateMoisture(moisture));
            } catch (JSONException e) {
                Log.e("Error", "Error parsing JSON data: " + e.getMessage());
            }
        });

        // Kết nối socket
        socket.connect();
    }

    private void updateMoisture(int moisture) {
        binding.moisturePercentage.setText(moisture + "%");
        binding.waveView.setPercentage(moisture);
    }

//    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
//    private void requestNotificationPermission() {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
//        }
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    private void requestLocationPermissions() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Quyền chưa được cấp, yêu cầu quyền
//            ActivityCompat.requestPermissions(this, new String[]{
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//            }, 1);
//        }
//    }

    private void sendNotification(String message) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.circular_background)
                .setContentTitle("Cảnh báo độ ẩm đất")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Humidity Alerts";
            String description = "Channel for humidity alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
        }
    }
}