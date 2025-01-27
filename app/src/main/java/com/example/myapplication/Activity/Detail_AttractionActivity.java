package com.example.myapplication.Activity;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.example.myapplication.Domain.ItemAttractions;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityDetailAttractionBinding;

public class Detail_AttractionActivity extends BaseActivity {
    private ActivityDetailAttractionBinding binding;
    private ItemAttractions object;
    private boolean isFavorite = false; // Статус избранного
    String l="",w="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailAttractionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra();
        setVariable();
    }

    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.adressTxt.setText(object.getAddress());
        binding.descriptionTxt.setText(object.getDescription());
        binding.bedTxt.setText(object.getBed());
        binding.ratingTxt.setText(object.getScore() + "");
        binding.ratingBar.setRating((float) object.getScore());
        l=object.getLongitude();
        w=object.getWidth();

        Glide.with(Detail_AttractionActivity.this)
                .load(object.getPic())
                .into(binding.pic);

        // Настройка кнопки "назад"
        binding.backBtn.setOnClickListener(v -> finish());

        // Настройка иконки избранного
        updateFavoriteIcon();

        binding.favIcon.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFavoriteIcon();
            saveFavoriteStatus();
        });

        // Настройка кнопки маршрута
        binding.addToCartBtn.setOnClickListener(v -> {

            Intent intent = new Intent(Detail_AttractionActivity.this, MapRouteActivity.class);
            intent.putExtra("l",l);
            intent.putExtra("w",w);
            startActivity(intent);
        });
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            binding.favIcon.setImageResource(R.drawable.fav); // Сердечко красного цвета
        } else {
            binding.favIcon.setImageResource(R.drawable.fav_icon); // Обычное сердечко
        }
    }

    private void saveFavoriteStatus() {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("favorite_" + object.getTitle(), isFavorite);
        editor.apply();
    }

    private void loadFavoriteStatus() {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        isFavorite = prefs.getBoolean("favorite_" + object.getTitle(), false);
    }

    private void getIntentExtra() {
        object = (ItemAttractions) getIntent().getSerializableExtra("object");
        loadFavoriteStatus(); // Загрузка статуса избранного
    }
}
