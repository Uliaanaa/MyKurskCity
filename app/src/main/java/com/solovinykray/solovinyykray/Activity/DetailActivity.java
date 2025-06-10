package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.solovinykray.solovinyykray.Domain.Review;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityDetailBinding;

/**
 * Activity отображающая подробную информацию о маршруте,
 * включая описание, рейтинг, статус "избранное",
 * возможность просмотра отзывов и перехода к детальному маршруту.
 */
public class DetailActivity extends BaseActivity {
    private ActivityDetailBinding binding;
    private ItemRoute object;
    private boolean isFavorite = false;
    private ValueEventListener favoriteListener;

    /**
     * Инициализация активности: загрузка данных маршрута, установка слушателей,
     * расчет рейтинга, активация полноэкранного режима.
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
     * Освобождение слушателя избранного при уничтожении активности.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (favoriteListener != null && object != null && object.getId() != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String uid = user.getUid();
                DatabaseReference favoritesRef = FirebaseDatabase.getInstance()
                        .getReference("Favorites")
                        .child(uid)
                        .child("Routes")
                        .child(object.getId());
                favoritesRef.removeEventListener(favoriteListener);
            }
        }
    }

    /**
     * Включение полноэкранного режима с скрытием навигации.
     */
    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Настройка переменных, обработчиков кликов и отображения информации о маршруте.
     */
    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.adressTxt.setText(object.getAddress());
        binding.descriptionTxt.setText(Html.fromHtml(object.getDescription(), Html.FROM_HTML_MODE_LEGACY));
        binding.bedTxt.setText(String.valueOf(object.getBed()));
        binding.distanceTxt.setText(object.getDistance());
        binding.durationTxt.setText(object.getDuration());
        binding.ratingBar.setRating((float) object.getScore());

        Glide.with(this).load(object.getPic()).into(binding.pic);

        binding.backBtn.setOnClickListener(v -> finish());

        binding.favIcon.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Войдите в систему, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            toggleFavoriteStatus();
        });

        binding.allbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewsActivity.class);
            intent.putExtra("productId", object.getTitle());
            startActivity(intent);
        });

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, Detail_RouteActivity.class);
            intent.putExtra("object", object);
            intent.putExtra("isFavorite", isFavorite);
            startActivity(intent);
        });
    }

    /**
     * Обновление иконки избранного маршрута в зависимости от статуса.
     */
    private void updateFavoriteIcon() {
        binding.favIcon.setImageResource(isFavorite ? R.drawable.fav : R.drawable.fav_icon);
    }

    /**
     * Переключение статуса "избранное" для текущего маршрута и обновление в Firebase.
     */
    private void toggleFavoriteStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || object.getId() == null || object.getId().isEmpty()) {
            Log.e("DetailActivity", "Пользователь не авторизован или id отсутствует: " + object.getTitle());
            Toast.makeText(this, "Ошибка: идентификатор маршрута отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance()
                .getReference("Favorites")
                .child(uid)
                .child("Routes")
                .child(object.getId());

        isFavorite = !isFavorite;
        if (isFavorite) {
            favoritesRef.setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Маршрут добавлен в избранное: " + object.getId()))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Firebase", "Ошибка сохранения избранного: ", e);
                        isFavorite = false;
                        updateFavoriteIcon();
                    });
        } else {
            favoritesRef.removeValue()
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Маршрут удалён из избранного: " + object.getId()))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Firebase", "Ошибка удаления избранного: ", e);
                        isFavorite = true;
                        updateFavoriteIcon();
                    });
        }
        updateFavoriteIcon();
    }

    /**
     * Загрузка текущего статуса "избранное" маршрута из Firebase и установка слушателя.
     */
    private void loadFavoriteStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || object.getId() == null || object.getId().isEmpty()) {
            isFavorite = false;
            updateFavoriteIcon();
            Log.e("DetailActivity", "Пользователь не авторизован или id отсутствует: " + object.getTitle());
            return;
        }

        String uid = user.getUid();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance()
                .getReference("Favorites")
                .child(uid)
                .child("Routes")
                .child(object.getId());

        favoriteListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                updateFavoriteIcon();
                Log.d("DetailActivity", "Статус избранного загружен: " + isFavorite + " для id: " + object.getId());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки статуса избранного: " + error.getMessage());
                isFavorite = false;
                updateFavoriteIcon();
            }
        };

        favoritesRef.addValueEventListener(favoriteListener);
    }

    /**
     * Получение объекта маршрута из Intent и запуск проверки избранного.
     */
    private void getIntentExtra() {
        object = (ItemRoute) getIntent().getSerializableExtra("object");
        if (object == null) {
            Log.e("DetailActivity", "Объект ItemRoute не передан");
            Toast.makeText(this, "Ошибка: данные о маршруте отсутствуют", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (object.getId() == null || object.getId().isEmpty()) {
            Log.e("DetailActivity", "ID отсутствует для title: " + object.getTitle());
            Toast.makeText(this, "Ошибка: идентификатор маршрута отсутствует", Toast.LENGTH_SHORT).show();
        }
        Log.d("DetailActivity", "Получен объект: id=" + object.getId() + ", title=" + object.getTitle());
        loadFavoriteStatus();
    }

    /**
     * Загрузка всех отзывов маршрута из Firebase и расчет среднего рейтинга.
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