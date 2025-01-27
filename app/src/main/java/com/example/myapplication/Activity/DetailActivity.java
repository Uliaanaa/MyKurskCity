package com.example.myapplication.Activity;

import android.content.Intent;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.example.myapplication.Domain.ItemDomain;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.databinding.ActivityDetailBinding;

public class DetailActivity extends BaseActivity {
    ActivityDetailBinding binding;
    private ItemRoute object;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra();
        setVariable();

    }

    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.backBtn.setOnClickListener(v -> finish());
        binding.bedTxt.setText(""+object.getBed());
        binding.distanceTxt.setText(object.getDistance());
        binding.durationTxt.setText(object.getDuration());
        binding.descriptionTxt.setText(object.getDescription());
        binding.adressTxt.setText(object.getAddress());
        binding.ratingTxt.setText(object.getScore()+"");
        binding.ratingBar.setRating((float) object.getScore());

        Glide.with(DetailActivity.this)
                .load(object.getPic())
                .into(binding.pic);

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent intent=new Intent(DetailActivity.this, MapActivity.class);
            startActivity(intent);
        });

    }

    private void getIntentExtra() {
        object= (ItemRoute) getIntent().getSerializableExtra("object");
    }
}