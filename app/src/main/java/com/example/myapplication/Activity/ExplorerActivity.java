package com.example.myapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myapplication.Adapter.ExplorerAdapter;
import com.example.myapplication.Adapter.PopularAdapter;
import com.example.myapplication.Domain.ItemDomain;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityExplorerBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

public class ExplorerActivity extends BaseActivity {
    ActivityExplorerBinding binding;
    ExplorerAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExplorerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initRoute();

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
                            list.add(item);
                        }
                    }

                    // Загружаем отзывы для расчета рейтинга
                    reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                            for (ItemRoute route : list) {
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
                                if (reviewCount > 0) {
                                    double averageRating = totalRating / reviewCount;
                                    route.setScore(averageRating); // Обновляем рейтинг маршрута
                                } else {
                                    route.setScore(0); // Если отзывов нет, рейтинг = 0
                                }
                            }

                            // Обновляем RecyclerView
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

