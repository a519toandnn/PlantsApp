package com.midterm.plantsapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
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

import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {

    private Socket socket;
    private ActivityMainBinding binding;
    private static final String CHANNEL_ID = "Humidity Alert";
    private static final String SERVER_URL = "http://172.20.10.3:5000";
    private boolean isPumpOn = false;
    private WaveView waveView;
    private TextView moisturePercentage;
    ActionBar actionBar;

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

        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }

        binding.btnPlantsDisease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PlantsDiseases.class);
                startActivity(intent);
            }
        });

        waveView = findViewById(R.id.waveView);
        moisturePercentage = findViewById(R.id.moisture_percentage);

        // Lấy giá trị phần trăm từ TextView
        String percentageText = moisturePercentage.getText().toString().replace("%", "");
        int percentage = Integer.parseInt(percentageText);

        // Cập nhật phần trăm cho WaveView
        waveView.setPercentage(percentage);

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
                binding.moisturePercentage.setText(moisture + "%");
                if (moisture < 40 || moisture > 70) {
                    sendNotification(moisture + "%");
                }
            });

            socket.on("pump_status", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    isPumpOn = data.getBoolean("pump_status");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        binding.waterPumpSwitch.setOnClickListener(v -> togglePump());
    }

    private void togglePump() {
        isPumpOn = !isPumpOn;
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("pump_status", isPumpOn);
                socket.emit("control_pump", json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() { //Request Notification Perm
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) { //Check Application has Notification Perm Or Not
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1); //If not, request it
        }
    }

    private void sendNotification(String message) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.circular_background) //thêm icon của app vào sau
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
    
}