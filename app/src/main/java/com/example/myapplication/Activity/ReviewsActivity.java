package com.example.myapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Adapter.ReviewsAdapter;
import com.example.myapplication.Domain.Review;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;
import java.util.List;

public class ReviewsActivity extends AppCompatActivity {

    private RecyclerView reviewsRecyclerView;
    private ReviewsAdapter reviewsAdapter;
    private List<Review> reviews = new ArrayList<>();
    private DatabaseReference reviewsRef;

    // Элементы для формы отзыва
    private RatingBar reviewRatingBar;
    private EditText reviewComment;
    private Button submitReviewButton;

    private String currentUserId; // ID текущего пользователя
    private String productId; // ID продукта

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        // Инициализация Firebase
        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        // Получаем ID текущего пользователя
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        // Получаем ID продукта из Intent
        productId = getIntent().getStringExtra("productId");

        // Инициализация элементов для формы отзыва
        reviewRatingBar = findViewById(R.id.reviewRatingBar);
        reviewComment = findViewById(R.id.reviewComment);
        submitReviewButton = findViewById(R.id.submitReviewButton);

        // Настройка RecyclerView
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsAdapter = new ReviewsAdapter(reviews);
        reviewsRecyclerView.setAdapter(reviewsAdapter);

        // Обработка изменения рейтинга
        reviewRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (rating > 0) {
                // Показываем поле для комментария и кнопку "Отправить"
                reviewComment.setVisibility(View.VISIBLE);
                submitReviewButton.setVisibility(View.VISIBLE);
            } else {
                // Скрываем поле для комментария и кнопку "Отправить"
                reviewComment.setVisibility(View.GONE);
                submitReviewButton.setVisibility(View.GONE);
            }
        });

        // Обработка нажатия на кнопку "Отправить"
        submitReviewButton.setOnClickListener(v -> {
            checkIfReviewExists();
        });

        // Загрузка отзывов
        loadReviews();

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if (id == R.id.explorer) {
                    intent = new Intent(ReviewsActivity.this, ExplorerActivity.class);
                    startActivity(intent);

                }
                if (id == R.id.home) {
                    intent = new Intent(ReviewsActivity.this, MainActivity.class);
                    startActivity(intent);

                }
                if (id == R.id.attractions) {
                    intent = new Intent(getApplicationContext(), AttractionsActivity.class);
                    startActivity(intent);

                }
                if (id == R.id.profile) {
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);
                }
                if (id == R.id.cart) {
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);

                }
            }
        });

    }

    private void checkIfReviewExists() {
        // Проверяем, есть ли уже отзыв от текущего пользователя
        reviewsRef.orderByChild("userId").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean hasReview = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Review review = snapshot.getValue(Review.class);
                    if (review != null && review.getProductId().equals(productId)) {
                        hasReview = true;
                        break;
                    }
                }

                if (hasReview) {
                    Toast.makeText(ReviewsActivity.this, "Вы уже оставили отзыв", Toast.LENGTH_SHORT).show();
                } else {
                    submitReview();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Ошибка Firebase: " + databaseError.getMessage());
            }
        });
    }

    private void submitReview() {
        float rating = reviewRatingBar.getRating();
        String comment = reviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Поставьте оценку", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем объект отзыва
        Review review = new Review(currentUserId, productId, (int) rating, comment, System.currentTimeMillis());

        // Сохраняем отзыв в Firebase
        String reviewId = reviewsRef.push().getKey();
        reviewsRef.child(reviewId).setValue(review)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Отзыв отправлен!", Toast.LENGTH_SHORT).show();
                        reviewRatingBar.setRating(0); // Сбрасываем рейтинг
                        reviewComment.setText(""); // Очищаем поле комментария
                        reviewComment.setVisibility(View.GONE); // Скрываем поле для комментария
                        submitReviewButton.setVisibility(View.GONE); // Скрываем кнопку "Отправить"
                        loadReviews(); // Обновляем список отзывов

                        // Возвращаем результат в Detail_AttractionActivity
                        Intent resultIntent = new Intent();
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Ошибка: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadReviews() {
        reviewsRef.orderByChild("productId").equalTo(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                reviews.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Review review = snapshot.getValue(Review.class);
                        if (review != null) {
                            // Загружаем данные пользователя по userId
                            loadUserData(review);
                        }
                    } catch (Exception e) {
                        Log.e("FirebaseError", "Ошибка при загрузке отзыва: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Ошибка Firebase: " + databaseError.getMessage());
            }
        });
    }

    private void loadUserData(Review review) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(review.getUserId());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userSnapshot) {
                if (userSnapshot.exists()) {
                    String username = userSnapshot.child("username").getValue(String.class);
                    String profileImageUrl = userSnapshot.child("profileImage").getValue(String.class);

                    // Обновляем отзыв данными пользователя
                    review.setUsername(username);
                    review.setProfileImageUrl(profileImageUrl);

                    // Добавляем отзыв в список и обновляем адаптер
                    reviews.add(review);
                    reviewsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Ошибка загрузки данных пользователя: " + databaseError.getMessage());
            }
        });
    }
}