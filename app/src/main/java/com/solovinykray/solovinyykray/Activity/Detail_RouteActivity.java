package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
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
import com.solovinyykray.solovinyykray.databinding.ActivityDetailRouteBinding;

/**
 * Активность для просмотра детальной информации о маршруте,
 * позволяет просмотреть полное описание, рейтнг изображения и отзывы.
 * Поддерживает функцию добавления в избранное, переход к карте и переход к разделу с отзывами.
 */

public class Detail_RouteActivity extends BaseActivity {
    ActivityDetailRouteBinding binding;
    private ItemRoute object;
    boolean isFavorite;

    /**
     * Инициализирует активность, получает переданные данные из Intent,
     * настраивает интерфейс и загружает рейтинг маршрута.
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra();
        setVariable();
        loadAndCalculateRating();
        enableImmersiveMode();
    }

    /**
     * Настраивает переменные и элементы интерфейса,
     * устанавливает обработчики событий для кнопок.
     */

    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.descriptionTxt.setText(object.getDetail());
        binding.ratingBar.setRating((float) object.getScore());

        Glide.with(Detail_RouteActivity.this)
                .load(object.getPic())
                .into(binding.pic);

        binding.backBtn.setOnClickListener(v -> finish());

        binding.allbtn.setOnClickListener(v -> {
            Intent intent = new Intent(Detail_RouteActivity.this, ReviewsActivity.class);
            intent.putExtra("productId", object.getTitle());
            startActivity(intent);
        });

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Detail_RouteActivity.this, MapActivity.class);
            intent.putExtra("object", object);
            startActivity(intent);
        });

        if (isFavorite) {
            binding.favIcon.setImageResource(R.drawable.fav);
        } else {
            binding.favIcon.setImageResource(R.drawable.fav_icon);
        }
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
     * Получает объект маршрута из Intent, загружает его статус избранного.
     */

    private void getIntentExtra() {
        object = (ItemRoute) getIntent().getSerializableExtra("object");
        isFavorite = getIntent().getBooleanExtra("isFavorite", false);
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