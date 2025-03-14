package com.example.myapplication.Activity;

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

import com.example.myapplication.Adapter.AttractionsAdapter;
import com.example.myapplication.Adapter.ExplorerAdapter;
import com.example.myapplication.Domain.ItemAttractions;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityFavoritesBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

public class FavoritesActivity extends BaseActivity {
    ActivityFavoritesBinding binding;
    AttractionsAdapter adapter;
    ExplorerAdapter explorerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupCategorySpinner();
        loadAttractions();

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
                        item.setId(issue.getKey()); // Устанавливаем id достопримечательности
                        favoriteList.add(item);
                    }
                }

                // Загружаем отзывы для расчета рейтинга
                reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                        for (ItemAttractions attraction : favoriteList) {
                            double totalRating = 0;
                            int reviewCount = 0;

                            // Проходим по всем отзывам
                            for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                                String productId = review.child("productId").getValue(String.class);
                                Double rating = review.child("rating").getValue(Double.class);

                                // Если productId совпадает с id достопримечательности
                                if (productId != null && productId.equals(attraction.getTitle()) && rating != null) {
                                    totalRating += rating;
                                    reviewCount++;
                                }
                            }

                            // Рассчитываем средний рейтинг
                            double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                            attraction.setScore(averageRating); // Обновляем рейтинг достопримечательности

                            // Логирование для проверки
                            Log.d("FavoriteRating", "Attraction: " + attraction.getTitle() + ", Rating: " + averageRating);
                        }

                        // Обновляем RecyclerView
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

                // Загружаем отзывы для расчета рейтинга
                reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                        for (ItemRoute route : favoriteList) {
                            double totalRating = 0;
                            int reviewCount = 0;

                            // Проходим по всем отзывам
                            for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                                String productId = review.child("productId").getValue(String.class);
                                Double rating = review.child("rating").getValue(Double.class);

                                // Если productId совпадает с id маршрута
                                if (productId != null && productId.equals(route.getTitle()) && rating != null) {
                                    totalRating += rating;
                                    reviewCount++;
                                }
                            }

                            // Рассчитываем средний рейтинг
                            double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                            route.setScore(averageRating); // Обновляем рейтинг маршрута

                            // Логирование для проверки
                            Log.d("RouteRating", "Route: " + route.getTitle() + ", Rating: " + averageRating);
                        }

                        // Обновляем RecyclerView
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

    private boolean isFavorite(String title) {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        return prefs.getBoolean("favorite_" + title, false);
    }
}
