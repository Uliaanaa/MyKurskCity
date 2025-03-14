package com.example.myapplication.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.Domain.Review;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityDetailBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DetailActivity extends BaseActivity {
    private ActivityDetailBinding binding;
    private ItemRoute object;
    private boolean isFavorite = false; // Статус избранного

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra();
        setVariable();
        loadAndCalculateRating(); // Загружаем отзывы и рассчитываем рейтинг
    }

    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.adressTxt.setText(object.getAddress());
        binding.descriptionTxt.setText(object.getDescription());
        binding.bedTxt.setText(String.valueOf(object.getBed()));
        binding.distanceTxt.setText(object.getDistance());
        binding.durationTxt.setText(object.getDuration());

        binding.ratingBar.setRating((float) object.getScore());

        Glide.with(DetailActivity.this)
                .load(object.getPic())
                .into(binding.pic);

        // Настройка кнопки "назад"
        binding.backBtn.setOnClickListener(v -> finish());

        // Обновление иконки избранного при загрузке
        updateFavoriteIcon();

        // Обработчик нажатия на иконку избранного
        binding.favIcon.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFavoriteIcon();
            saveFavoriteStatus();
        });

        // Кнопка для перехода на экран отзывов
        binding.allbtn.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, ReviewsActivity.class);
            intent.putExtra("productId", object.getTitle()); // Передаем productId
            startActivity(intent);
        });

        // Настройка кнопки маршрута
        binding.addToCartBtn.setOnClickListener(v -> {
            // Переход на экран с деталями маршрута
            Intent intent = new Intent(DetailActivity.this, Detail_RouteActivity.class);
            intent.putExtra("object", object);
            intent.putExtra("isFavorite", isFavorite);
            startActivity(intent);
        });
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            binding.favIcon.setImageResource(R.drawable.fav); // Красное сердечко
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
        object = (ItemRoute) getIntent().getSerializableExtra("object");
        loadFavoriteStatus(); // Загрузка статуса избранного
    }

    private void loadAndCalculateRating() {
        String productId = object.getTitle(); // Используем название маршрута как productId

        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");
        reviewsRef.orderByChild("productId").equalTo(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double totalRating = 0;
                int reviewCount = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Review review = snapshot.getValue(Review.class);
                    if (review != null && review.getProductId().equals(productId)) {
                        totalRating += review.getRating();
                        reviewCount++;
                    }
                }

                if (reviewCount > 0) {
                    double averageRating = totalRating / reviewCount;
                    binding.ratingBar.setRating((float) averageRating); // Обновляем рейтинг в RatingBar

                } else {
                    binding.ratingBar.setRating(0); // Если отзывов нет, рейтинг = 0

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Ошибка загрузки отзывов: " + databaseError.getMessage());
            }
        });
    }
}