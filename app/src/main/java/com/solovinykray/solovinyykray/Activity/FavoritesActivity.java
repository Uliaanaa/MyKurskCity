package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.solovinykray.solovinyykray.Adapter.AttractionsAdapter;
import com.solovinykray.solovinyykray.Adapter.ExplorerAdapter;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityFavoritesBinding;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Активность для отображения избранных пользователем достопримечательностей и маршрутов.
 * Позволяет переключаться между категориями ("Достопримечательности" и "Маршруты") через выпадающий список,
 * загружает данные из Firebase и отображает их в RecyclerView. Также включает нижнюю навигационную панель
 * для перехода к другим разделам приложения и показывает обучающее наложение при первом запуске.
 */
public class FavoritesActivity extends BaseActivity {
    ActivityFavoritesBinding binding;
    AttractionsAdapter adapter;
    ExplorerAdapter explorerAdapter;
    private static final String PREFS_NAME = "TutorialPrefs";
    private static final String KEY_TUTORIAL_SHOWN = "tutorial_shown";

    /**
     * Инициализирует активность, настраивает интерфейс и загружает данные.
     * Проверяет авторизацию пользователя, настраивает выпадающий список категорий,
     * нижнюю навигацию и отображает обучающее наложение при необходимости.
     *
     * @param savedInstanceState Сохраненное состояние активности, если она была пересоздана
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Войдите в систему, чтобы просмотреть избранное", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupCategorySpinner();
        loadAttractions();
        enableImmersiveMode();
        showTutorialIfFirstLaunch();

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
     * Показывает обучающее наложение при первом запуске активности.
     * Проверяет, было ли наложение уже показано, используя SharedPreferences.
     * Если наложение отображается, оно скрывается при клике, и флаг сохраняется.
     */
    private void showTutorialIfFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean tutorialShown = prefs.getBoolean(KEY_TUTORIAL_SHOWN, false);

        if (!tutorialShown) {
            binding.tutorialOverlay.setVisibility(View.VISIBLE);
            binding.tutorialOverlay.setOnClickListener(v -> {
                binding.tutorialOverlay.setVisibility(View.GONE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_TUTORIAL_SHOWN, true);
                editor.apply();
            });
        } else {
            binding.tutorialOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Включает иммерсивный режим, скрывая системную панель навигации.
     * Обеспечивает полноэкранное отображение интерфейса.
     */
    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Настраивает выпадающий список (Spinner) для выбора категории ("Достопримечательности" или "Маршруты").
     * Устанавливает адаптер с данными из ресурсов и слушатель для обработки выбора категории.
     * При выборе категории загружаются соответствующие данные (достопримечательности или маршруты).
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
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Загружает список избранных достопримечательностей пользователя из Firebase.
     * Получает идентификаторы избранных элементов, соответствующие данные достопримечательностей
     * и их рейтинги на основе отзывов. Обновляет RecyclerView для отображения данных.
     */
    private void loadAttractions() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            binding.progressBarAttractions.setVisibility(View.GONE);
            return;
        }
        String uid = user.getUid();

        binding.progressBarAttractions.setVisibility(View.VISIBLE);
        ArrayList<ItemAttractions> favoriteList = new ArrayList<>();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference("Favorites").child(uid).child("Attractions");
        DatabaseReference attractionsRef = FirebaseDatabase.getInstance().getReference("Attractions");
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> favoriteIds = new HashSet<>();
                for (DataSnapshot favSnapshot : snapshot.getChildren()) {
                    favoriteIds.add(favSnapshot.getKey());
                }

                attractionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favoriteList.clear();
                        for (DataSnapshot issue : snapshot.getChildren()) {
                            ItemAttractions item = issue.getValue(ItemAttractions.class);
                            if (item != null && favoriteIds.contains(issue.getKey())) {
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

                                        if (productId != null && productId.equals(attraction.getId()) && rating != null) {
                                            totalRating += rating;
                                            reviewCount++;
                                        }
                                    }

                                    double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                                    attraction.setScore(averageRating);

                                    Log.d("FavoriteRating", "Attraction: " + attraction.getTitle() + ", id: " + attraction.getId() + ", Rating: " + averageRating);
                                }

                                if (!favoriteList.isEmpty()) {
                                    binding.RecyclerViewAttractions.setLayoutManager(new LinearLayoutManager(FavoritesActivity.this));
                                    adapter = new AttractionsAdapter(favoriteList);
                                    binding.RecyclerViewAttractions.setAdapter(adapter);
                                    adapter.notifyDataSetChanged();
                                } else {
                                    binding.RecyclerViewAttractions.setAdapter(null);
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки избранного: " + error.getMessage());
                binding.progressBarAttractions.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Загружает список избранных маршрутов пользователя из Firebase.
     * Получает идентификаторы избранных маршрутов, соответствующие данные маршрутов
     * и их рейтинги на основе отзывов. Обновляет RecyclerView для отображения данных.
     */
    private void loadRoutes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            binding.progressBarAttractions.setVisibility(View.GONE);
            return;
        }
        String uid = user.getUid();

        binding.progressBarAttractions.setVisibility(View.VISIBLE);
        ArrayList<ItemRoute> favoriteList = new ArrayList<>();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference("Favorites").child(uid).child("Routes");
        DatabaseReference routesRef = FirebaseDatabase.getInstance().getReference("Route");
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> favoriteIds = new HashSet<>();
                for (DataSnapshot favSnapshot : snapshot.getChildren()) {
                    favoriteIds.add(favSnapshot.getKey());
                }

                routesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favoriteList.clear();
                        for (DataSnapshot issue : snapshot.getChildren()) {
                            ItemRoute item = issue.getValue(ItemRoute.class);
                            if (item != null && favoriteIds.contains(issue.getKey())) {
                                item.setId(issue.getKey());
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

                                        if (productId != null && productId.equals(route.getId()) && rating != null) {
                                            totalRating += rating;
                                            reviewCount++;
                                        }
                                    }

                                    double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                                    route.setScore(averageRating);

                                    Log.d("RouteRating", "Route: " + route.getTitle() + ", id: " + route.getId() + ", Rating: " + averageRating);
                                }

                                if (!favoriteList.isEmpty()) {
                                    binding.RecyclerViewAttractions.setLayoutManager(new LinearLayoutManager(FavoritesActivity.this));
                                    explorerAdapter = new ExplorerAdapter(favoriteList);
                                    binding.RecyclerViewAttractions.setAdapter(explorerAdapter);
                                    explorerAdapter.notifyDataSetChanged();
                                } else {
                                    binding.RecyclerViewAttractions.setAdapter(null);
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки избранного: " + error.getMessage());
                binding.progressBarAttractions.setVisibility(View.GONE);
            }
        });
    }
}