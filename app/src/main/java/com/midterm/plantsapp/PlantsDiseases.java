package com.midterm.plantsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.midterm.plantsapp.databinding.ActivityPlantsDiseasesBinding;

import java.util.Map;

public class PlantsDiseases extends AppCompatActivity {

    private DatabaseReference imageUrlRef;
    private ActivityPlantsDiseasesBinding binding;
    private String databaseURL = "https://plantsapp-58396-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlantsDiseasesBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.plants_diseases_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.btnBack.setOnClickListener(view1 -> finish());

        imageUrlRef = FirebaseDatabase.getInstance(databaseURL).getReference("image_urls");

        // Listen for changes in image_urls
        imageUrlRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String latestImageUrl = null;
                long latestTimestamp = 0;
                String latestPredictedClass = null;
                String latestConfidenceValue = null;

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    Map<String, Object> data = (Map<String, Object>) childSnapshot.getValue();

                    if (data != null) {
                        String imageUrl = (String) data.get("url");
                        long timestamp = Long.parseLong(data.get("timestamp").toString());
                        String predictedClass = (String) data.get("predicted_class");
                        String confidenceValue = (String) data.get("confidence_value");

                        if (timestamp > latestTimestamp) {
                            latestTimestamp = timestamp;
                            latestImageUrl = imageUrl;
                            latestPredictedClass = predictedClass;
                            latestConfidenceValue = confidenceValue;
                        }
                    }
                }
                RequestOptions requestOptions = new RequestOptions()
                        .fitCenter()
                        .override(500, 500);

                if (latestImageUrl != null) {
                    Glide.with(PlantsDiseases.this)
                            .load(latestImageUrl)
                            .apply(requestOptions)
                            .placeholder(R.drawable.default_image)
                            .centerInside()
                            .override(500, 500)
                            .into(binding.plantImage);

                    binding.plantDiseaseInfo.setText(String.format("%s: %s", latestPredictedClass, latestConfidenceValue));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle any errors
            }
        });
    }
}