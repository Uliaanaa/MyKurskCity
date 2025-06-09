package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.solovinykray.solovinyykray.Domain.Review;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityDetailBinding;

/**
 * Активность для просмотра общей информации о маршруте,
 * позволяет просмотреть краткое описание, рейтнг, тип маршрута, длительность, изображения и отзывы.
 * Поддерживает функцию добавления в избранное, переход к экрану с подробной информацией и переход
 * к разделу с отзывами.
 */

public class DetailActivity extends BaseActivity {
    private ActivityDetailBinding binding;
    private ItemRoute object;
    private boolean isFavorite = false;

    /**
     * Инициализирует активность, получает переданные данные из Intent,
     * настраивает интерфейс и загружает рейтинг маршрута.
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra();
        setVariable();
        loadAndCalculateRating();
        enableImmersiveMode();
    }

    /**
     * Скрывает системную навигацию (включает иммерсивный режим).
     */

    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Настраивает переменные и элементы интерфейса,
     * устанавливает обработчики событий для кнопок.
     */

    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.adressTxt.setText(object.getAddress());
        binding.descriptionTxt.setText(Html.fromHtml(object.getDescription(), Html.FROM_HTML_MODE_LEGACY));
        binding.bedTxt.setText(String.valueOf(object.getBed()));
        binding.distanceTxt.setText(object.getDistance());
        binding.durationTxt.setText(object.getDuration());

        binding.ratingBar.setRating((float) object.getScore());

        Glide.with(DetailActivity.this)
                .load(object.getPic())
                .into(binding.pic);

        binding.backBtn.setOnClickListener(v -> finish());

        updateFavoriteIcon();

        binding.favIcon.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFavoriteIcon();
            saveFavoriteStatus();
        });

        binding.allbtn.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, ReviewsActivity.class);
            intent.putExtra("productId", object.getTitle());
            startActivity(intent);
        });

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, Detail_RouteActivity.class);
            intent.putExtra("object", object);
            intent.putExtra("isFavorite", isFavorite);
            startActivity(intent);
        });
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            binding.favIcon.setImageResource(R.drawable.fav);
        } else {
            binding.favIcon.setImageResource(R.drawable.fav_icon);
        }
    }

    /**
     * Сохраняет статус избранного в SharedPreferences.
     */


    private void saveFavoriteStatus() {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("favorite_" + object.getTitle(), isFavorite);
        editor.apply();
    }

    /**
     * Загружает статус избранного из SharedPreferences.
     */


    private void loadFavoriteStatus() {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        isFavorite = prefs.getBoolean("favorite_" + object.getTitle(), false);
    }

    /**
     * Получает объект маршрута из Intent, загружает его статус избранного.
     */

    private void getIntentExtra() {
        object = (ItemRoute) getIntent().getSerializableExtra("object");
        loadFavoriteStatus();
    }

    /**
     * Загружает отзывы из Firebase и на их основе высчитывает рейтинг достопримечательности,
     * в соотвествии с рейтингом обновляет RatingBar.
     */

    private void loadAndCalculateRating() {
        String productId = object.getTitle();

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
                    binding.ratingBar.setRating((float) averageRating);

                } else {
                    binding.ratingBar.setRating(0);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Ошибка загрузки отзывов: " + databaseError.getMessage());
            }
        });
    }
}