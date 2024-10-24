package com.midterm.plantsapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;

public class BackgroundService extends Service {

    private static final String CHANNEL_ID = "Humidity Alerts";
    private static final String SERVER_URL = "http://192.168.2.103:5000"; // Địa chỉ SSE của server
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification()); // Khởi động dịch vụ như một Foreground Service
        startReadingSoilMoistureData(); // Bắt đầu quá trình đọc dữ liệu độ ẩm
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Plant App")
                .setContentText("Đang theo dõi độ ẩm đất...")
                .setSmallIcon(R.drawable.circular_background)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void startReadingSoilMoistureData() {
        new Thread(() -> {
            try {
                socket = IO.socket(SERVER_URL);

                // Kết nối thành công
                socket.on(Socket.EVENT_CONNECT, args -> {
                    Log.d("SocketIO", "Kết nối thành công với server");
                });

                // Nhận dữ liệu độ ẩm từ server
                socket.on("soil_moisture_data", args -> {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        int moisture = data.getInt("moisture");
                        if (moisture < 40 || moisture > 70) {
                            sendNotification("Độ ẩm hiện tại: " + moisture + "%");
                        }
                    } catch (JSONException e) {
                        Log.e("Error", "Error parsing JSON data: " + e.getMessage());
                    }
                });

                // Kết nối socket
                socket.connect();
            } catch (Exception e) {
                Log.e("Error", "Error receiving soil moisture data: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void sendNotification(String message) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.circular_background) // Thêm icon của app vào sau
                .setContentTitle("Cảnh báo độ ẩm")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Humidity Alerts";
            String description = "Channel for plant alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
