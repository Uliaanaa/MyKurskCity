package com.example.myapplication.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.Domain.Review;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityDetailRouteBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Detail_RouteActivity extends BaseActivity {
    ActivityDetailRouteBinding binding;
    private ItemRoute object;
    boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra();
        setVariable();
        loadAndCalculateRating(); // Загружаем отзывы и рассчитываем рейтинг
    }

    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.descriptionTxt.setText(object.getDetail());
        binding.ratingBar.setRating((float) object.getScore());

        Glide.with(Detail_RouteActivity.this)
                .load(object.getPic())
                .into(binding.pic);

        binding.backBtn.setOnClickListener(v -> finish());

        // Кнопка для перехода на экран отзывов
        binding.allbtn.setOnClickListener(v -> {
            Intent intent = new Intent(Detail_RouteActivity.this, ReviewsActivity.class);
            intent.putExtra("productId", object.getTitle()); // Передаем productId
            startActivity(intent);
        });

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Detail_RouteActivity.this, MapActivity.class);
            intent.putExtra("object", object);
            startActivity(intent);
        });

        if (isFavorite) {
            binding.favIcon.setImageResource(R.drawable.fav); // Красное сердечко
        } else {
            binding.favIcon.setImageResource(R.drawable.fav_icon); // Обычное сердечко
        }
    }

    private void getIntentExtra() {
        object = (ItemRoute) getIntent().getSerializableExtra("object");
        isFavorite = getIntent().getBooleanExtra("isFavorite", false); // Получаем статус избранного (по умолчанию false)
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