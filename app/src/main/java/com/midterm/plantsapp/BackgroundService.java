package com.midterm.plantsapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class BackgroundService extends Service {

    private static final String CHANNEL_ID = "PlantAlerts";
    private static final String SERVER_URL = "http://192.168.137.205:5000"; // Raspberry Pi IP
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        try {
            socket = IO.socket(SERVER_URL);
            socket.connect();

            //Receive Humidity data from Raspberry
            socket.on("soil_moisture_data", args -> {
                JSONObject data = (JSONObject) args[0];
                int moisture = 0;
                try {
                    moisture = data.getInt("moisture");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                if (moisture < 40) {
                    sendNotification("Cảnh báo độ ẩm đất", "Độ ẩm hiện tại: " + moisture + "%");
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    private void sendNotification(String title, String message) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.circular_background) //thêm icon của app vào sau
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "PlantAlerts";
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
