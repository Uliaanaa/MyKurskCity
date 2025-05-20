package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.solovinykray.solovinyykray.Adapter.AttractionsAdapter;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityAttractionsBinding;

import java.util.ArrayList;

/**
 * Активность для отображения и управления достопримечательностями.
 * Класс отвечает за отображение списка достопримечательностей, реализацию поиска
 * и навигацию между экранами через нижнее меню.
 * Также она позволяет выбирать конкретные достопримечательности для составление маршрута из них
 * и реализовывает возможность добавления достопримечательности
 */

public class AttractionsActivity extends BaseActivity {
    ActivityAttractionsBinding binding;
    AttractionsAdapter adapter;
    private DatabaseReference database;

    /**
     * Инициализирует активность, настраивает UI компоненты и загружает данные о достопримечательностях.
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttractionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance().getReference();
        initRoute();
        enableImmersiveMode();

        binding.editTextText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        CardView fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), RequestActivity.class);
            startActivity(intent);
        });

        binding.fabRoute.setOnClickListener(view -> {
            if (adapter.getSelectedAttractions().isEmpty()) {
                Toast.makeText(this, "Выберите хотя бы одну достопримечательность", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, RouteDetailActivity.class);
                intent.putExtra("selectedItems", new ArrayList<>(adapter.getSelectedAttractions())); // Используем putExtra
                startActivity(intent);
            }
        });

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if (id == R.id.home) {
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                } else if (id == R.id.explorer) {
                    intent = new Intent(getApplicationContext(), ExplorerActivity.class);
                    startActivity(intent);
                } else if (id == R.id.profile) {
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);
                } else if (id == R.id.cart) {
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * показывает кнопку построения маршрута.
     */

    public void showRouteButton() {
        binding.fabRoute.setVisibility(View.VISIBLE);
    }

    /**
     * Скрывает кнопку построения маршрута.
     */

    public void hideRouteButton() {
        binding.fabRoute.setVisibility(View.GONE);
    }

    /**
     * показывает кнопку для отправки формы.
     */

    public void showFormButton() {
        binding.fabAdd.setVisibility(View.VISIBLE);
    }

    /**
     * Скрывает кнопку для отправки фомы.
     */

    public void hideFormButton() {
        binding.fabAdd.setVisibility(View.GONE);
    }

    /**
     * Скрывает системную навигацию (включает иммерсивный режим).
     */

    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Инициализирует загрузку данных о достопримечательностях и
     * высчитывает рейтинг на основе отзывов пользователей.
     */

    private void initRoute() {
        DatabaseReference attractionsRef = database.child("Attractions");
        DatabaseReference reviewsRef = database.child("reviews");
        binding.progressBarAttractions.setVisibility(View.VISIBLE);

        ArrayList<ItemAttractions> list = new ArrayList<>();
        attractionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int index = 0;
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemAttractions item = issue.getValue(ItemAttractions.class);
                        if (item != null) {
                            if (index < 36 || "approved".equals(item.getStatus())) {
                                list.add(item);
                            }
                            index++;
                        }
                    }


                    reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                            for (ItemAttractions attraction : list) {
                                double totalRating = 0;
                                int reviewCount = 0;

                                for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                                    String productId = review.child("productId").getValue(String.class);
                                    Double rating = review.child("rating").getValue(Double.class);

                                    if (productId != null && productId.equals(attraction.getTitle()) && rating != null) {
                                        totalRating += rating;
                                        reviewCount++;
                                    }
                                }

                                double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                                attraction.setScore(averageRating);

                                Log.d("AttractionRating", "Attraction: " + attraction.getTitle() + ", Rating: " + averageRating);
                            }

                            if (!list.isEmpty()) {
                                binding.RecyclerViewAttractions.setLayoutManager(new LinearLayoutManager(AttractionsActivity.this, LinearLayoutManager.VERTICAL, false));
                                adapter = new AttractionsAdapter(list);
                                binding.RecyclerViewAttractions.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                            }
                            binding.progressBarAttractions.setVisibility(View.GONE);
                            binding.RecyclerViewAttractions.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Ошибка загрузки отзывов: " + error.getMessage());
                            binding.progressBarAttractions.setVisibility(View.GONE);
                        }
                    });
                } else {
                    Log.d("Firebase", "Нет данных о достопримечательностях.");
                    binding.progressBarAttractions.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки достопримечательностей: " + error.getMessage());
                binding.progressBarAttractions.setVisibility(View.GONE);
            }
        });
    }
}