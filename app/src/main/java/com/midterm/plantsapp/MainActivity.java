//package com.midterm.plantsapp;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.Build;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.TextView;
//
//import androidx.annotation.RequiresApi;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.midterm.plantsapp.databinding.ActivityMainBinding;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//
//public class MainActivity extends AppCompatActivity {
//
//    private static final String CHANNEL_ID = "Humidity Alert";
//    private static final String SERVER_URL = "http://192.168.2.103:5000";
//    private ActivityMainBinding binding;
//    private TextView moisturePercentage;
//    private WaveView waveView;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        View view = binding.getRoot();
//        setContentView(view);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        if (isNetworkAvailable(this)) {
//            Log.d("Network Status", "Kết nối mạng có sẵn.");
//            if (isInternetAvailable()) {
//                Log.d("Network Status", "Có thể kết nối đến Internet.");
//            } else {
//                Log.e("Network Status", "Kết nối mạng có sẵn nhưng không thể kết nối đến Internet.");
//            }
//        } else {
//            Log.e("Network Status", "Không có kết nối mạng.");
//        }
//
//        createNotificationChannel();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            requestNotificationPermission();
//        }
//
//        binding.btnPlantsDisease.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this, PlantsDiseases.class);
//                startActivity(intent);
//            }
//        });
//
//        moisturePercentage = findViewById(R.id.moisture_percentage);
//        waveView = findViewById(R.id.waveView);
//        moisturePercentage = findViewById(R.id.moisture_percentage);
//
//        // Lấy giá trị phần trăm từ TextView
//        String percentageText = moisturePercentage.getText().toString().replace("%", "");
//        int percentage = Integer.parseInt(percentageText);
//
//        // Cập nhật phần trăm cho WaveView
//        waveView.setPercentage(percentage);
//
//        // Bắt đầu nhận dữ liệu độ ẩm từ server
//        new Thread(this::receiveSoilMoistureData).start();
//    }
//
//    public boolean isInternetAvailable() {
//        try {
//            HttpURLConnection urlConnection = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
//            urlConnection.setRequestProperty("User-Agent", "test");
//            urlConnection.setRequestProperty("Connection", "close");
//            urlConnection.setConnectTimeout(3000); // 3 giây
//            urlConnection.connect();
//            return (urlConnection.getResponseCode() == 200);
//        } catch (Exception e) {
//            Log.e("Internet Check", "Error checking internet availability: " + e.getMessage());
//            return false;
//        }
//    }
//
//
//    public boolean isNetworkAvailable(Context context) {
//        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
//    }
//
//    private void receiveSoilMoistureData() {
//
//        try {
//            URL url = new URL(SERVER_URL + "/soil_moisture_events");
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("GET");
//            connection.setRequestProperty("Accept", "text/event-stream");
//            connection.connect();
//
//            int responseCode = connection.getResponseCode();
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                Log.d("Connection Status", "Kết nối đến server thành công.");
//                binding.connectionStatus.setText("Status: Connected");
//            } else {
//                Log.e("Connection Status", "Kết nối đến server thất bại với mã: " + responseCode);
//            }
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("data: ")) {
//                    String jsonData = line.substring(6);
//                    Log.d("SSE Data", jsonData);
//                    runOnUiThread(() -> updateMoisture(jsonData));
//                }
//            }
//        } catch (Exception e) {
//            Log.e("Error", "Error receiving soil moisture data: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//
//    private void updateMoisture(String jsonData) {
//        try {
//            // Phân tích dữ liệu JSON
//            JSONObject jsonObject = new JSONObject(jsonData);
//            int moisture = jsonObject.getInt("moisture");
//            moisturePercentage.setText(moisture + "%");
//            waveView.setPercentage(moisture);
//
//            // Gửi thông báo nếu độ ẩm nằm ngoài khoảng cho phép
//            if (moisture < 40 || moisture > 70) {
//                sendNotification("Độ ẩm hiện tại: " + moisture + "%");
//            }
//        } catch (JSONException e) {
//            Log.e("Error", "Error parsing JSON data: " + e.getMessage());
//        }
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
//    private void requestNotificationPermission() {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
//        }
//    }
//
//    private void sendNotification(String message) {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.circular_background)
//                .setContentTitle("Cảnh báo độ ẩm đất")
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
//            String name = "Humidity Alerts";
//            String description = "Channel for humidity alerts";
//            int importance = NotificationManager.IMPORTANCE_HIGH;
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
//            channel.setDescription(description);
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//        }
//    }
//}
package com.midterm.plantsapp;

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

import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "Humidity Alert";
    private static final String SERVER_URL = "http://192.168.2.103:5000"; // Thay đổi IP nếu cần
    private ActivityMainBinding binding;
    private TextView moisturePercentage;
    private WaveView waveView;
    private Socket socket;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }

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

        // Gửi thông báo nếu độ ẩm nằm ngoài khoảng cho phép
        if (moisture < 40 || moisture > 70) {
            sendNotification("Độ ẩm hiện tại: " + moisture + "%");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

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