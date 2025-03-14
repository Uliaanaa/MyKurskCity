package com.example.myapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myapplication.Adapter.AttractionsAdapter;
import com.example.myapplication.Adapter.ExplorerAdapter;
import com.example.myapplication.Domain.ItemAttractions;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityAttractionsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

public class AttractionsActivity extends BaseActivity {
    ActivityAttractionsBinding binding;
    AttractionsAdapter adapter;
    private DatabaseReference database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttractionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance().getReference();
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

        CardView fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), FormActivity.class);
            startActivity(intent);
        });



        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if(id==R.id.home){
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);

                }if(id==R.id.explorer){
                    intent = new Intent(getApplicationContext(), ExplorerActivity.class);
                    startActivity(intent);

                }if(id==R.id.profile){
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);

                }if(id==R.id.cart){
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);

                }
            }

        });
    }

    private void initRoute() {
        DatabaseReference attractionsRef = database.child("Attractions");
        DatabaseReference reviewsRef = database.child("reviews");
        binding.progressBarAttractions.setVisibility(View.VISIBLE);

        ArrayList<ItemAttractions> list = new ArrayList<>();
        attractionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemAttractions item = issue.getValue(ItemAttractions.class);
                        if (item != null) {
                            list.add(item);
                        }
                    }

                    // Загружаем отзывы для расчета рейтинга
                    reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                            for (ItemAttractions attraction : list) {
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
                                Log.d("AttractionRating", "Attraction: " + attraction.getTitle() + ", Rating: " + averageRating);
                            }

                            // Обновляем RecyclerView
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