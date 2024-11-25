package com.midterm.plantsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class FullscreenImageActivity extends AppCompatActivity {

    private ImageView fullscreenImageView;
    private View overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        fullscreenImageView = findViewById(R.id.fullscreen_image);
        overlayView = findViewById(R.id.overlay_view);

        String imageUrl = getIntent().getStringExtra("image_url");

        if (imageUrl != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(fullscreenImageView);
        }

        fullscreenImageView.setVisibility(View.VISIBLE);
        overlayView.setVisibility(View.VISIBLE);

        overlayView.setOnClickListener(v -> finish());
    }
}
