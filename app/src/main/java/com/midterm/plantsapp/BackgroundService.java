//package com.midterm.plantsapp;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.Service;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.IBinder;
//
//import androidx.annotation.Nullable;
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.net.URISyntaxException;
//
//import io.socket.client.IO;
//import io.socket.client.Socket;
//
//public class BackgroundService extends Service {
//
//    private static final String CHANNEL_ID = "PlantAlerts";
//    private static final String SERVER_URL = "http://192.168.137.205:5000"; // Raspberry Pi IP
//    private Socket socket;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        createNotificationChannel();
//
//        try {
//            socket = IO.socket(SERVER_URL);
//            socket.connect();
//
//            //Receive Humidity data from Raspberry
//            socket.on("soil_moisture_data", args -> {
//                JSONObject data = (JSONObject) args[0];
//                int moisture = 0;
//                try {
//                    moisture = data.getInt("moisture");
//                } catch (JSONException e) {
//                    throw new RuntimeException(e);
//                }
//                if (moisture < 40) {
//                    sendNotification("Cảnh báo độ ẩm đất", "Độ ẩm hiện tại: " + moisture + "%");
//                }
//            });
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private void sendNotification(String title, String message) {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.circular_background) //thêm icon của app vào sau
//                .setContentTitle(title)
//                .setContentText(message)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setDefaults(Notification.DEFAULT_ALL);
//
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        notificationManager.notify(1, builder.build());
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = "PlantAlerts";
//            String description = "Channel for plant alerts";
//            int importance = NotificationManager.IMPORTANCE_HIGH;
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
//            channel.setDescription(description);
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//        }
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//}
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackgroundService extends Service {

    private static final String CHANNEL_ID = "Humidity Alerts";
    private static final String SERVER_URL = "http://192.168.137.205:5000/soil_moisture_events"; // Địa chỉ SSE của server

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startReadingSoilMoistureData(); // Bắt đầu quá trình đọc dữ liệu độ ẩm
    }

    private void startReadingSoilMoistureData() {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "/soil_moisture_events");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("Connection Status", "Kết nối đến server thành công.");
                } else {
                    Log.e("Connection Status", "Kết nối đến server thất bại với mã: " + responseCode);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line, jsonData = "";
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        jsonData = line.substring(6);
                        Log.d("SSE Data", jsonData);
                    }
                    try {
                        // Phân tích dữ liệu JSON
                        JSONObject jsonObject = new JSONObject(jsonData);
                        int moisture = jsonObject.getInt("moisture");

                        // Gửi thông báo nếu độ ẩm nằm ngoài khoảng cho phép
                        if (moisture < 40 || moisture > 70) {
                            sendNotification("Độ ẩm hiện tại: " + moisture + "%");
                        }
                    } catch (JSONException e) {
                        Log.e("Error", "Error parsing JSON data: " + e.getMessage());
                    }
                }
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
