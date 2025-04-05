package com.example.solovinyykray.Activity;

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

import com.example.solovinyykray.Adapter.ReviewsAdapter;
import com.example.solovinyykray.Domain.Review;
import com.example.solovinyykray.R;
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

/**
 * Activity для просмотра и добавления отзывов о продукте/достопримечательности.
 * Позволяет авторизованным пользователям оставлять отзывы с рейтингом и комментарием,
 * а также просматривать отзывы других пользователей.
 */

public class ReviewsActivity extends AppCompatActivity {

    private RecyclerView reviewsRecyclerView;
    private ReviewsAdapter reviewsAdapter;
    private List<Review> reviews = new ArrayList<>();
    private DatabaseReference reviewsRef;
    private RatingBar reviewRatingBar;
    private EditText reviewComment;
    private Button submitReviewButton;
    private String currentUserId;
    private String productId;

    /**
     * Инициализирует активность, настраивает интерфейс и обработчики событий.
     * @param savedInstanceState Сохраненное состояние активности. Может быть null.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        productId = getIntent().getStringExtra("productId");

        reviewRatingBar = findViewById(R.id.reviewRatingBar);
        reviewComment = findViewById(R.id.reviewComment);
        submitReviewButton = findViewById(R.id.submitReviewButton);

        if (currentUser == null) {
            reviewRatingBar.setVisibility(View.GONE);
            reviewComment.setVisibility(View.GONE);
            submitReviewButton.setVisibility(View.GONE);
            Toast.makeText(this, "Чтобы оставить отзыв, войдите в систему", Toast.LENGTH_SHORT).show();
        } else {
            reviewRatingBar.setVisibility(View.VISIBLE);
            reviewComment.setVisibility(View.VISIBLE);
            submitReviewButton.setVisibility(View.VISIBLE);
        }

        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsAdapter = new ReviewsAdapter(reviews);
        reviewsRecyclerView.setAdapter(reviewsAdapter);

        reviewRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (rating > 0) {
                reviewComment.setVisibility(View.VISIBLE);
                submitReviewButton.setVisibility(View.VISIBLE);
            } else {
                reviewComment.setVisibility(View.GONE);
                submitReviewButton.setVisibility(View.GONE);
            }
        });

        submitReviewButton.setOnClickListener(v -> {
            checkIfReviewExists();
        });

        loadReviews();
        enableImmersiveMode();

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

    /**
     * Проверяет, оставил ли текущий пользователь уже отзыв для данного продукта.
     * Если отзыв существует, показывает сообщение, в противном случае разрешает отправить новый отзыв.
     */

    private void checkIfReviewExists() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Чтобы оставить отзыв, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ReviewsActivity.this, LoginActivity.class));
            return;
        }
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
     * Отправляет новый отзыв в базу данных Firebase.
     * Выполняет валидацию данных перед отправкой.
     */

    private void submitReview() {
        float rating = reviewRatingBar.getRating();
        String comment = reviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Поставьте оценку", Toast.LENGTH_SHORT).show();
            return;
        }

        Review review = new Review(currentUserId, productId, (int) rating, comment, System.currentTimeMillis());

        String reviewId = reviewsRef.push().getKey();
        reviewsRef.child(reviewId).setValue(review)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Отзыв отправлен!", Toast.LENGTH_SHORT).show();
                        reviewRatingBar.setRating(0);
                        reviewComment.setText("");
                        reviewComment.setVisibility(View.GONE);
                        submitReviewButton.setVisibility(View.GONE);
                        loadReviews();

                        Intent resultIntent = new Intent();
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Ошибка: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Загружает отзывы для текущего продукта из Firebase Database.
     * Обновляет RecyclerView после загрузки данных.
     */

    private void loadReviews() {
        reviewsRef.orderByChild("productId").equalTo(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                reviews.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Review review = snapshot.getValue(Review.class);
                        if (review != null) {
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

    /**
     * Загружает данные пользователя (имя и аватар) для указанного отзыва.
     * Обновляет отзыв в списке после загрузки данных пользователя.
     *
     * @param review Отзыв, для которого нужно загрузить данные пользователя
     */

    private void loadUserData(Review review) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(review.getUserId());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userSnapshot) {
                if (userSnapshot.exists()) {
                    String username = userSnapshot.child("username").getValue(String.class);
                    String profileImageUrl = userSnapshot.child("profileImage").getValue(String.class);

                    review.setUsername(username);
                    review.setProfileImageUrl(profileImageUrl);

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