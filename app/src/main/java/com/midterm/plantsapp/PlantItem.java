package com.midterm.plantsapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class PlantItem {
    private final View itemView;
    private final Context context;

    public PlantItem(Context context, String imageUrl, String predictedClass, String confidenceValue) {
        this.context = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        itemView = inflater.inflate(R.layout.plant_item, null);

        ImageView plantImage = itemView.findViewById(R.id.plant_image);
        TextView diseaseName = itemView.findViewById(R.id.plant_disease_name);
        TextView percentageValue = itemView.findViewById(R.id.percentage_value);
        ImageButton moreInfoButton = itemView.findViewById(R.id.more_info_button);
        RequestOptions requestOptions = new RequestOptions()
                .fitCenter()
                .override(500, 500);

        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .placeholder(R.drawable.default_image)
                .into(plantImage);

        diseaseName.setText(String.format("%s", predictedClass));
        percentageValue.setText(String.format("%s", confidenceValue));


        moreInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, FullscreenImageActivity.class);
                intent.putExtra("image_url", imageUrl);
                context.startActivity(intent);
            }
        });
    }

    public View getItemView() {
        return itemView;
    }
}

