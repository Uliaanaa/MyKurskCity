package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;


import com.solovinykray.solovinyykray.Adapter.ExplorerAdapter;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityExplorerBinding;

import java.util.ArrayList;

/**
 * Активность для отображения и управления маршрутами.
 * Класс отвечает за отображение списка маршрутов, реализацию поиска
 * и навигацию между экранами через нижнее меню.
 */

public class ExplorerActivity extends BaseActivity {
    ActivityExplorerBinding binding;
    ExplorerAdapter adapter;

    /**
     * Инициализирует активность, настраивает UI компоненты и загружает данные о маршрутах.
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExplorerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if(id==R.id.home){
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);

                }if(id==R.id.attractions){
                    intent = new Intent(getApplicationContext(), AttractionsActivity.class);
                    startActivity(intent);

                }if(id==R.id.profile){
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);

                }
                if(id==R.id.cart){
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);

                }
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
     * Инициализирует загрузку данных о достопримечательностях и
     * высчитывает рейтинг на основе отзывов пользователей.
     */

    private void initRoute() {
        DatabaseReference routeRef = database.getReference("Route");
        DatabaseReference reviewsRef = database.getReference("reviews");
        binding.progressBarExplorer.setVisibility(View.VISIBLE);

        ArrayList<ItemRoute> list = new ArrayList<>();
        routeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemRoute item = issue.getValue(ItemRoute.class);
                        if (item != null) {
                            item.setId(issue.getKey());
                            list.add(item);
                        }
                    }

                    reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                            for (ItemRoute route : list) {
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

                                if (reviewCount > 0) {
                                    double averageRating = totalRating / reviewCount;
                                    route.setScore(averageRating);
                                } else {
                                    route.setScore(0);
                                }
                            }

                            if (!list.isEmpty()) {
                                binding.RecyclerViewExplorer.setLayoutManager(new LinearLayoutManager(ExplorerActivity.this, LinearLayoutManager.VERTICAL, false));
                                adapter = new ExplorerAdapter(list);
                                binding.RecyclerViewExplorer.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                            }
                            binding.progressBarExplorer.setVisibility(View.GONE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Ошибка загрузки отзывов: " + error.getMessage());
                            binding.progressBarExplorer.setVisibility(View.GONE);
                        }
                    });
                } else {
                    Log.d("Firebase", "Нет данных о маршрутах.");
                    binding.progressBarExplorer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки маршрутов: " + error.getMessage());
                binding.progressBarExplorer.setVisibility(View.GONE);
            }
        });
    }

}

