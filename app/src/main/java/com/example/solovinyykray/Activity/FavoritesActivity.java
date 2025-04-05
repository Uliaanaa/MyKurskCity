package com.example.solovinyykray.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.solovinyykray.Adapter.AttractionsAdapter;
import com.example.solovinyykray.Adapter.ExplorerAdapter;
import com.example.solovinyykray.Domain.ItemAttractions;
import com.example.solovinyykray.Domain.ItemRoute;
import com.example.solovinyykray.R;
import com.example.solovinyykray.databinding.ActivityFavoritesBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

/**
 * Активность для отображения избранных маршрутов и достопримечательностей,
 * позволяет переключаться на нужный раздел.
 * Загружает соответсвующие данные из Firebase и отображает их в списке.
 * Для каждого элемента высчитывает рейтинг.
 */
public class FavoritesActivity extends BaseActivity {
    ActivityFavoritesBinding binding;
    AttractionsAdapter adapter;
    ExplorerAdapter explorerAdapter;

    /**
     * Инициализирует активность, интерфейс, настраивает спиннер категорий, загружает
     * избранные элементы и настраивает нижнее меню.
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupCategorySpinner();
        loadAttractions();
        enableImmersiveMode();

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        if (chipNavigationBar != null) {
            chipNavigationBar.setOnItemSelectedListener(id -> {
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
                } else if (id == R.id.attractions) {
                    intent = new Intent(getApplicationContext(), AttractionsActivity.class);
                    startActivity(intent);
                }
            });
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
     * Настраивает спиннер для выбора категории.
     * При выборе пользователем загружает соответствующий список элементов.
     */

    private void setupCategorySpinner() {
        Spinner categorySpinner = findViewById(R.id.category_spinner);
        categorySpinner.setBackgroundResource(R.drawable.spinner_background);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.category_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(getResources().getColor(R.color.darkBlue));
                String selectedCategory = parent.getItemAtPosition(position).toString();
                if (selectedCategory.equals("Достопримечательности")) {
                    loadAttractions();
                } else if (selectedCategory.equals("Маршруты")) {
                    loadRoutes();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Загружает список избранных достопримечательностей из Firebase,
     * для каждой достопримечательности расчитывает рейтинг, обновляет
     * RecyclerView с загруженными данными.
     */

    private void loadAttractions() {
        binding.progressBarAttractions.setVisibility(View.VISIBLE);
        ArrayList<ItemAttractions> favoriteList = new ArrayList<>();
        DatabaseReference attractionsRef = FirebaseDatabase.getInstance().getReference("Attractions");
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        attractionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteList.clear();
                for (DataSnapshot issue : snapshot.getChildren()) {
                    ItemAttractions item = issue.getValue(ItemAttractions.class);
                    if (item != null && isFavorite(item.getTitle())) {
                        item.setId(issue.getKey());
                        favoriteList.add(item);
                    }
                }

                reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                        for (ItemAttractions attraction : favoriteList) {
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

                            Log.d("FavoriteRating", "Attraction: " + attraction.getTitle() + ", Rating: " + averageRating);
                        }

                        if (!favoriteList.isEmpty()) {
                            binding.RecyclerViewAttractions.setLayoutManager(new LinearLayoutManager(FavoritesActivity.this));
                            adapter = new AttractionsAdapter(favoriteList);
                            binding.RecyclerViewAttractions.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        }
                        binding.progressBarAttractions.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Ошибка загрузки отзывов: " + error.getMessage());
                        binding.progressBarAttractions.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки достопримечательностей: " + error.getMessage());
                binding.progressBarAttractions.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Загружает список избранных маршрутов из Firebase,
     * для каждого маршрута расчитывает рейтинг, обновляет
     * RecyclerView с загруженными данными.
     */

    private void loadRoutes() {
        binding.progressBarAttractions.setVisibility(View.VISIBLE);
        ArrayList<ItemRoute> favoriteList = new ArrayList<>();
        DatabaseReference routesRef = FirebaseDatabase.getInstance().getReference("Route");
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        routesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteList.clear();
                for (DataSnapshot issue : snapshot.getChildren()) {
                    ItemRoute item = issue.getValue(ItemRoute.class);
                    if (item != null && isFavorite(item.getTitle())) {
                        favoriteList.add(item);
                    }
                }

                reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                        for (ItemRoute route : favoriteList) {
                            double totalRating = 0;
                            int reviewCount = 0;

                            for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                                String productId = review.child("productId").getValue(String.class);
                                Double rating = review.child("rating").getValue(Double.class);

                                if (productId != null && productId.equals(route.getTitle()) && rating != null) {
                                    totalRating += rating;
                                    reviewCount++;
                                }
                            }

                            double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                            route.setScore(averageRating);

                            Log.d("RouteRating", "Route: " + route.getTitle() + ", Rating: " + averageRating);
                        }

                        if (!favoriteList.isEmpty()) {
                            binding.RecyclerViewAttractions.setLayoutManager(new LinearLayoutManager(FavoritesActivity.this));
                            explorerAdapter = new ExplorerAdapter(favoriteList);
                            binding.RecyclerViewAttractions.setAdapter(explorerAdapter);
                            explorerAdapter.notifyDataSetChanged();
                        }
                        binding.progressBarAttractions.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Ошибка загрузки отзывов: " + error.getMessage());
                        binding.progressBarAttractions.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки маршрутов: " + error.getMessage());
                binding.progressBarAttractions.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Проверяе, добавлен ли элемент в избранное
     * @param title название достопримечательности/маршрута.
     * @return true если элемент добавлен в избранное, false в противном случае.
     */

    private boolean isFavorite(String title) {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        return prefs.getBoolean("favorite_" + title, false);
    }
}
